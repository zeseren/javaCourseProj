package org.test.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User 实体类单元测试。
 *
 * 实体类只有 getter/setter，不需要连接数据库。
 * 测试目标是确保字段赋值和读取行为正确。
 */
class UserTest {

    @Test
    void shouldSetAndGetId() {
        User user = new User();
        user.setId(1);
        assertEquals(1, user.getId());
    }

    @Test
    void shouldSetAndGetUsername() {
        User user = new User();
        user.setUsername("张三");
        assertEquals("张三", user.getUsername());
    }

    @Test
    void shouldSetAndGetPassword() {
        User user = new User();
        user.setPassword("secret123");
        assertEquals("secret123", user.getPassword());
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        User user = new User();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        user.setCreatedAt(now);
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void shouldReturnZeroIdForNewUser() {
        // 新对象没有设置任何值，int 类型默认是 0
        User user = new User();
        assertEquals(0, user.getId());
    }

    @Test
    void shouldReturnNullUsernameForNewUser() {
        // 新对象没有设置任何值，String 类型默认是 null
        User user = new User();
        assertNull(user.getUsername());
    }
}
