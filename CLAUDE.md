# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 工作流规则（最高优先级）

### 1. 瀑布流开发

严格遵循瀑布流模型，每个阶段必须：

- 完成当前阶段所有产出后，**向用户汇报并等待明确许可**，方可进入下一阶段。
- 汇报时说明：本阶段完成了什么、下一步计划是什么。
- 所有设计文档、说明文档必须在编码前完成并通过用户确认。

### 2. Less is More

- 所有开发必须**基于已有的代码结构**，保持最小、最简。
- 只有当某事物**绝对缺失将导致项目无法继续**时，才可引入新依赖、新框架或新抽象，且**必须先向用户确认**。
- 不添加"以后可能会用到"的东西。

### 3. 文档第一

- 代码必须配有**大量通俗易懂的注释和文档**，面向**无编程基础的人群**。
- 解释"为什么这样做"而非"代码在做什么"。
- **文档比代码更重要**——宁可多写注释，不可让人猜测意图。

---

## Build & Test

```bash
# Build the project (compiles + packages as WAR)
./mvnw package

# Compile only
./mvnw compile

# Run all tests
./mvnw test

# Run a single test class
./mvnw -Dtest=MyTestClass test

# Run a single test method
./mvnw -Dtest=MyTestClass#myMethod test

# Clean build artifacts
./mvnw clean
```

## Project Overview

- **Group**: `org.test`, **Artifact**: `javaCourseProj`
- **Packaging**: WAR — this is a Jakarta Servlet-based web application.
- **Java target**: 8 (source/target), though the IntelliJ project is configured for JDK 22.
- **Test framework**: JUnit Jupiter 5.13.x.

## Dependencies

| Dependency | Version | Scope | Notes |
|---|---|---|---|
| `jakarta.servlet-api` | 6.1.0 | provided | Servlet container provides this at runtime |
| `junit-jupiter-api` / `junit-jupiter-engine` | 5.13.2 | test | JUnit 5 |

## Architecture

Standard Maven WAR layout:

```
src/main/java/        → Java sources
src/main/resources/   → classpath resources (bundled in WAR)
src/test/java/        → test sources
src/test/resources/   → test classpath resources
```

Servlet API is Jakarta namespace (`jakarta.servlet.*`), not the older `javax.servlet.*` — this means the app targets Tomcat 10+, Jetty 11+, or equivalent Jakarta-compatible containers.
