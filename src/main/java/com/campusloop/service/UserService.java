package com.campusloop.service;

import com.campusloop.common.exception.AuthenticationException;
import com.campusloop.common.exception.DuplicateException;
import com.campusloop.common.exception.NotFoundException;
import com.campusloop.common.exception.PermissionDeniedException;
import com.campusloop.common.exception.ValidationException;
import com.campusloop.common.util.Logs;
import com.campusloop.common.util.PasswordEncoder;
import com.campusloop.common.util.ValidationUtils;
import com.campusloop.dao.DaoFactory;
import com.campusloop.dao.UserDao;
import com.campusloop.domain.Role;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;
import com.campusloop.service.dto.UserCommand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * 用户领域服务：注册、登录、资料维护、管理员用户管理。
 *
 * <p>职责边界：
 * <ul>
 *   <li>业务规则（用户名唯一、密码强度、封禁不可登录、信用分区间）均在此集中实现；</li>
 *   <li>不接触 JDBC，只依赖 DAO 接口；不接触界面，便于单元测试与后续 GUI 复用。</li>
 * </ul>
 */
public class UserService {

    private static final Logger LOG = Logs.getLogger(UserService.class);
    private static final int DEFAULT_CREDIT = 100;
    private static final int MIN_CREDIT = 0;
    private static final int MAX_CREDIT = 150;

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(DaoFactory daoFactory) {
        this(daoFactory, new PasswordEncoder());
    }

    public UserService(DaoFactory daoFactory, PasswordEncoder passwordEncoder) {
        this.userDao = daoFactory.userDao();
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册普通学生账号。
     */
    public User register(UserCommand command) {
        ValidationUtils.requireNonNull(command, "注册信息");
        String username = ValidationUtils.requireUsername(command.getUsername());
        ValidationUtils.requireStrongPassword(command.getPassword(), "密码");
        if (command.getConfirmPassword() != null
                && !command.getConfirmPassword().equals(command.getPassword())) {
            throw new ValidationException("confirmPassword", "两次输入的密码不一致");
        }
        String nickname = ValidationUtils.requireText(command.getNickname(), "昵称", 2, 20);
        String contact = ValidationUtils.optionalText(command.getContact(), "联系方式", 40);

        if (userDao.existsByUsername(username)) {
            throw new DuplicateException("用户名「" + username + "」已被注册");
        }
        User user = new User(null, username, passwordEncoder.encode(command.getPassword()),
                nickname, contact, Role.STUDENT, UserStatus.ACTIVE,
                DEFAULT_CREDIT, LocalDateTime.now(), null);
        long id = userDao.insert(user);
        LOG.info("用户注册成功，id=" + id + ", username=" + username);
        return findById(id);
    }

    /**
     * 登录校验：用户名不存在与密码错误使用同一提示，避免账号枚举。
     */
    public User login(String username, String rawPassword) {
        String normalized = ValidationUtils.requireUsername(username);
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("密码", "密码不能为空");
        }
        User user = userDao.findByUsername(normalized)
                .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthenticationException("该账号已被封禁，请联系管理员");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("用户名或密码错误");
        }
        userDao.touchLastLogin(user.getId(), LocalDateTime.now());
        LOG.info("用户登录成功，id=" + user.getId() + ", username=" + user.getUsername());
        return findById(user.getId());
    }

    public User findById(long userId) {
        ValidationUtils.requirePositiveId(userId, "用户ID");
        return userDao.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id=" + userId));
    }

    /**
     * 修改密码：先校验旧密码，再校验新密码强度，且新旧密码不能相同。
     */
    public void changePassword(long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("原密码不正确");
        }
        ValidationUtils.requireStrongPassword(newPassword, "新密码");
        if (oldPassword.equals(newPassword)) {
            throw new ValidationException("新密码", "新密码不能与原密码相同");
        }
        int updated = userDao.update(user.withPasswordHash(passwordEncoder.encode(newPassword)));
        if (updated == 0) {
            throw new NotFoundException("用户不存在，id=" + userId);
        }
        LOG.info("用户修改密码，id=" + userId);
    }

    /**
     * 更新昵称与联系方式。
     */
    public User updateProfile(long userId, String nickname, String contact) {
        User user = findById(userId);
        String newNickname = ValidationUtils.requireText(nickname, "昵称", 2, 20);
        String newContact = ValidationUtils.optionalText(contact, "联系方式", 40);
        User updated = user.withProfile(newNickname, newContact);
        userDao.update(updated);
        return findById(userId);
    }

    /**
     * 管理员封禁/解封账号。管理员不能封禁自己，也不能封禁其他管理员。
     */
    public User changeUserStatus(long operatorId, long targetUserId, boolean banned) {
        User operator = requireAdmin(operatorId);
        User target = findById(targetUserId);
        if (operator.getId().equals(target.getId())) {
            throw new PermissionDeniedException("不能封禁自己");
        }
        if (target.isAdmin()) {
            throw new PermissionDeniedException("不能封禁管理员账号");
        }
        UserStatus status = banned ? UserStatus.BANNED : UserStatus.ACTIVE;
        userDao.update(target.withStatus(status));
        LOG.info("管理员 " + operator.getUsername() + " 将用户 " + target.getUsername()
                + " 状态改为 " + status);
        return findById(targetUserId);
    }

    /**
     * 信用分调整（评价模块将复用）：结果限制在 [0, 150]。
     */
    public User adjustCredit(long userId, int delta) {
        User user = findById(userId);
        int score = Math.max(MIN_CREDIT, Math.min(MAX_CREDIT, user.getCreditScore() + delta));
        userDao.update(user.withCreditScore(score));
        return findById(userId);
    }

    /**
     * 管理员查看用户列表，支持用户名/昵称模糊查询。
     */
    public List<User> listUsers(long operatorId, String keyword) {
        requireAdmin(operatorId);
        return userDao.search(keyword, 200);
    }

    private User requireAdmin(long operatorId) {
        User operator = findById(operatorId);
        if (!operator.isAdmin()) {
            throw new PermissionDeniedException("该操作需要管理员权限");
        }
        if (!operator.isActive()) {
            throw new PermissionDeniedException("管理员账号已被封禁");
        }
        return operator;
    }
}
