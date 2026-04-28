package org.test.model;

import java.sql.Timestamp;

/**
 * 用户实体类 —— 一颗"数据胶囊"。
 *
 * 这个类本身不做任何复杂的事情，它只是把用户相关的信息装在一起，
 * 方便在各个代码之间传递。就像一个信封，里面装着用户的姓名、密码等信息。
 *
 * 为什么不在这里写数据库操作？
 * ──────────────────────────────
 * 如果让同一个类既保存数据又操作数据库，一旦数据库结构变了，
 * 所有用到用户信息的地方都要跟着改。把"装数据"和"操作数据库"
 * 拆成两个类之后，改数据库不影响其他代码。
 *
 * 生活中的类比：
 * 快递盒（实体类）只负责装东西，快递员（Model 类）负责送到哪里。
 * 盒子不需要知道怎么运输自己。
 */
public class User {

    /**
     * 用户编号 —— 数据库自动生成的唯一标识。
     *
     * 为什么用数字而不是用户名当标识？
     * 用户名可能会改，但编号一辈子不变，用编号当"身份证"
     * 最可靠。
     */
    private int id;

    /**
     * 用户名 —— 登录时使用的账号。
     */
    private String username;

    /**
     * 密码 —— 登录时校验的凭证。
     *
     * 注意：当前课程设计中密码是明文存储的。
     * 真实项目中绝对不能这样做，需要用哈希（如 bcrypt）加密后再存。
     */
    private String password;

    /**
     * 账号创建时间。
     *
     * 记录这个时间是为了以后可能的审计需求，
     * 比如查看"某天之前注册的用户"。
     */
    private Timestamp createdAt;

    // ==================== getter / setter ====================
    //
    // 为什么每个字段都要写 getXxx 和 setXxx？
    // 这是 Java 世界的"行规"（JavaBean 规范）。
    // JSP 页面中写 ${user.username} 时，
    // 底层实际上调用的是 getUsername() 方法。
    // 没有这些方法，JSP 就拿不到数据。

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
