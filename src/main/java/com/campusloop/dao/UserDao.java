package com.campusloop.dao;

import com.campusloop.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口：业务层只依赖该接口，便于替换存储实现与编写测试替身。
 */
public interface UserDao {

    /** 插入用户并回填自增主键，返回主键。 */
    long insert(User user);

    /** 整体更新用户记录（按 id），返回受影响行数。 */
    int update(User user);

    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按用户名/昵称模糊查询，keyword 为空时返回全部（管理员用户列表）。 */
    List<User> search(String keyword, int limit);

    /** 更新最后登录时间。 */
    void touchLastLogin(long userId, LocalDateTime time);
}
