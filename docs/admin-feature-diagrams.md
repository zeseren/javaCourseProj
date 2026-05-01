# 管理员功能设计图

本文档是针对校园问卷投票系统 **管理员功能** 的增量设计图，需与 `design-diagrams.md` 配合阅读。

## 1. ER 图（更新）

新增字段已在图中标出。

```mermaid
erDiagram
    USERS ||--o{ VOTE_QUESTIONS : publishes
    VOTE_QUESTIONS ||--|{ VOTE_OPTIONS : contains

    USERS {
        int id PK
        varchar username UK
        varchar password
        varchar role "新增: user/admin"
        timestamp created_at
    }

    VOTE_QUESTIONS {
        int id PK
        varchar title
        int user_id FK
        varchar status "新增: pending/approved/ended"
        timestamp created_at
    }

    VOTE_OPTIONS {
        int id PK
        int question_id FK
        varchar content
        int vote_count
    }
```

## 2. 问卷状态流转图

```mermaid
flowchart LR
    Pending[待审批 pending] -->|管理员审批| Approved[已通过 approved]
    Approved -->|管理员/发布者结束| Ended[已结束 ended]
    Pending -->|管理员/发布者删除| Deleted[已删除]
    Approved -->|管理员/发布者删除| Deleted
    Ended -->|管理员/发布者删除| Deleted
```

## 3. 用例图（更新）

```mermaid
flowchart LR
    User[普通用户]
    Admin[管理员]

    Register([注册])
    Login([登录])
    Logout([退出登录])
    Publish([发布问卷])
    List([查看问卷列表])
    Submit([提交投票])
    Result([查看投票结果])
    EndOwn([结束自己的问卷])
    DeleteOwn([删除自己的问卷])

    Approve([审批问卷])
    ManageList([查看所有问卷含待审批])
    EndAny([结束任意问卷])
    DeleteAny([删除任意问卷])

    User --> Register
    User --> Login
    User --> Logout
    User --> Publish
    User --> List
    User --> Submit
    User --> Result
    User --> EndOwn
    User --> DeleteOwn

    Admin --> Login
    Admin --> Logout
    Admin --> Publish
    Admin --> ManageList
    Admin --> Submit
    Admin --> Result
    Admin --> Approve
    Admin --> EndAny
    Admin --> DeleteAny
```

## 4. 类图（更新）

仅展示变更部分，新增字段用注释标记。

```mermaid
classDiagram
    class User {
        -int id
        -String username
        -String password
        -String role
        -Timestamp createdAt
    }

    class VoteQuestion {
        -int id
        -String title
        -int userId
        -String username
        -String status
        -Timestamp createdAt
    }

    class VoteOption {
        -int id
        -int questionId
        -String content
        -int voteCount
        -double percent
    }

    class UserModel {
        +findByUsername(String username) User
        +findByUsernameAndPassword(String username, String password) User
        +createUser(String username, String password) boolean
        +isAdmin(int userId) boolean
    }

    class VoteModel {
        +createQuestion(int userId, String title, List~String~ options) boolean
        +findQuestionsForUser() List~VoteQuestion~
        +findAllQuestionsForAdmin() List~VoteQuestion~
        +findQuestionById(int questionId) VoteQuestion
        +findOptionsByQuestionId(int questionId) List~VoteOption~
        +increaseOptionVoteCount(int optionId) boolean
        +isQuestionOwner(int questionId, int userId) boolean
        +isQuestionOwnerOrAdmin(int questionId, int userId) boolean
        +approveQuestion(int questionId) boolean
        +endQuestion(int questionId) boolean
        +deleteQuestion(int questionId) boolean
    }

    User "1" --> "0..*" VoteQuestion : publishes
    VoteQuestion "1" --> "2..*" VoteOption : contains
```

## 5. Servlet 类图（更新）

```mermaid
classDiagram
    class UserServlet {
        +doGet(request, response)
        +doPost(request, response)
        -login(request, response)
        -register(request, response)
        -logout(request, response)
    }

    class AdminServlet {
        +doGet(request, response)
        +doPost(request, response)
        -approve(request, response)
        -end(request, response)
    }

    class VotePublishServlet {
        +doGet(request, response)
        +doPost(request, response)
    }

    class VoteListServlet {
        +doGet(request, response)
    }

    class VoteSubmitServlet {
        +doGet(request, response)
        +doPost(request, response)
    }

    class VoteResultServlet {
        +doGet(request, response)
    }

    class VoteDeleteServlet {
        +doGet(request, response)
        +doPost(request, response)
    }

    class UserModel
    class VoteModel
    class JSP

    UserServlet ..> UserModel
    AdminServlet ..> VoteModel
    VotePublishServlet ..> VoteModel
    VoteListServlet ..> VoteModel
    VoteSubmitServlet ..> VoteModel
    VoteResultServlet ..> VoteModel
    VoteDeleteServlet ..> VoteModel

    UserServlet ..> JSP
    AdminServlet ..> JSP
    VotePublishServlet ..> JSP
    VoteListServlet ..> JSP
    VoteSubmitServlet ..> JSP
    VoteResultServlet ..> JSP
    VoteDeleteServlet ..> JSP
```

## 6. 发布问卷流程图（更新）

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
    G -- 是 --> I[保存vote_questions记录，status设为pending]
    I --> J[保存vote_options记录]
    J --> K[提示：等待管理员审批]
    K --> L[跳转问卷列表]
```

## 7. 审批流程图（新增）

```mermaid
flowchart TD
    A([管理员点击审批]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D{是否为管理员}
    D -- 否 --> E[提示无权限]
    D -- 是 --> F{问卷是否存在}
    F -- 否 --> G[提示问卷不存在]
    F -- 是 --> H{问卷状态是否为pending}
    H -- 否 --> I[提示状态不正确]
    H -- 是 --> J[将status改为approved]
    J --> K[跳转问卷列表]
```

## 8. 结束问卷流程图（新增）

```mermaid
flowchart TD
    A([点击结束问卷]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D{是否为管理员或发布者}
    D -- 否 --> E[提示无权限]
    D -- 是 --> F{问卷是否存在}
    F -- 否 --> G[提示问卷不存在]
    F -- 是 --> H{问卷状态是否为approved}
    H -- 否 --> I[提示状态不正确]
    H -- 是 --> J[将status改为ended]
    J --> K[跳转问卷列表]
```

## 9. 删除流程图（更新）

```mermaid
flowchart TD
    A([点击删除问卷]) --> B{是否登录}
    B -- 否 --> C[跳转登录页面]
    B -- 是 --> D[读取questionId]
    D --> E{问卷是否存在}
    E -- 否 --> F[提示问卷不存在]
    E -- 是 --> G{当前用户是否为管理员或发布者}
    G -- 否 --> H[提示无删除权限]
    G -- 是 --> I[展示删除确认页面]
    I --> J{用户是否确认删除}
    J -- 否 --> K[返回问卷列表]
    J -- 是 --> L[删除问卷及级联删除选项]
    L --> M[返回问卷列表]
```

## 10. 管理员审批时序图（新增）

```mermaid
sequenceDiagram
    actor Admin as 管理员
    participant JSP as vote_list.jsp
    participant Servlet as AdminServlet
    participant Session as Session
    participant Model as VoteModel
    participant DB as PostgreSQL

    Admin->>JSP: 点击"审批通过"按钮
    JSP->>Servlet: POST /admin?action=approve&questionId=xxx
    Servlet->>Session: 检查currentUser
    Servlet->>Model: isAdmin(userId)
    Model->>DB: 查询用户role
    DB-->>Model: 返回role信息
    Model-->>Servlet: 返回true（是管理员）
    Servlet->>Model: approveQuestion(questionId)
    Model->>DB: update vote_questions set status='approved'
    DB-->>Model: 返回更新结果
    Model-->>Servlet: 返回成功
    Servlet-->>Admin: 重定向到/vote/list
```

## 11. 结束问卷时序图（新增）

```mermaid
sequenceDiagram
    actor User as 管理员/发布者
    participant JSP as vote_list.jsp
    participant Servlet as AdminServlet
    participant Session as Session
    participant Model as VoteModel
    participant DB as PostgreSQL

    User->>JSP: 点击"结束"按钮
    JSP->>Servlet: POST /admin?action=end&questionId=xxx
    Servlet->>Session: 检查currentUser
    Servlet->>Model: isQuestionOwnerOrAdmin(questionId, userId)
    Model->>DB: 查询问卷发布者和用户角色
    DB-->>Model: 返回判断结果
    Model-->>Servlet: 返回true（有权限）
    Servlet->>Model: endQuestion(questionId)
    Model->>DB: update vote_questions set status='ended'
    DB-->>Model: 返回更新结果
    Model-->>Servlet: 返回成功
    Servlet-->>User: 重定向到/vote/list
```

## 12. 设计变更总结

| 变更项 | 变更类型 | 说明 |
|---|---|---|
| `users` 表新增 `role` 字段 | 数据库 | 区分 user/admin |
| `vote_questions` 表新增 `status` 字段 | 数据库 | 标记 pending/approved/ended |
| `User` 实体新增 `role` 字段 | 实体类 | 对应数据库 |
| `VoteQuestion` 实体新增 `status` 字段 | 实体类 | 对应数据库 |
| `UserModel.isAdmin()` | 新增方法 | 判断管理员身份 |
| `VoteModel` 查询方法变更 | 变更方法 | 区分用户/管理员视角 |
| `VoteModel.approveQuestion()` | 新增方法 | 审批通过 |
| `VoteModel.endQuestion()` | 新增方法 | 结束问卷 |
| `VoteModel.isQuestionOwnerOrAdmin()` | 新增方法 | 权限判断 |
| `AdminServlet` | 新增 Servlet | `/admin`，处理审批和结束 |
| `VotePublishServlet` | 变更 | 发布时设置 pending 状态 |
| `VoteListServlet` | 变更 | 区分用户/管理员查询 |
| `VoteSubmitServlet` | 变更 | 增加状态校验 |
| `VoteDeleteServlet` | 变更 | 权限放宽至管理员 |
| `vote_list.jsp` | 变更 | 区分用户/管理员 UI |
| `vote_submit.jsp` | 变更 | 已结束状态显示 |
| `vote_publish.jsp` | 变更 | 提示文案变更 |
