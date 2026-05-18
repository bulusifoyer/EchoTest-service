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

### 6. 测试执行与报告模块 ✅

- **DDL**: `t_execution_report` + 补丁（project_id / create_by / failed_steps / total_duration_ms / is_deleted）；`t_execution_detail` + 补丁（step_order / request_method / request_url / status_code / fail_reason；status 枚举改 PASSED/FAILED；elapsed_ms 修正为 BIGINT）
- **功能**: 同步串行执行用例（遇错即停） + ${var} 单层变量替换（仅 URL/headers/body） + JsonPath 提取 + 三种 operator 断言 + 三层 headers 合并复用 M1
- **接口**: `POST /api/executions/run`（同步返回 reportId）、`GET /api/executions/{id}`（含明细树）、`GET /api/executions/list/{projectId}`、`DELETE /api/executions/{id}`（软删）
- **数据隔离**: 用例 / 环境 / 步骤接口的归属严格按 create_by 校验；env 必须与 case 同项目；列表查询 LEFT JOIN caseName + envName
- **重构**: 把 M1 的 `mergeJsonHeaders` 等工具方法抽到 `service/execution/HeaderMerger`，M3 与 M1 试调共用
- **MVP 边界**: 同步执行（无任务队列），不支持 SKIP，不支持 DATABASE 断言
- **日期**: 2026-05-18

---

## 每日进展记录

### 2026-05-18

#### 后端 (EchoTest-service) — M3 执行引擎与报告落库
- **DDL 补丁**：schema.sql 末尾追加 M3 ALTER 段
  - `t_execution_report`：补 `project_id` / `create_by` / `failed_steps` / `total_duration_ms` / `is_deleted`；`status` 注释统一为 `RUNNING / PASSED / FAILED`；增加 `idx_project_create_by` 索引
  - `t_execution_detail`：补 `step_order` / `request_method` / `request_url` / `status_code` / `fail_reason`；`status` 改为 `PASSED / FAILED`；修正历史脏类型 `elapsed_ms` LONG → BIGINT
  - 已应用到本地 `echotest-mysql` 容器
- **执行引擎核心**（`service/execution/` 包，6 个新文件）
  - `VariableContext`：`${var}` 单层替换，未命中保持原文，不递归
  - `Asserter`：STATUS_CODE / JSON_PATH × EQUALS / CONTAINS / GREATER_THAN
  - `Extractor`：JsonPath 提取，失败仅记日志不阻断（写入 detail.assertResult._extracts）
  - `HttpExecutor`：OkHttp 真实发起，网络异常统一 ExecuteResult 兜底（不抛出）
  - `HeaderMerger`：从 `ApiTryRunServiceImpl` 抽出的公共工具，M1 试调与 M3 引擎共用
  - `ExecutionEngine`：同步串行 + 遇错即停 + 三层 headers 合并 + Content-Type 自动补 + 三处变量替换 + 写报告/明细
- **业务 + Controller**：`ExecutionService` / `ExecutionServiceImpl` / `ExecutionController`，4 个 RESTful 接口
- **新增实体 / Mapper / DTO**：`ExecutionReport` / `ExecutionDetail` 实体；`ExecutionReportMapper`（含 LEFT JOIN VO 列表）+ `ExecutionDetailMapper`；`ExecutionRunDTO` / `ExecutionReportVO` / `ExecutionDetailVO` / `ExecutionReportDetailVO`
- **测试**：`AsserterTest`（8 个）+ `VariableContextTest`（6 个）+ `ExecutionEngineTest`（2 个：PASSED 链路 + 遇错即停 FAILED 链路）；`ApiTryRunServiceTest` 在 HeaderMerger 重构后仍全过
- **验收**：`mvn test` 全部 25 测试通过；编译干净
- **契约同步**：父目录 `docs/api-contract.md` 新增 §6 测试执行与报告（替换原占位段），错误码 §7、联调 §8

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

