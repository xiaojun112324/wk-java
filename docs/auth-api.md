# 认证接口文档（前端联调）

更新时间：2026-03-05

## 1. 通用说明

- Base URL: `http://{host}:8080`
- Content-Type: `application/json`
- 统一响应格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

失败示例：

```json
{
  "code": 500,
  "msg": "username already exists",
  "data": null
}
```

---

## 2. 普通用户认证（`user` 表）

### 2.1 用户注册

- 路径：`POST /api/auth/register`
- 说明：使用账号/邮箱/密码注册，可选填写上级邀请码（用户邀请码）

请求参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 登录账号（唯一） |
| email | string | 是 | 邮箱（唯一） |
| password | string | 是 | 密码，至少 6 位 |
| inviteCode | string | 否 | 上级用户的邀请码 |

请求示例：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456",
  "inviteCode": "AB12CD34"
}
```

成功响应示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "6bdf0ad69e2d4cc5a7e8d9f62f5f294b",
    "user": {
      "id": 1,
      "username": "alice",
      "email": "alice@example.com",
      "inviteCode": "Q7M2K8P1",
      "inviterId": 10,
      "status": 1
    }
  }
}
```

常见错误：

- `username is required`
- `email is required`
- `password is required`
- `password must be at least 6 characters`
- `username already exists`
- `email already exists`
- `invite code not found`

### 2.2 用户登录

- 路径：`POST /api/auth/login`
- 说明：支持“账号或邮箱 + 密码”

请求参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 可传 `username` 或 `email` |
| password | string | 是 | 登录密码 |

请求示例：

```json
{
  "account": "alice@example.com",
  "password": "123456"
}
```

常见错误：

- `account is required`
- `password is required`
- `user not found`
- `password is incorrect`
- `account is disabled`

---

## 3. 管理后台认证（`sys_user` 表）

### 3.1 管理员注册

- 路径：`POST /api/admin/auth/register`
- 说明：管理员注册必须提供系统邀请码
- 邀请码来源：`sys_config` 表中 `config_key = admin_register_invite_code` 的 `config_value`

请求参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 管理员账号（唯一） |
| email | string | 是 | 管理员邮箱（唯一） |
| password | string | 是 | 密码，至少 6 位 |
| registerInviteCode | string | 是 | 管理员注册邀请码（来自系统配置） |

请求示例：

```json
{
  "username": "admin01",
  "email": "admin01@example.com",
  "password": "123456",
  "registerInviteCode": "ADMIN2026"
}
```

成功响应示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "95df6d1c0ca14d13b6d14552a2a61df1",
    "user": {
      "id": 1,
      "username": "admin01",
      "email": "admin01@example.com",
      "status": 1
    }
  }
}
```

常见错误：

- `registerInviteCode is required`
- `admin register invite code is not configured`
- `register invite code is incorrect`
- `username already exists`
- `email already exists`

### 3.2 管理员登录

- 路径：`POST /api/admin/auth/login`
- 说明：支持“账号或邮箱 + 密码”

请求参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 可传 `username` 或 `email` |
| password | string | 是 | 登录密码 |

请求示例：

```json
{
  "account": "admin01",
  "password": "123456"
}
```

常见错误：

- `account is required`
- `password is required`
- `admin user not found`
- `password is incorrect`
- `admin account is disabled`

---

## 4. 系统配置说明（`sys_config`）

已增加默认配置：

```sql
config_key = 'admin_register_invite_code'
config_value = 'ADMIN2026'
```

生产环境建议在初始化后立刻改成你自己的邀请码。
