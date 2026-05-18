# EchoTest-service（后端）

基于 Spring Boot 的分层自动化接口测试平台 —— 后端服务。

提供：用户认证、项目管理、环境配置、接口管理、单接口在线试调、测试用例聚合管理（用例 + 步骤 + 变量提取 + 断言）、用例执行引擎与报告（M3：变量替换 + 串行执行 + 断言判定 + 报告落库）。

> 接口契约见 `../docs/api-contract.md`；模块进度见 `PROGRESS.md`。

---

## 1. 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **8** | Temurin 8u412 实测 |
| Spring Boot | 2.7.18 | Web / Validation / AOP / Test |
| MyBatis-Plus | 3.5.3.1 | + Druid 数据源 |
| MySQL | 8.0.x | 数据库引擎 InnoDB / utf8mb4 |
| Maven | 3.9.x | 构建 |
| OkHttp | 4.12.0 | M1 试调 + M3 引擎真实 HTTP 调用 |
| JsonPath | 2.9.0 | M2 变量提取 / M3 断言 |
| JWT | jjwt 0.9.1 | 鉴权 |
| Swagger | springfox 2.9.2 | 文档（`/doc.html` / `/swagger-ui.html`） |

监听端口：**8080**。
数据库名：**`test_platform`**，账号密码默认 `root / root`（见 `application.yml`）。

---

## 2. 环境准备

### 2.1 必需

- macOS / Linux（Windows 同理，命令略有差异）
- Docker Desktop（用于本地 MySQL）
- JDK 8 + Maven 3.9+

### 2.2 推荐：本仓库内置工具链脚本

仓库根目录提供 `.echotest-env.sh`，一键把 `~/.echotest-toolchain` 下的 JDK 8 / Maven / Node.js 注入当前 shell（仅当前会话生效，不污染全局）：

```bash
cd EchoTest-service
source ./.echotest-env.sh
# 输出：
#   JAVA_HOME=...jdk8u412...
#   MAVEN_HOME=...apache-maven-3.9.15
#   NODE_HOME=...node-v20.18.1...
java -version   # openjdk 8u412
mvn -v          # 3.9.x
```

如果你没用 toolchain 包，自己装好 JDK 8 + Maven 跳过该脚本即可。

---

## 3. 数据库准备

### 3.1 启动本地 MySQL（Docker）

仓库默认假设容器名为 `echotest-mysql`，对外端口 3306。第一次启动：

```bash
docker run -d \
  --name echotest-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=test_platform \
  -e TZ=Asia/Shanghai \
  --restart unless-stopped \
  mysql:8.0
```

之后只需 `docker start echotest-mysql`。

### 3.2 应用 schema

`schema.sql` 是「初始 CREATE TABLE + 各模块 ALTER 补丁」累积而成，按顺序执行一次即可。

```bash
docker exec -i echotest-mysql mysql -uroot -proot test_platform < schema.sql
```

> 已经初始化过、只想跑某次新增的 ALTER：用 `awk` 截尾，例如只跑 M3：
> ```bash
> awk '/M3 测试执行与报告模块补丁/,0' schema.sql \
>   | docker exec -i echotest-mysql mysql -uroot -proot test_platform
> ```

### 3.3 验证

```bash
docker exec echotest-mysql mysql -uroot -proot -Dtest_platform \
  -e "SHOW TABLES;"
# 应看到 t_user / t_project / t_environment / t_api_definition
#         / t_test_case / t_test_step / t_variable_extract / t_assertion
#         / t_execution_report / t_execution_detail
```

---

## 4. 启动后端

### 4.1 开发模式（前台）

```bash
cd EchoTest-service
source ./.echotest-env.sh        # 可选：注入工具链
mvn spring-boot:run              # 监听 8080
```

启动成功的标志：控制台出现
```
=   访问地址：http://localhost:8080      =
=   API文档：http://localhost:8080/doc.html =
```

### 4.2 健康自检

```bash
curl -s -o /dev/null -w 'HTTP %{http_code}\n' \
  'http://localhost:8080/api/auth/check-username?username=ping'
# 期望：HTTP 200
```

### 4.3 打包 JAR 后启动（演示用）

```bash
mvn clean package -DskipTests
java -jar target/test-platform-1.0.0.jar
```

### 4.4 切换 profile（可选）

`application.yml` 内置 `dev / prod / test` 三段。`test` 段使用 H2 内存库，仅供测试。运行时切换：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 5. 测试

```bash
# 单元测试 + Mockito 集成测试，**不依赖数据库**
mvn test

# 编译检查
mvn clean compile
```

当前测试覆盖：
- `UserServiceTest`、`ApiDefinitionServiceTest`、`ApiTryRunServiceTest`
- `TestCaseServiceAddTest`、`TestCaseServiceUpdateTest`
- `AsserterTest`、`VariableContextTest`、`ExecutionEngineTest`

---

## 6. 端到端冒烟（curl，可选）

后端启动后，以下脚本可一键串完「注册 → 登录 → 建项目/环境/接口 → 建用例 → 执行 → 看报告」全链路（依赖 `python3` 解析 JSON）：

```bash
B="http://localhost:8080"; HDR='Content-Type: application/json'
U="smoke$(date +%s)"; P="abc123456"

curl -s -X POST $B/api/auth/register -H "$HDR" \
  -d "{\"username\":\"$U\",\"password\":\"$P\",\"confirmPassword\":\"$P\",\"nickname\":\"smoke\"}" >/dev/null

TOKEN=$(curl -s -X POST $B/api/auth/login -H "$HDR" \
  -d "{\"username\":\"$U\",\"password\":\"$P\"}" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')
AUTH="Authorization: Bearer $TOKEN"

PID=$(curl -s -X POST $B/api/projects/add -H "$HDR" -H "$AUTH" \
  -d '{"name":"smoke","description":"e2e"}' \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')

EID=$(curl -s -X POST $B/api/environments/add -H "$HDR" -H "$AUTH" \
  -d "{\"projectId\":$PID,\"envName\":\"httpbin\",\"baseUrl\":\"https://httpbin.org\"}" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')

A1=$(curl -s -X POST $B/api/apis/add -H "$HDR" -H "$AUTH" \
  -d "{\"projectId\":$PID,\"name\":\"uuid\",\"method\":\"GET\",\"path\":\"/uuid\"}" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')
A2=$(curl -s -X POST $B/api/apis/add -H "$HDR" -H "$AUTH" \
  -d "{\"projectId\":$PID,\"name\":\"echo\",\"method\":\"GET\",\"path\":\"/get?token=\${token}\"}" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')

CID=$(curl -s -X POST $B/api/cases/add -H "$HDR" -H "$AUTH" -d '{
  "projectId":'$PID',"caseName":"M3 demo","steps":[
    {"apiId":'$A1',"stepName":"取uuid","extracts":[{"variableName":"token","jsonPath":"$.uuid"}],
     "assertions":[{"assertType":"STATUS_CODE","expectedValue":"200","operator":"EQUALS"}]},
    {"apiId":'$A2',"stepName":"带token调用","extracts":[],
     "assertions":[{"assertType":"JSON_PATH","expression":"$.args.token","operator":"CONTAINS","expectedValue":"-"}]}
  ]
}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')

RID=$(curl -s -X POST $B/api/executions/run -H "$HDR" -H "$AUTH" \
  -d "{\"caseId\":$CID,\"envId\":$EID,\"timeoutMs\":15000}" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"])')

curl -s "$B/api/executions/$RID" -H "$AUTH" \
  | python3 -c 'import json,sys;d=json.load(sys.stdin);r=d["data"]["report"];
print("status=%s passed=%d failed=%d totalSteps=%d totalMs=%d"%(r["status"],r["passedSteps"],r["failedSteps"],r["totalSteps"],r["totalDurationMs"]))'
```

期望输出：`status=PASSED passed=2 failed=0 totalSteps=2 totalMs=...`

---

## 7. 目录结构

```
EchoTest-service/
├── schema.sql                  # 全量 DDL + 各模块 ALTER 补丁（按时间顺序累积）
├── PROGRESS.md                 # 模块进度 / 每日记录
├── pom.xml
├── .echotest-env.sh            # 工具链注入脚本（可选）
├── src/main/java/com/testplatform/
│   ├── controller/             # REST 控制器
│   ├── service/                # 业务接口
│   │   ├── impl/               # 业务实现
│   │   └── execution/          # M3 执行引擎核心（VariableContext / Asserter / Extractor / HttpExecutor / HeaderMerger / ExecutionEngine）
│   ├── mapper/                 # MyBatis-Plus Mapper
│   ├── entity/                 # 数据库实体
│   │   └── dto/                # DTO / VO
│   ├── common/                 # Result / UserContext 等
│   ├── config/                 # WebMvc / 拦截器 / Swagger 等
│   └── TestPlatformApplication.java
└── src/main/resources/
    └── application.yml         # 多 profile 配置（dev / prod / test）
```

---

## 8. 常见问题

**Q：8080 被占用？**
```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN -t | xargs -r kill -9
```

**Q：MySQL 启动了但 backend 报 `Communications link failure`？**
- 端口冲突：`docker ps` 检查 3306 是否被旧容器占用
- 时区：`application.yml` 已写死 `serverTimezone=Asia/Shanghai`
- 密码：默认 `root/root`，与 `docker run` 时的 `MYSQL_ROOT_PASSWORD` 必须一致

**Q：DDL 执行报 `Duplicate column name`？**
表示该 ALTER 已应用过。`schema.sql` 设计为「累积式补丁」，重复执行只会从第一次失败的 ALTER 中断；已建好的表无需关心。

**Q：JDK 版本？**
必须 **JDK 8**。pom.xml 明确写死 `<java.version>8</java.version>`，`@TableLogic` / `@TableField` 等注解依赖 MyBatis-Plus 3.5.x 与 Spring Boot 2.7.x，与 JDK 17+ 不完全兼容。

**Q：日志在哪？**
默认 `logs/test-platform.log`，开发态推荐看控制台。`logs/` 已在 `.gitignore` 中。

---

## 9. 提交规范（仅本仓库）

- Commit message 格式：`feat(M{编号}): xxx`、`fix(P{编号}): xxx`
- **不提交**：`target/`、`logs/`（已在 `.gitignore`）
- 每个模块完成需更新 `PROGRESS.md` 与 `../docs/api-contract.md`，在 commit message 中引用 docs 改动
