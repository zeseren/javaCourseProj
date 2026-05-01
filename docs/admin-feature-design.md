# 管理员功能概要设计

本文档是针对校园问卷投票系统 **管理员功能** 的增量概要设计，需与 `outline-design.md` 配合阅读。

## 1. 设计原则

- 最小改动原则：尽量在现有表结构上新增字段，避免新建表。
- 向后兼容：现有数据和功能不受影响，新增字段均设置合理的默认值。
- 保持 MVC 结构：不引入新框架或复杂中间层。

## 2. 数据库变更

### 2.1 users 表变更

新增 `role` 字段，用于区分普通用户和管理员。

```sql
-- 添加 role 字段（默认值为 'user'）
alter table users add column if not exists role varchar(20) not null default 'user';
```

字段说明：

- `role`：用户角色，可选值 `user`（普通用户）或 `admin`（管理员）。
- 默认值为 `user`，确保现有数据自动兼容。

### 2.2 vote_questions 表变更

新增 `status` 字段，用于标记问卷的审批和状态。

```sql
-- 添加 status 字段（默认值为 'approved'，确保现有问卷不受影响）
alter table vote_questions add column if not exists status varchar(20) not null default 'approved';
```

字段说明：

- `status`：问卷状态，可选值：
  - `pending`：待审批（用户发布后、管理员审批前）。
  - `approved`：已通过（管理员审批通过后，用户可以投票）。
  - `ended`：已结束（投票关闭，不可再投票，结果仍可查看）。
- 默认值为 `approved`，确保系统中已有的历史问卷自动变为"已通过"状态，不受影响。

### 2.3 完整建表语句（更新后）

```sql
create table if not exists users (
    id serial primary key,
    username varchar(50) not null unique,
    password varchar(255) not null,
    role varchar(20) not null default 'user',
    created_at timestamp not null default current_timestamp
);

create table if not exists vote_questions (
    id serial primary key,
    title varchar(200) not null,
    user_id integer not null references users(id),
    status varchar(20) not null default 'approved',
    created_at timestamp not null default current_timestamp
);

create table if not exists vote_options (
    id serial primary key,
    question_id integer not null references vote_questions(id) on delete cascade,
    content varchar(200) not null,
    vote_count integer not null default 0
);
```

## 3. 实体类变更

### 3.1 User 实体（更新）

新增字段：

```text
role: String  （新增，取值 "user" 或 "admin"）
```

完整字段：

```text
id: int
username: String
password: String（bcrypt 哈希值）
role: String（"user" / "admin"）
createdAt: Timestamp
```

### 3.2 VoteQuestion 实体（更新）

新增字段：

```text
status: String  （新增，取值 "pending" / "approved" / "ended"）
```

完整字段：

```text
id: int
title: String
userId: int
username: String（展示字段，非数据库字段）
status: String（"pending" / "approved" / "ended"）
createdAt: Timestamp
```

### 3.3 VoteOption 实体

无变更。

## 4. 数据访问方法变更

### 4.1 UserModel（新增方法）

```text
isAdmin(int userId) : boolean
```

用途：查询指定用户是否为管理员。根据用户 ID 查询 `role` 字段。

### 4.2 VoteModel（变更与新增方法）

**变更方法：**

```text
findAllQuestions() → 修改为带状态过滤的查询
```

原实现查询所有问卷。修改后：
- 该方法改为接收一个布尔参数或重载：一个返回"普通用户可见的问卷"（即 `status = 'approved'` 或 `status = 'ended'`），一个返回"管理员可见的全部问卷"。

**或**保留原方法，新增两个方法：

```text
findQuestionsForUser()           → 查询已通过和已结束的问卷（普通用户视角）
findAllQuestionsForAdmin()       → 查询所有问卷（管理员视角）
```

**新增方法：**

```text
approveQuestion(int questionId)              → 将问卷状态改为 'approved'
endQuestion(int questionId)                  → 将问卷状态改为 'ended'
isQuestionOwnerOrAdmin(int questionId, int userId) → 判断是否发布者或管理员
```

## 5. Servlet 变更

### 5.1 新增 Servlet

**AdminServlet**（`/admin`）

通过 `action` 参数区分操作：

```text
/admin?action=approve&questionId=xxx
/admin?action=end&questionId=xxx
```

- GET：暂不处理（管理功能在问卷列表页面中展示）。
- POST `action=approve`：审批通过问卷。校验管理员身份，将问卷状态改为 `approved`，重定向到 `/vote/list`。
- POST `action=end`：结束问卷。校验管理员或发布者身份，将问卷状态改为 `ended`，重定向到 `/vote/list`。

### 5.2 VotePublishServlet（变更）

POST 发布问卷时，`createQuestion` 方法将问卷 `status` 设为 `pending`（而非原来的无状态）。

发布成功后提示信息改为："问卷已提交，等待管理员审批。"

### 5.3 VoteListServlet（变更）

GET：根据用户角色查询不同范围的问卷。

- 普通用户：调用 `findQuestionsForUser()`，只显示 `approved` 和 `ended` 状态的问卷。
- 管理员：调用 `findAllQuestionsForAdmin()`，显示所有状态的问卷（待审批、已通过、已结束分组展示）。

### 5.4 VoteSubmitServlet（变更）

GET/POST：新增状态校验——如果问卷状态不是 `approved`，拒绝投票。

### 5.5 VoteDeleteServlet（变更）

GET/POST：权限校验从"是否为发布者"改为"是否为发布者或管理员"（调用 `isQuestionOwnerOrAdmin`）。

## 6. JSP 页面变更

### 6.1 vote_list.jsp（变更）

**普通用户视角：**
- 只显示"已通过"和"已结束"的问卷。
- "已结束"的问卷不显示"去投票"按钮，显示"已结束"标记。
- 对自己发布的"已通过"问卷，额外显示"结束"按钮。
- 对自己发布的问卷，显示"删除"按钮。

**管理员视角：**
- 顶部显示"待审批"区域，列出所有 `pending` 问卷。
- 每个待审批问卷旁显示"审批通过"按钮。
- 已通过区域中，每个问卷旁显示"结束"按钮和"删除"按钮。
- 管理员自己发布的问卷同样遵循以上规则。
- 审批按钮提交到 `POST /admin?action=approve&questionId=xxx`。
- 结束按钮提交到 `POST /admin?action=end&questionId=xxx`。

### 6.2 vote_submit.jsp（变更）

- 问卷标题旁显示问卷状态标签（如"已结束"）。
- 已结束的问卷隐藏投票表单，只显示"该问卷已结束"的提示和"查看结果"链接。

### 6.3 vote_publish.jsp（变更）

- 发布成功后的提示信息变更。

## 7. 页面跳转关系（更新）

在原有基础上新增：

```text
vote_list.jsp（管理员）
  -> 点击审批：POST /admin?action=approve&questionId=xxx → /vote/list
  -> 点击结束：POST /admin?action=end&questionId=xxx → /vote/list
  -> 点击删除：/vote/delete?questionId=xxx → vote_delete.jsp
```

## 8. 管理员账号初始化

在数据库中执行以下 SQL 创建管理员账号：

```sql
-- 创建一个管理员用户（密码需要后续通过应用注册或手动设置）
-- 方式一：直接插入（密码为 bcrypt 哈希，实际使用时替换为真实哈希值）
-- insert into users (username, password, role) values ('admin', '此处填写bcrypt哈希', 'admin');

-- 方式二：先注册普通用户，再将其角色提升为管理员
-- update users set role = 'admin' where username = 'admin';
```

由于密码使用 bcrypt 哈希存储，建议在后续编码中提供便捷的管理员初始化方法（如通过 SQL 脚本插入初始管理员，密码同样用应用层的 bcrypt 工具生成哈希值）。

## 9. Session 变更

登录成功后，`currentUser`（存在 Session 中的 `User` 对象）需要包含 `role` 字段，以便 JSP 和 Servlet 判断用户是否为管理员。

## 10. 关键校验规则（新增与变更）

审批：
- 必须登录。
- 必须是管理员（`role == "admin"`）。
- 问卷必须存在。
- 问卷状态必须为 `pending`。

结束：
- 必须登录。
- 必须是管理员或问卷发布者。
- 问卷必须存在。
- 问卷状态必须为 `approved`。

删除（变更）：
- 必须登录。
- 必须是管理员或问卷发布者（原规则：发布者本人）。

发布（变更）：
- 新发布问卷的 `status` 设为 `pending`。

列表（变更）：
- 普通用户只显示 `approved` 和 `ended` 的问卷。
- 管理员显示所有状态的问卷。

投票（新增）：
- 问卷状态必须为 `approved`，否则拒绝投票。

## 11. 后续编码顺序建议

1. 修改数据库：新增 `role` 和 `status` 字段。
2. 更新实体类 `User`（加 `role`）和 `VoteQuestion`（加 `status`）。
3. 更新 `UserModel`（加 `isAdmin` 方法，登录时读取 `role`）。
4. 更新 `VoteModel`（加状态相关方法，修改查询逻辑）。
5. 更新 `VoteListServlet`（区分用户/管理员视角）。
6. 更新 `VotePublishServlet`（发布时设置 `pending` 状态）。
7. 新增 `AdminServlet`（处理审批和结束操作）。
8. 更新 `VoteDeleteServlet`（权限校验加入管理员）。
9. 更新 `VoteSubmitServlet`（状态校验）。
10. 更新 `vote_list.jsp`（区分用户/管理员 UI，增加审批/结束按钮）。
11. 更新 `vote_submit.jsp`（已结束状态提示）。
12. 初始化管理员账号，联调测试。
