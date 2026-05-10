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

### 4. 接口管理模块 ✅

- **DDL**: `t_api_definition` + 补丁（create_by / is_deleted）
- **功能**: 接口 CRUD（双层数据隔离、method+path 同项目唯一）+ 单接口试调（OkHttp 真实发起、三层 headers 合并、body 非空自动补 Content-Type、网络异常兜底）
- **依赖**: OkHttp 4.12.0 + JsonPath 2.9.0
- **日期**: 2026-05-10

### 5. 测试用例管理模块 ✅

- **DDL**: `t_test_case` + 补丁（create_by / is_deleted）；`t_test_step` / `t_variable_extract` / `t_assertion` 沿用现有
- **功能**: 用例 + 步骤 + 变量提取 + 断言 一次性聚合管理（add / getDetail / list / update / delete）；更新走全量替换；用例软删，子表硬删
- **MVP 边界**: 断言类型仅支持 STATUS_CODE / JSON_PATH（DATABASE 类型为规划项）
- **日期**: 2026-05-10

### 6. 测试执行与报告模块 ⏳

- **DDL**: `t_execution_report` 执行报告表, `t_execution_detail` 执行明细表
- **说明**: 执行测试用例，生成测试报告，记录请求/响应详情

---

## 每日进展记录

### 2026-05-10

#### 后端 (EchoTest-service)
- **commit `9ae03d7`** `feat(M1): 接口管理模块 - 接口定义 CRUD 与在线试调`
  - 新增 13 个 Java 文件（ApiDefinition 实体 + Add/Update/TryRun DTO + ResultVO + Mapper + Service 接口/实现 + Controller + 2 个测试）
  - schema.sql 末尾追加 ALTER：`t_api_definition` 补 `create_by` + `is_deleted`
  - pom.xml 引入 OkHttp 4.12.0 + JsonPath 2.9.0
  - application.yml 修复 datasource 缩进（之前因破损导致后端无法启动）
  - 新增 `.gitignore` + `.echotest-env.sh`
  - PROGRESS.md：模块 4 ⏳ → ✅
  - 验证：`mvn clean compile` 通过、2 个 Mockito 测试通过、curl 真实 httpbin 调用闭环
  - 已推送：`origin/master`

- **commit 待办（M2）** `feat(M2): 测试用例聚合管理（用例+步骤+变量提取+断言）`
  - 新增 15 个 Java 文件（4 实体 + 5 DTO/VO + 4 Mapper + Service 接口/实现 + Controller + 2 测试）
  - schema.sql 末尾追加 ALTER：`t_test_case` 补 `create_by` + `is_deleted`
  - PROGRESS.md：模块 5 ⏳ → ✅
  - 验证：mvn 编译 + 2 测试全过、curl add/detail/update/list/delete 全链路、DB 直查证据齐全
  - 状态：仅本地工作树，未 git add / 未 commit（明日验收后提交）

#### 前端 (EchoTest-web)
- **commit `dc90e75`** `fix(P1): 修复环境配置抽屉新增环境按钮消失问题`
  - `src/views/project/index.vue`：误用 `el-drawer` 的 `#extra` 插槽（实际不存在）→ 改为放入抽屉默认插槽顶部 `.env-toolbar` 容器（右对齐）
  - 新增 `.gitignore`（忽略 `node_modules/` `dist/` `logs/` 等）
  - 新增 `.echotest-env.sh`（指向后端工具链脚本的软链）
  - 已推送：`origin/master`

- 本次会话后期探索过 P2 接口管理 + 试调页面（4 子组件方案），因效果不达标已撤销，留作明日重做

#### 工具链与运维
- 安装本地工具链到 `~/.echotest-toolchain/`：JDK 8 (Temurin 8u412) + Maven 3.9.15 + Node.js 20.18.1
- 修复 `application.yml` 数据库密码与 Docker 容器对齐（root/root）
- DB 补丁实际应用到本地容器 `echotest-mysql`：`t_api_definition` + `t_test_case` 各 2 个字段

#### 明日计划
- [ ] M2 端到端验收通过后 git commit + push
- [ ] 重做 P2 前端（接口管理 + 试调，按修订版 4 子组件方案）
- [ ] 或继续推进 M3 执行引擎（论文核心：变量替换 + 顺序执行 + 断言判定 + 写报告/明细）

