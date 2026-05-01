package org.test.model;

import java.sql.Timestamp;

/**
 * 投票问卷实体类 —— 承载一个投票题目的全部信息。
 *
 * 每个 VoteQuestion 代表一个投票（比如"周末去哪玩？"）。
 * 它既包含题目本身（标题），也记录了是谁发布的、什么时候发布的。
 *
 * username 字段的来源：
 * 数据库 vote_questions 表里只存 user_id，不存 username。
 * 但在列表页展示时，需要显示发布者的名字。
 * 所以查询时会用 JOIN 把 users 表的 username 一起查出来，
 * 临时存在这个字段里。这就是为什么 username 不是数据库字段
 * 却能出现在实体类中的原因。
 */
public class VoteQuestion {

    /**
     * 问卷编号 —— 数据库自动生成的唯一标识。
     *
     * 为什么要有一个全局唯一的编号？
     * 因为用户要通过 URL 访问某个问卷（如 /vote/result?questionId=5），
     * 没有编号就无法定位到具体的问卷。
     */
    private int id;

    /**
     * 问卷标题 —— 展示给投票者看的问题文字。
     *
     * 例如："你最喜欢的编程语言是？"、"下周团建去哪？"
     */
    private String title;

    /**
     * 发布者编号 —— 指向 users 表中的某个用户。
     *
     * 为什么存编号而不是存名字？
     * 因为名字可能重复（两个人都叫"张三"），但编号不会。
     * 用编号做关联是数据库设计的基本做法。
     */
    private int userId;

    /**
     * 发布者用户名 —— 查询时从 users 表 JOIN 过来的临时数据。
     *
     * 这个字段不对应 vote_questions 表中的任何列，
     * 纯粹是为了方便页面显示。
     */
    private String username;

    /**
     * 发布时间 —— 问卷创建时自动记录。
     *
     * 列表中按这个时间倒序排列，越新的越靠前。
     */
    private Timestamp createdAt;

    /**
     * 问卷状态 —— 在生命周期中所处的阶段。
     *
     * 三种状态：
     *   pending   = 待审批（用户刚发布，等待管理员审批）
     *   approved  = 已通过（管理员审批通过，用户可投票）
     *   ended     = 已结束（投票关闭，不可再投票，结果仍可查看）
     */
    private String status;

    // ==================== getter / setter ====================

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
