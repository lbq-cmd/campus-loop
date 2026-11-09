package com.campusloop.service;

import com.campusloop.common.exception.AuthenticationException;
import com.campusloop.common.exception.DuplicateException;
import com.campusloop.common.exception.NotFoundException;
import com.campusloop.common.exception.PermissionDeniedException;
import com.campusloop.common.exception.ValidationException;
import com.campusloop.domain.Role;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;
import com.campusloop.support.TestData;
import com.campusloop.support.TestDb;
import com.campusloop.service.dto.UserCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private TestDb testDb;
    private UserService userService;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        userService = new UserService(testDb.daoFactory());
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    private static UserCommand command(String username, String password, String nickname) {
        return new UserCommand()
                .setUsername(username)
                .setPassword(password)
                .setConfirmPassword(password)
                .setNickname(nickname)
                .setContact("QQ123456");
    }

    @Test
    @DisplayName("注册成功：默认学生角色、正常状态、信用分 100，密码不落明文")
    void registerSuccess() {
        User user = userService.register(command("student1", "Abcd1234", "小明"));
        assertNotNull(user.getId());
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(100, user.getCreditScore());
        assertEquals("小明", user.getNickname());
        assertNotEquals("Abcd1234", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("pbkdf2$"));
    }

    @Test
    @DisplayName("重复用户名注册失败")
    void registerDuplicateUsername() {
        userService.register(command("student1", "Abcd1234", "小明"));
        DuplicateException e = assertThrows(DuplicateException.class,
                () -> userService.register(command("student1", "Abcd1234", "小红")));
        assertTrue(e.getMessage().contains("已被注册"));
    }

    @Test
    @DisplayName("弱密码、非法用户名、过短昵称均被拒绝注册")
    void registerRejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> userService.register(command("student1", "12345678", "小明")));
        assertThrows(ValidationException.class, () -> userService.register(command("1student", "Abcd1234", "小明")));
        assertThrows(ValidationException.class, () -> userService.register(command("student1", "Abcd1234", "明")));
    }

    @Test
    @DisplayName("两次密码不一致时拒绝注册")
    void registerRejectsMismatchedConfirm() {
        UserCommand cmd = command("student1", "Abcd1234", "小明").setConfirmPassword("Abcd12345");
        assertThrows(ValidationException.class, () -> userService.register(cmd));
    }

    @Test
    @DisplayName("登录成功返回最新用户并记录登录时间")
    void loginSuccess() {
        userService.register(command("student1", "Abcd1234", "小明"));
        User user = userService.login("student1", "Abcd1234");
        assertEquals("student1", user.getUsername());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    @DisplayName("密码错误与用户不存在返回同样的提示（防止账号枚举）")
    void loginFailuresShareMessage() {
        userService.register(command("student1", "Abcd1234", "小明"));
        AuthenticationException wrongPassword = assertThrows(AuthenticationException.class,
                () -> userService.login("student1", "Wrong1234"));
        AuthenticationException unknownUser = assertThrows(AuthenticationException.class,
                () -> userService.login("nobody", "Abcd1234"));
        assertEquals("用户名或密码错误", wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
    }

    @Test
    @DisplayName("被封禁账号禁止登录")
    void bannedUserCannotLogin() {
        long userId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        userService.changeUserStatus(adminId, userId, true);

        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> userService.login("student1", "Abcd1234"));
        assertTrue(e.getMessage().contains("封禁"));
    }

    @Test
    @DisplayName("修改密码：旧密码错误失败，成功后新密码可登录、旧密码失效")
    void changePassword() {
        long userId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        assertThrows(AuthenticationException.class,
                () -> userService.changePassword(userId, "Wrong1234", "Newpass123"));
        assertThrows(ValidationException.class,
                () -> userService.changePassword(userId, "Abcd1234", "Abcd1234"));
        assertThrows(ValidationException.class,
                () -> userService.changePassword(userId, "Abcd1234", "12345678"));

        userService.changePassword(userId, "Abcd1234", "Newpass123");
        assertDoesNotThrowLogin("student1", "Newpass123");
        assertThrows(AuthenticationException.class, () -> userService.login("student1", "Abcd1234"));
    }

    private void assertDoesNotThrowLogin(String username, String password) {
        User user = userService.login(username, password);
        assertNotNull(user);
    }

    @Test
    @DisplayName("更新昵称与联系方式")
    void updateProfile() {
        long userId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        User updated = userService.updateProfile(userId, "明同学", "wx: ming");
        assertEquals("明同学", updated.getNickname());
        assertEquals("wx: ming", updated.getContact());
    }

    @Test
    @DisplayName("管理员可封禁与解封普通用户")
    void adminCanBanAndUnban() {
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        long userId = userService.register(command("student1", "Abcd1234", "小明")).getId();

        User banned = userService.changeUserStatus(adminId, userId, true);
        assertEquals(UserStatus.BANNED, banned.getStatus());
        User unbanned = userService.changeUserStatus(adminId, userId, false);
        assertEquals(UserStatus.ACTIVE, unbanned.getStatus());
    }

    @Test
    @DisplayName("普通用户不能执行后台封禁操作")
    void studentCannotBan() {
        long studentId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        long targetId = userService.register(command("student2", "Abcd1234", "小红")).getId();
        assertThrows(PermissionDeniedException.class,
                () -> userService.changeUserStatus(studentId, targetId, true));
    }

    @Test
    @DisplayName("管理员不能封禁自己，也不能封禁其他管理员")
    void adminCannotBanAdmin() {
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        long secondAdminId = TestData.createAdmin(testDb.userDao(), "root2");
        assertThrows(PermissionDeniedException.class,
                () -> userService.changeUserStatus(adminId, adminId, true));
        assertThrows(PermissionDeniedException.class,
                () -> userService.changeUserStatus(adminId, secondAdminId, true));
    }

    @Test
    @DisplayName("信用分调整在 [0,150] 区间内截断")
    void creditScoreClamped() {
        long userId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        assertEquals(150, userService.adjustCredit(userId, 100).getCreditScore());
        assertEquals(140, userService.adjustCredit(userId, -10).getCreditScore());
        assertEquals(0, userService.adjustCredit(userId, -500).getCreditScore());
    }

    @Test
    @DisplayName("管理员可分页查询用户，普通用户无权限")
    void listUsersRequiresAdmin() {
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        long studentId = userService.register(command("student1", "Abcd1234", "小明")).getId();
        userService.register(command("student2", "Abcd1234", "小红"));

        List<User> all = userService.listUsers(adminId, null);
        assertEquals(3, all.size());
        assertEquals(1, userService.listUsers(adminId, "student1").size());
        assertThrows(PermissionDeniedException.class, () -> userService.listUsers(studentId, null));
    }

    @Test
    @DisplayName("查询不存在的用户抛出 NotFoundException")
    void findMissingUser() {
        assertThrows(NotFoundException.class, () -> userService.findById(99999));
        assertThrows(ValidationException.class, () -> userService.findById(0));
    }
}
