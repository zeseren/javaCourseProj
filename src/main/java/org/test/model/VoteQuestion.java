package org.test.model;

import java.sql.Timestamp;

/**
 * 投票问卷实体类。
 *
 * 本课程设计中，一个 VoteQuestion 表示一个简单投票题目。
 * username 是列表展示时使用的发布者名称，不单独保存到 vote_questions 表。
 */
public class VoteQuestion {
    private int id;
    private String title;
    private int userId;
    private String username;
    private Timestamp createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
