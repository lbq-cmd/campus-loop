package com.campusloop.dao.jdbc;

import com.campusloop.common.exception.DataAccessException;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;
import com.campusloop.support.TestData;
import com.campusloop.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserDaoTest {

    private TestDb testDb;
    private JdbcUserDao userDao;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        userDao = new JdbcUserDao(testDb.database());
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    @Test
    @DisplayName("新增用户后可回读全部字段")
    void insertAndFindBack() {
        long id = userDao.insert(TestData.user("zhangsan"));
        assertTrue(id > 0);

        User stored = userDao.findById(id).orElseThrow();
        assertEquals("zhangsan", stored.getUsername());
        assertEquals("昵称zhangsan", stored.getNickname());
        assertEquals(100, stored.getCreditScore());
        assertTrue(stored.getPasswordHash().startsWith("pbkdf2$"));
        assertTrue(stored.getLastLoginAt() == null);
    }

    @Test
    @DisplayName("用户名唯一约束被触发时转换为 DataAccessException")
    void duplicateUsernameRejected() {
        userDao.insert(TestData.user("lisi"));
        assertThrows(DataAccessException.class, () -> userDao.insert(TestData.user("lisi")));
    }

    @Test
    @DisplayName("按用户名查询：存在返回对象，不存在返回空 Optional")
    void findByUsername() {
        userDao.insert(TestData.user("wangwu"));
        assertTrue(userDao.findByUsername("wangwu").isPresent());
        assertFalse(userDao.findByUsername("nobody").isPresent());
        assertFalse(userDao.findByUsername(null).isPresent());
    }

    @Test
    @DisplayName("整体更新覆盖昵称、状态、信用分与登录时间")
    void updateFields() {
        long id = userDao.insert(TestData.user("zhaoliu"));
        User user = userDao.findById(id).orElseThrow();
        LocalDateTime loginTime = LocalDateTime.of(2026, 11, 1, 10, 30);
        User changed = user.withProfile("新昵称", "QQ10001")
                .withStatus(UserStatus.BANNED)
                .withCreditScore(88)
                .withLastLoginAt(loginTime);
        assertEquals(1, userDao.update(changed));

        User stored = userDao.findById(id).orElseThrow();
        assertEquals("新昵称", stored.getNickname());
        assertEquals("QQ10001", stored.getContact());
        assertEquals(UserStatus.BANNED, stored.getStatus());
        assertEquals(88, stored.getCreditScore());
        assertEquals(loginTime, stored.getLastLoginAt());
    }

    @Test
    @DisplayName("更新不存在的用户返回 0 行")
    void updateMissingUserReturnsZero() {
        User ghost = TestData.user("ghost");
        User withId = new User(999L, ghost.getUsername(), ghost.getPasswordHash(), ghost.getNickname(),
                ghost.getContact(), ghost.getRole(), ghost.getStatus(), 100, ghost.getCreatedAt(), null);
        assertEquals(0, userDao.update(withId));
    }

    @Test
    @DisplayName("模糊查询支持用户名与昵称，并转义 LIKE 通配符")
    void searchByKeyword() {
        userDao.insert(TestData.user("zhangsan"));
        userDao.insert(TestData.user("zhangsi"));
        userDao.insert(TestData.user("lisi"));

        List<User> matched = userDao.search("zhang", 10);
        assertEquals(2, matched.size());
        assertEquals(3, userDao.search(null, 10).size());
        assertEquals(0, userDao.search("%", 10).size(), "% 应被当作普通字符而非通配符");
    }

    @Test
    @DisplayName("限制查询条数生效")
    void searchRespectsLimit() {
        for (int i = 0; i < 5; i++) {
            userDao.insert(TestData.user("user" + i));
        }
        assertEquals(2, userDao.search("user", 2).size());
    }

    @Test
    @DisplayName("登录时间单独更新")
    void touchLastLogin() {
        long id = userDao.insert(TestData.user("sunqi"));
        LocalDateTime time = LocalDateTime.of(2026, 11, 8, 9, 0);
        userDao.touchLastLogin(id, time);
        assertEquals(time, userDao.findById(id).orElseThrow().getLastLoginAt());
    }

    @Test
    @DisplayName("查询不存在的 id 返回空 Optional")
    void findByIdMissing() {
        Optional<User> missing = userDao.findById(123456);
        assertFalse(missing.isPresent());
        assertNotNull(userDao);
    }
}
