# 用户认证模块 API 接口文档

## 统一响应格式

所有接口均返回 `Result<T>` 格式的 JSON 数据：

```json
{
  "code": 200,        // 状态码：200成功，400参数错误，401未授权，500服务器错误
  "message": "操作成功", // 提示信息
  "data": T           // 响应数据，类型根据接口不同而变化
}
```

## 接口列表

### 1. 用户注册

**请求方式**：POST  
**接口地址**：`/api/auth/register`  
**Content-Type**：`application/json`

#### 请求参数（Body）

| 字段名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|----------|
| username | String | 是 | 登录账号 | 3-20位，只能包含字母、数字和下划线 |
| password | String | 是 | 登录密码 | 至少6位，不能包含空格 |
| confirmPassword | String | 是 | 确认密码 | 必须与password字段一致 |
| nickname | String | 否 | 用户昵称 | 长度不超过30位 |
| email | String | 否 | 邮箱地址 | 符合邮箱格式，长度不超过100位 |
| captcha | String | 否 | 验证码 | 用于防止恶意注册 |

**请求示例**：
```json
{
  "username": "testuser",
  "password": "123456",
  "confirmPassword": "123456",
  "nickname": "测试用户",
  "email": "test@example.com"
}
```

#### 响应数据（data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 注册成功后的用户ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": 123
}
```

### 2. 用户登录

**请求方式**：POST  
**接口地址**：`/api/auth/login`  
**Content-Type**：`application/json`

#### 请求参数（Body）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 登录账号，3-20位 |
| password | String | 是 | 登录密码，至少6位 |
| rememberMe | Boolean | 否 | 记住我，默认false |
| captcha | String | 否 | 验证码，用于防止暴力破解 |
| captchaId | String | 否 | 验证码唯一标识 |

**请求示例**：
```json
{
  "username": "testuser",
  "password": "123456",
  "rememberMe": true
}
```

#### 响应数据（data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| token | String | JWT认证令牌 |

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEyMywic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE2MTYxNzE2MDAsImV4cCI6MTYxNjI1ODAwMH0.xxxxxx"
}
```

### 3. 检查用户名是否可用

**请求方式**：GET  
**接口地址**：`/api/auth/check-username`  
**Content-Type**：`application/x-www-form-urlencoded`

#### 请求参数（Query）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 待检查的用户名 |

**请求示例**：`/api/auth/check-username?username=testuser`

#### 响应数据（data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| available | Boolean | 用户名是否可用（true表示可用） |

**响应示例**：
```json
{
  "code": 200,
  "message": "检查完成",
  "data": true
}
```

### 4. 检查邮箱是否可用

**请求方式**：GET  
**接口地址**：`/api/auth/check-email`  
**Content-Type**：`application/x-www-form-urlencoded`

#### 请求参数（Query）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | String | 是 | 待检查的邮箱地址 |

**请求示例**：`/api/auth/check-email?email=test@example.com`

#### 响应数据（data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| available | Boolean | 邮箱是否可用（true表示可用） |

**响应示例**：
```json
{
  "code": 200,
  "message": "检查完成",
  "data": true
}
```

### 5. 刷新JWT令牌

**请求方式**：POST  
**接口地址**：`/api/auth/refresh-token`  
**Content-Type**：`application/json`

#### 请求头（Header）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer令牌，格式：`Bearer <oldToken>` |

**请求示例**：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxxx
```

#### 响应数据（data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| newToken | String | 新的JWT令牌 |

**响应示例**：
```json
{
  "code": 200,
  "message": "令牌刷新成功",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEyMywic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE2MTYyNzE2MDAsImV4cCI6MTYxNjM1ODAwMH0.yyyyyy"
}
```

### 6. 用户退出登录

**请求方式**：POST  
**接口地址**：`/api/auth/logout`  
**Content-Type**：`application/json`

#### 请求头（Header）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer令牌，格式：`Bearer <token>` |

#### 响应数据（data）

此接口无数据返回，data字段为null。

**响应示例**：
```json
{
  "code": 200,
  "message": "退出登录成功",
  "data": null
}
```

## 注意事项

1. **JWT令牌使用**：登录成功后获得的JWT令牌需要在后续请求的`Authorization`头部中携带，格式为：`Bearer <token>`
2. **密码安全**：密码在传输前应在前端进行加密（如使用HTTPS），后端使用BCrypt算法加密存储
3. **验证码**：当前系统配置中验证码功能默认关闭（`app.config.enable-captcha: false`），如需启用请修改配置
4. **跨域支持**：接口已配置CORS，支持前后端分离部署
5. **错误处理**：所有接口都进行参数校验，错误信息会在`message`字段中返回

## 接口依赖

1. **数据库**：需要先创建`t_user`表（DDL语句见schema.sql）
2. **服务端口**：默认运行在`8080`端口，可通过`application.yml`修改
3. **JWT配置**：默认密钥和过期时间可在配置文件中修改