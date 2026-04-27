package org.test.model;

import java.sql.Timestamp;

/**
 * 用户实体类。
 *
 * 实体类只负责保存数据，不负责连接数据库，也不负责页面跳转。
 * 这样做的目的是让 Model、View、Controller 的职责更清楚。
 */
public class User {
    private int id;
    private String username;
    private String password;
    private Timestamp createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
