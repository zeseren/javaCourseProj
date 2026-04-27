# 校园问卷投票系统概要设计

## 1. 设计说明

本项目是一个简单课程设计，采用 JSP + Servlet + JDBC + PostgreSQL 实现。

根据需求分析阶段的确认，本系统只实现“一个问卷包含一个投票题目和多个选项”的简单投票场景，不实现多题问卷、复杂权限、管理员后台等扩展功能。

本阶段按用户要求省略 DIO 相关设计，不额外增加复杂中间层。系统仍保持 MVC 基本结构：

- Model：实体类和数据库访问代码。
- View：JSP 页面。
- Controller：Servlet 控制器。

为了让课程设计更容易理解，本概要设计避免过多抽象，优先保证结构清楚、功能完整、代码可讲解。

## 2. 技术选型

- Java 8
- Jakarta Servlet 6.1
- JSP
- JDBC
- PostgreSQL
- Maven WAR 项目
- Tomcat 10 或其他 Jakarta Servlet 容器

说明：

- 当前项目使用 `jakarta.servlet.*`，因此运行环境需要 Tomcat 10+。
- PostgreSQL JDBC 驱动后续编码阶段需要加入 `pom.xml`。

## 3. 项目目录设计

建议 Java 包结构：

```text
src/main/java/
  org/test/model/
    User.java
    VoteQuestion.java
    VoteOption.java

  org/test/servlet/
    UserServlet.java
    VotePublishServlet.java
    VoteListServlet.java
    VoteSubmitServlet.java
    VoteResultServlet.java
    VoteDeleteServlet.java

  org/test/util/
    JdbcUtil.java
```

说明：

- `model` 包保存实体类，也可以放少量与实体相关的数据访问方法，避免课程设计层次过多。
- `servlet` 包保存所有控制器。
- `util` 包保存 JDBC 连接工具。
- 不让 JSP 直接连接数据库，页面只负责显示数据。

建议 JSP 目录：

```text
src/main/webapp/
  index.jsp
  login.jsp
  register.jsp
  vote_publish.jsp
  vote_list.jsp
  vote_submit.jsp
  vote_result.jsp
  vote_delete.jsp
```

`index.jsp` 建议只做入口跳转：

- 未登录：跳转 `login.jsp`
- 已登录：跳转 `/vote/list`

## 4. 数据库表设计

数据库名称建议：`campus_vote`

### 4.1 users 表

保存用户信息。

```sql
create table users (
    id serial primary key,
    username varchar(50) not null unique,
    password varchar(100) not null,
    created_at timestamp not null default current_timestamp
);
```

字段说明：

- `id`：用户主键。
- `username`：用户名，不允许重复。
- `password`：密码。课程设计阶段可以直接保存，实际项目应使用加密哈希。
- `created_at`：注册时间。

### 4.2 vote_questions 表

保存投票问卷。

```sql
create table vote_questions (
    id serial primary key,
    title varchar(200) not null,
    user_id integer not null references users(id),
    created_at timestamp not null default current_timestamp
);
```

字段说明：

- `id`：问卷主键。
- `title`：问卷标题。
- `user_id`：发布者用户 ID。
- `created_at`：发布时间。

### 4.3 vote_options 表

保存问卷选项和票数。

```sql
create table vote_options (
    id serial primary key,
    question_id integer not null references vote_questions(id) on delete cascade,
    content varchar(200) not null,
    vote_count integer not null default 0
);
```

字段说明：

- `id`：选项主键。
- `question_id`：所属问卷 ID。
- `content`：选项内容。
- `vote_count`：当前票数。

删除规则：

- `vote_options.question_id` 使用 `on delete cascade`。
- 删除问卷时，数据库会自动删除该问卷下的所有选项。

## 5. 实体类设计

### 5.1 User

字段：

```text
id: int
username: String
password: String
createdAt: Timestamp
```

用途：

- 保存登录用户信息。
- 登录成功后放入 Session。

### 5.2 VoteQuestion

字段：

```text
id: int
title: String
userId: int
username: String
createdAt: Timestamp
```

说明：

- `username` 不是数据库表中的字段，而是查询列表时为了显示发布者名称附带出来的展示字段。
- 这样 JSP 可以直接显示发布者，不需要再次查询用户表。

### 5.3 VoteOption

字段：

```text
id: int
questionId: int
content: String
voteCount: int
percent: double
```

说明：

- `percent` 是结果页面展示用字段，不保存到数据库。
- 统计结果时由 Servlet 或查询方法计算。

## 6. JDBC 工具设计

工具类：`JdbcUtil`

职责：

- 加载 PostgreSQL JDBC 驱动。
- 提供数据库连接。
- 统一关闭 `Connection`、`PreparedStatement`、`ResultSet`。

建议方法：

```text
getConnection()
close(ResultSet rs, PreparedStatement ps, Connection conn)
close(PreparedStatement ps, Connection conn)
```

连接信息建议：

```text
url      = jdbc:postgresql://localhost:5432/campus_vote
username = postgres
password = 自己本机数据库密码
```

说明：

- 课程设计阶段可以先写在 `JdbcUtil` 常量中。
- 如果后续需要更规范，可以改为读取配置文件。

## 7. 数据访问方法设计

因为本项目省略复杂分层，数据访问方法可以写成简单的静态工具方法，或者写在对应 Model 辅助类中。

建议为了清楚，后续编码阶段可以建立以下简单类：

```text
org/test/model/UserModel.java
org/test/model/VoteModel.java
```

这样既不引入完整 DAO 层，又能避免 Servlet 中堆太多 SQL。

### 7.1 UserModel 方法

```text
findByUsername(String username)
findByUsernameAndPassword(String username, String password)
createUser(String username, String password)
```

用途：

- 注册时检查用户名是否存在。
- 登录时校验用户名和密码。
- 注册成功时保存用户。

### 7.2 VoteModel 方法

```text
createQuestion(int userId, String title, List<String> options)
findAllQuestions()
findQuestionById(int questionId)
findOptionsByQuestionId(int questionId)
increaseOptionVoteCount(int optionId)
isQuestionOwner(int questionId, int userId)
deleteQuestion(int questionId)
```

用途：

- 发布问卷。
- 查询问卷列表。
- 查询投票页面数据。
- 增加票数。
- 判断删除权限。
- 删除问卷。

## 8. Servlet 路由设计

### 8.1 UserServlet

访问路径：

```text
/user
```

通过 `action` 参数区分操作：

```text
/user?action=login
/user?action=register
/user?action=logout
```

GET：

- `action=login`：进入登录页。
- `action=register`：进入注册页。
- `action=logout`：退出登录。

POST：

- `action=login`：处理登录表单。
- `action=register`：处理注册表单。

这样设计可以少写 Servlet 类，适合简单课程项目。

### 8.2 VotePublishServlet

访问路径：

```text
/vote/publish
```

GET：

- 检查是否登录。
- 已登录则进入 `vote_publish.jsp`。

POST：

- 检查是否登录。
- 接收标题和选项。
- 校验至少两个有效选项。
- 保存问卷。
- 重定向到 `/vote/list`。

### 8.3 VoteListServlet

访问路径：

```text
/vote/list
```

GET：

- 检查是否登录。
- 查询所有问卷。
- 转发到 `vote_list.jsp`。

### 8.4 VoteSubmitServlet

访问路径：

```text
/vote/submit
```

GET：

- 检查是否登录。
- 根据 `questionId` 查询问卷和选项。
- 检查 Session 中是否已投票。
- 未投票则进入 `vote_submit.jsp`。
- 已投票则提示并提供查看结果入口。

POST：

- 检查是否登录。
- 检查 Session 中是否已投票。
- 接收 `questionId` 和 `optionId`。
- 对选项票数加 1。
- 将 `questionId` 放入 Session 已投票集合。
- 重定向到 `/vote/result?questionId=xxx`。

### 8.5 VoteResultServlet

访问路径：

```text
/vote/result
```

GET：

- 检查是否登录。
- 根据 `questionId` 查询问卷和选项。
- 计算总票数。
- 计算每个选项百分比。
- 转发到 `vote_result.jsp`。

### 8.6 VoteDeleteServlet

访问路径：

```text
/vote/delete
```

GET：

- 检查是否登录。
- 检查问卷是否存在。
- 检查当前用户是否为发布者。
- 进入 `vote_delete.jsp` 确认页。

POST：

- 检查是否登录。
- 检查当前用户是否为发布者。
- 删除问卷。
- 重定向到 `/vote/list`。

## 9. JSP 页面设计

### 9.1 login.jsp

输入字段：

```text
username
password
```

显示内容：

- 登录表单。
- 错误提示。
- 注册入口。
- 最近登录用户名 Cookie 回显。

提交地址：

```text
POST /user?action=login
```

### 9.2 register.jsp

输入字段：

```text
username
password
confirmPassword
```

显示内容：

- 注册表单。
- 错误提示。
- 返回登录入口。

提交地址：

```text
POST /user?action=register
```

### 9.3 vote_publish.jsp

输入字段：

```text
title
options
```

说明：

- `options` 可以使用多个同名输入框。
- Servlet 使用 `request.getParameterValues("options")` 接收。

提交地址：

```text
POST /vote/publish
```

### 9.4 vote_list.jsp

需要 request 数据：

```text
questions
currentUser
```

显示内容：

- 问卷标题。
- 发布者。
- 发布时间。
- 投票入口。
- 结果入口。
- 当前用户是发布者时显示删除入口。

### 9.5 vote_submit.jsp

需要 request 数据：

```text
question
options
error
alreadyVoted
```

显示内容：

- 问卷标题。
- 单选按钮选项。
- 提交按钮。
- 已投票提示。

提交地址：

```text
POST /vote/submit
```

### 9.6 vote_result.jsp

需要 request 数据：

```text
question
options
totalVotes
```

显示内容：

- 问卷标题。
- 每个选项的票数。
- 每个选项的百分比。
- 总票数。
- 返回列表入口。

### 9.7 vote_delete.jsp

需要 request 数据：

```text
question
error
```

显示内容：

- 删除确认信息。
- 确认删除按钮。
- 取消返回列表按钮。

提交地址：

```text
POST /vote/delete
```

## 10. Session 与 Cookie 设计

### 10.1 Session 字段

保存当前登录用户：

```text
session key: currentUser
value: User
```

保存当前会话已投票问卷：

```text
session key: votedQuestionIds
value: Set<Integer>
```

说明：

- 用户提交投票成功后，将 `questionId` 加入 `votedQuestionIds`。
- 再次提交同一问卷时，如果集合中已经存在该 ID，则拒绝提交。

### 10.2 Cookie 字段

保存最近登录用户名：

```text
cookie name: lastUsername
value: username
```

说明：

- 登录成功后写入 Cookie。
- 登录页读取该 Cookie 并回显用户名。
- 不保存密码。

## 11. 页面跳转关系

```text
index.jsp
  -> 未登录：login.jsp
  -> 已登录：/vote/list

login.jsp
  -> 登录成功：/vote/list
  -> 登录失败：login.jsp

register.jsp
  -> 注册成功：login.jsp
  -> 注册失败：register.jsp

vote_publish.jsp
  -> 发布成功：/vote/list
  -> 发布失败：vote_publish.jsp

vote_list.jsp
  -> 点击投票：/vote/submit?questionId=xxx
  -> 点击结果：/vote/result?questionId=xxx
  -> 点击删除：/vote/delete?questionId=xxx

vote_submit.jsp
  -> 投票成功：/vote/result?questionId=xxx
  -> 投票失败：vote_submit.jsp

vote_delete.jsp
  -> 删除成功：/vote/list
  -> 取消删除：/vote/list
```

## 12. 关键校验规则

登录注册：

- 用户名不能为空。
- 密码不能为空。
- 注册时两次密码必须一致。
- 注册用户名不能重复。
- 登录用户名或密码错误时提示。

发布问卷：

- 标题不能为空。
- 至少两个非空选项。
- 空白选项不保存。

投票：

- 必须登录。
- 问卷必须存在。
- 选项必须存在。
- 选项必须属于当前问卷。
- 同一 Session 内不能重复投票。

删除：

- 必须登录。
- 问卷必须存在。
- 只有发布者本人可以删除。

## 13. 后续编码顺序建议

编码阶段建议按以下顺序实现：

1. 修改 `pom.xml`，加入 PostgreSQL JDBC 驱动。
2. 创建数据库和三张表。
3. 编写 `JdbcUtil`。
4. 编写三个实体类。
5. 编写 `UserModel` 和 `VoteModel`。
6. 编写登录注册 JSP 和 `UserServlet`。
7. 编写问卷列表、发布功能。
8. 编写投票提交、结果统计功能。
9. 编写删除确认和删除功能。
10. 完整联调并修正页面提示。

## 14. 本阶段待确认事项

进入编码阶段前，需要确认：

- 是否同意添加 PostgreSQL JDBC 驱动依赖。
- 数据库连接信息是否采用默认值：`localhost:5432/campus_vote`。
- 数据库用户名是否使用 `postgres`。
- 密码是否由用户在本机自行填写，还是现在直接给出。

确认概要设计通过后，可以进入编码实现阶段。
