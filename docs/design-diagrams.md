# 校园问卷投票系统设计图

本文档用于补充概要设计中的图形化说明，所有图均使用 Mermaid 编写，方便在支持 Mermaid 的 Markdown 工具中直接预览。

本系统是简单课程设计版本，主要围绕以下核心对象展开：

- 用户：负责注册、登录、发布问卷、投票、查看结果、删除自己的问卷。
- 问卷：表示一个投票题目。
- 选项：表示某个问卷下可选择的投票项，并保存当前票数。

## 1. ER 图

ER 图用于说明数据库中表与表之间的关系。

```mermaid
erDiagram
    USERS ||--o{ VOTE_QUESTIONS : publishes
    VOTE_QUESTIONS ||--|{ VOTE_OPTIONS : contains

    USERS {
        int id PK
        varchar username UK
        varchar password
        timestamp created_at
    }

    VOTE_QUESTIONS {
        int id PK
        varchar title
        int user_id FK
        timestamp created_at
    }

    VOTE_OPTIONS {
        int id PK
        int question_id FK
        varchar content
        int vote_count
    }
```

关系说明：

- 一个用户可以发布多个问卷。
- 一个问卷只能属于一个发布用户。
- 一个问卷必须包含多个选项。
- 一个选项只能属于一个问卷。
- 删除问卷时，该问卷下的选项一并删除。

## 2. 数据表关系图

该图更接近数据库表结构，重点展示主键、外键和字段类型。

```mermaid
erDiagram
    users {
        serial id PK "用户ID"
        varchar username UK "用户名"
        varchar password "密码"
        timestamp created_at "注册时间"
    }

    vote_questions {
        serial id PK "问卷ID"
        varchar title "问卷标题"
        integer user_id FK "发布者ID"
        timestamp created_at "发布时间"
    }

    vote_options {
        serial id PK "选项ID"
        integer question_id FK "问卷ID"
        varchar content "选项内容"
        integer vote_count "票数"
    }

    users ||--o{ vote_questions : "id = user_id"
    vote_questions ||--|{ vote_options : "id = question_id"
```

## 3. 类图

类图用于说明 Java 代码中的主要类、字段、方法和依赖关系。

```mermaid
classDiagram
    class User {
        -int id
        -String username
        -String password
        -Timestamp createdAt
        +getId() int
        +setId(int id) void
        +getUsername() String
        +setUsername(String username) void
        +getPassword() String
        +setPassword(String password) void
        +getCreatedAt() Timestamp
        +setCreatedAt(Timestamp createdAt) void
    }

    class VoteQuestion {
        -int id
        -String title
        -int userId
        -String username
        -Timestamp createdAt
        +getId() int
        +setId(int id) void
        +getTitle() String
        +setTitle(String title) void
        +getUserId() int
        +setUserId(int userId) void
        +getUsername() String
        +setUsername(String username) void
        +getCreatedAt() Timestamp
        +setCreatedAt(Timestamp createdAt) void
    }

    class VoteOption {
        -int id
        -int questionId
        -String content
        -int voteCount
        -double percent
        +getId() int
        +setId(int id) void
        +getQuestionId() int
        +setQuestionId(int questionId) void
        +getContent() String
        +setContent(String content) void
        +getVoteCount() int
        +setVoteCount(int voteCount) void
        +getPercent() double
        +setPercent(double percent) void
    }

    class UserModel {
        +findByUsername(String username) User
        +findByUsernameAndPassword(String username, String password) User
        +createUser(String username, String password) boolean
    }

    class VoteModel {
        +createQuestion(int userId, String title, List~String~ options) boolean
        +findAllQuestions() List~VoteQuestion~
        +findQuestionById(int questionId) VoteQuestion
        +findOptionsByQuestionId(int questionId) List~VoteOption~
        +increaseOptionVoteCount(int optionId) boolean
        +isQuestionOwner(int questionId, int userId) boolean
        +deleteQuestion(int questionId) boolean
    }

    class JdbcUtil {
        +getConnection() Connection
        +close(ResultSet rs, PreparedStatement ps, Connection conn) void
        +close(PreparedStatement ps, Connection conn) void
    }

    class PasswordUtil {
        +hash(String plainPassword) String
        +verify(String plainPassword, String hash) boolean
    }

    User "1" --> "0..*" VoteQuestion : publishes
    VoteQuestion "1" --> "2..*" VoteOption : contains
    UserModel ..> User : creates/finds
    UserModel ..> JdbcUtil : uses
    UserModel ..> PasswordUtil : uses
    VoteModel ..> VoteQuestion : creates/finds
    VoteModel ..> VoteOption : creates/finds
    VoteModel ..> JdbcUtil : uses
```

## 4. Servlet 控制类图

该图展示 Servlet、Model 和 JSP 的协作关系。

```mermaid
classDiagram
    class UserServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
        +doPost(HttpServletRequest request, HttpServletResponse response) void
        -login(request, response) void
        -register(request, response) void
        -logout(request, response) void
    }

    class VotePublishServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
        +doPost(HttpServletRequest request, HttpServletResponse response) void
    }

    class VoteListServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
    }

    class VoteSubmitServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
        +doPost(HttpServletRequest request, HttpServletResponse response) void
    }

    class VoteResultServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
    }

    class VoteDeleteServlet {
        +doGet(HttpServletRequest request, HttpServletResponse response) void
        +doPost(HttpServletRequest request, HttpServletResponse response) void
    }

    class UserModel
    class VoteModel
    class JSP

    UserServlet ..> UserModel : calls
    VotePublishServlet ..> VoteModel : calls
    VoteListServlet ..> VoteModel : calls
    VoteSubmitServlet ..> VoteModel : calls
    VoteResultServlet ..> VoteModel : calls
    VoteDeleteServlet ..> VoteModel : calls

    UserServlet ..> JSP : forwards
    VotePublishServlet ..> JSP : forwards
    VoteListServlet ..> JSP : forwards
    VoteSubmitServlet ..> JSP : forwards
    VoteResultServlet ..> JSP : forwards
    VoteDeleteServlet ..> JSP : forwards
```

## 5. 用例图

用例图用于说明用户可以完成哪些系统功能。

```mermaid
flowchart LR
    User[普通用户]

    Register([注册])
    Login([登录])
    Logout([退出登录])
    Publish([发布问卷])
    List([查看问卷列表])
    Submit([提交投票])
    Result([查看投票结果])
    Delete([删除自己的问卷])

    User --> Register
    User --> Login
    User --> Logout
    User --> Publish
    User --> List
    User --> Submit
    User --> Result
    User --> Delete

    Publish -.需要登录.-> Login
    List -.需要登录.-> Login
    Submit -.需要登录.-> Login
    Delete -.需要登录.-> Login
```

说明：

- Mermaid 没有标准 UML 用例图语法，因此这里使用流程图语法模拟用例图。
- 系统不单独设计管理员角色。

## 6. 系统总体流程图

该图展示用户从进入系统到完成主要操作的总体流程。

```mermaid
flowchart TD
    Start([进入系统]) --> CheckLogin{是否已登录}
    CheckLogin -- 否 --> LoginPage[登录页面]
    CheckLogin -- 是 --> VoteList[问卷列表页面]

    LoginPage --> LoginAction{登录成功}
    LoginAction -- 否 --> LoginPage
    LoginAction -- 是 --> VoteList

    LoginPage --> RegisterPage[注册页面]
    RegisterPage --> RegisterAction{注册成功}
    RegisterAction -- 否 --> RegisterPage
    RegisterAction -- 是 --> LoginPage

    VoteList --> PublishPage[发布问卷页面]
    PublishPage --> PublishAction{发布成功}
    PublishAction -- 否 --> PublishPage
    PublishAction -- 是 --> VoteList

    VoteList --> SubmitPage[投票提交页面]
    SubmitPage --> AlreadyVoted{Session中是否已投票}
    AlreadyVoted -- 是 --> ResultPage[投票结果页面]
    AlreadyVoted -- 否 --> SubmitAction{提交成功}
    SubmitAction -- 否 --> SubmitPage
    SubmitAction -- 是 --> ResultPage

    VoteList --> ResultPage
    VoteList --> DeletePage[删除确认页面]
    DeletePage --> IsOwner{是否为发布者本人}
    IsOwner -- 否 --> VoteList
    IsOwner -- 是 --> DeleteAction{确认删除}
    DeleteAction -- 否 --> VoteList
    DeleteAction -- 是 --> VoteList
```

## 7. 注册登录流程图

```mermaid
flowchart TD
    A([开始]) --> B[输入用户名和密码]
    B --> C{用户名和密码是否为空}
    C -- 是 --> D[返回错误提示]
    D --> B
    C -- 否 --> E{执行登录或注册}

    E -- 登录 --> F{数据库中是否存在匹配用户}
    F -- 否 --> G[提示用户名或密码错误]
    G --> B
    F -- 是 --> H[保存User到Session]
    H --> I[写入lastUsername Cookie]
    I --> J[跳转问卷列表]

    E -- 注册 --> K{用户名是否已存在}
    K -- 是 --> L[提示用户名已存在]
    L --> B
    K -- 否 --> M[保存新用户]
    M --> N[跳转登录页面]
```

## 8. 发布问卷流程图

```mermaid
flowchart TD
    A([开始发布]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D[填写问卷标题和选项]
    D --> E{标题是否为空}
    E -- 是 --> F[提示标题不能为空]
    F --> D
    E -- 否 --> G{有效选项是否不少于2个}
    G -- 否 --> H[提示至少填写两个选项]
    H --> D
    G -- 是 --> I[保存vote_questions记录]
    I --> J[保存vote_options记录]
    J --> K[跳转问卷列表]
```

## 9. 投票与防重复投票流程图

```mermaid
flowchart TD
    A([进入投票页面]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D[读取questionId]
    D --> E{问卷是否存在}
    E -- 否 --> F[提示问卷不存在]
    E -- 是 --> G{Session中是否已投过该问卷}
    G -- 是 --> H[提示已投票并进入结果页]
    G -- 否 --> I[展示投票选项]
    I --> J[用户选择选项并提交]
    J --> K{是否选择选项}
    K -- 否 --> L[提示请选择选项]
    L --> I
    K -- 是 --> M{选项是否属于该问卷}
    M -- 否 --> N[提示非法选项]
    M -- 是 --> O[选项票数加1]
    O --> P[questionId写入Session已投票集合]
    P --> Q[跳转投票结果页面]
```

## 10. 删除问卷流程图

```mermaid
flowchart TD
    A([点击删除问卷]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D[读取questionId]
    D --> E{问卷是否存在}
    E -- 否 --> F[提示问卷不存在]
    E -- 是 --> G{当前用户是否为发布者}
    G -- 否 --> H[提示无删除权限]
    G -- 是 --> I[展示删除确认页面]
    I --> J{用户是否确认删除}
    J -- 否 --> K[返回问卷列表]
    J -- 是 --> L[删除vote_questions记录]
    L --> M[数据库级联删除vote_options记录]
    M --> N[返回问卷列表]
```

## 11. 登录时序图

```mermaid
sequenceDiagram
    actor User as 用户
    participant JSP as login.jsp
    participant Servlet as UserServlet
    participant Model as UserModel
    participant PwdUtil as PasswordUtil
    participant DB as PostgreSQL
    participant Session as Session
    participant Cookie as Cookie

    User->>JSP: 输入用户名和密码
    JSP->>Servlet: POST /user?action=login
    Servlet->>Model: findByUsernameAndPassword(username, password)
    Model->>DB: 按用户名查询用户
    DB-->>Model: 返回用户记录（含 bcrypt 哈希）
    Model->>PwdUtil: verify(明文密码, bcrypt哈希)
    PwdUtil-->>Model: 匹配成功
    Model-->>Servlet: 返回User对象
    Servlet->>Servlet: 清除User中的密码哈希
    Servlet->>Session: 保存currentUser
    Servlet->>Cookie: 写入lastUsername
    Servlet-->>User: 重定向到/vote/list
```

## 12. 投票提交时序图

```mermaid
sequenceDiagram
    actor User as 用户
    participant JSP as vote_submit.jsp
    participant Servlet as VoteSubmitServlet
    participant Session as Session
    participant Model as VoteModel
    participant DB as PostgreSQL

    User->>JSP: 选择选项并提交
    JSP->>Servlet: POST /vote/submit
    Servlet->>Session: 检查currentUser
    Servlet->>Session: 检查votedQuestionIds

    alt 已投过该问卷
        Servlet-->>User: 跳转结果页或显示已投票提示
    else 未投过该问卷
        Servlet->>Model: increaseOptionVoteCount(optionId)
        Model->>DB: update vote_options set vote_count = vote_count + 1
        DB-->>Model: 返回更新结果
        Model-->>Servlet: 返回成功
        Servlet->>Session: 保存questionId到votedQuestionIds
        Servlet-->>User: 重定向到/vote/result
    end
```

## 13. 发布问卷时序图

```mermaid
sequenceDiagram
    actor User as 用户
    participant JSP as vote_publish.jsp
    participant Servlet as VotePublishServlet
    participant Session as Session
    participant Model as VoteModel
    participant DB as PostgreSQL

    User->>JSP: 填写标题和多个选项
    JSP->>Servlet: POST /vote/publish
    Servlet->>Session: 获取currentUser
    Servlet->>Servlet: 校验标题和选项数量
    Servlet->>Model: createQuestion(userId, title, options)
    Model->>DB: insert vote_questions
    DB-->>Model: 返回questionId
    Model->>DB: insert vote_options
    DB-->>Model: 返回保存结果
    Model-->>Servlet: 返回成功
    Servlet-->>User: 重定向到/vote/list
```

## 14. 删除问卷时序图

```mermaid
sequenceDiagram
    actor User as 用户
    participant JSP as vote_delete.jsp
    participant Servlet as VoteDeleteServlet
    participant Session as Session
    participant Model as VoteModel
    participant DB as PostgreSQL

    User->>Servlet: GET /vote/delete?questionId=xxx
    Servlet->>Session: 获取currentUser
    Servlet->>Model: isQuestionOwner(questionId, userId)
    Model->>DB: 查询问卷发布者
    DB-->>Model: 返回判断结果
    Model-->>Servlet: 返回是否本人

    alt 是发布者本人
        Servlet-->>JSP: 转发到删除确认页面
        User->>JSP: 点击确认删除
        JSP->>Servlet: POST /vote/delete
        Servlet->>Model: deleteQuestion(questionId)
        Model->>DB: delete from vote_questions
        DB-->>Model: 级联删除选项并返回结果
        Model-->>Servlet: 返回成功
        Servlet-->>User: 重定向到/vote/list
    else 不是发布者本人
        Servlet-->>User: 返回无权限提示
    end
```

## 15. MVC 请求处理图

该图展示一次典型请求在 MVC 中的流转方式。

```mermaid
flowchart LR
    Browser[浏览器] --> Servlet[Controller Servlet]
    Servlet --> Model[Model 数据访问方法]
    Model --> DB[(PostgreSQL)]
    DB --> Model
    Model --> Servlet
    Servlet --> JSP[View JSP]
    JSP --> Browser
```

说明：

- 浏览器只访问 Servlet 或 JSP 页面。
- Servlet 负责判断登录状态、校验参数、调用 Model。
- Model 负责执行 SQL。
- JSP 只展示 Servlet 放入 request 中的数据。

## 16. 设计约束总结

- 系统只设计普通用户角色。
- 一个问卷只包含一个投票题目。
- 一个投票题目至少包含两个选项。
- 防重复投票使用 Session 实现。
- Cookie 只保存最近登录用户名，不保存密码。
- 删除问卷时只允许发布者本人操作。
- JSP 不直接访问数据库。
- 代码实现阶段不引入复杂框架，保持 Servlet + JSP + JDBC。
