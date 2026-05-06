# 项目开发进度跟踪

---

## 已完成的模块

### 1. 用户认证模块 ✅

- **DDL**: `t_user` 用户表
- **功能**: 用户注册、登录、JWT令牌签发与验证、退出登录、令牌刷新
- **文件**: `User.java`, `UserRegisterDTO.java`, `UserLoginDTO.java`, `UserMapper.java`, `UserService.java`, `UserServiceImpl.java`, `AuthController.java`, `JwtUtils.java`, `PasswordUtils.java`
- **日期**: 2026-04-21

### 2. 基础设施补全 ✅

- **功能**: ThreadLocal 用户上下文、JWT 认证拦截器、WebMvc 配置注册
- **文件**: `UserContext.java`, `JwtAuthenticationInterceptor.java`, `WebMvcConfig.java`, `MyMetaObjectHandler.java`
- **说明**: 统一拦截 `/api/**` 路径（排除认证公开接口），自动解析 token 并注入用户上下文
- **日期**: 2026-04-22

### 3. 项目与环境管理模块（MVP版本） ✅

- **DDL**: `t_project` 项目表, `t_environment` 环境配置表（MVP版）
- **功能**: 项目增删改查（数据隔离）、环境配置增删改查（归属权校验、JSON格式验证）
- **文件**: `Project.java`, `Environment.java`, `ProjectAddDTO.java`, `ProjectUpdateDTO.java`, `EnvironmentAddDTO.java`, `EnvironmentUpdateDTO.java`, `ProjectMapper.java`, `EnvironmentMapper.java`, `ProjectService.java`, `ProjectServiceImpl.java`, `EnvironmentService.java`, `EnvironmentServiceImpl.java`, `ProjectController.java`, `EnvironmentController.java`, `JsonUtils.java`
- **说明**: MVP版本支持基础路径（base_url）和全局请求头（global_headers，JSON格式存储）
- **日期**: 2026-04-23

---

## 计划中的模块

### 4. 接口管理模块 ⏳

- **DDL**: `t_api` 接口定义表
- **说明**: 管理每个项目下的接口定义，包括请求方法、路径、请求头、请求参数（Query/Body/Path）的配置
- **计划**: 2026-04-24

### 5. 测试用例管理模块 ⏳

- **DDL**: `t_test_case` 测试用例表
- **说明**: 对已定义的接口编写测试用例，配置断言规则、变量提取等

### 6. 测试执行与报告模块 ⏳

- **DDL**: `t_test_execution` 执行记录表, `t_execution_result` 执行结果表
- **说明**: 执行测试用例，生成测试报告，记录请求/响应详情
