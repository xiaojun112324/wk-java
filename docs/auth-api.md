# 认证接口文档（JWT）

更新时间：2026-03-06

## 1. 通用说明

- Base URL: `https://api.kuaiyi.info`
- Content-Type: `application/json`

统一响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

## 2. JWT 返回说明

注册/登录成功后返回：

- `token`：JWT 字符串
- `tokenType`：固定 `Bearer`
- `expiresIn`：过期秒数（默认 604800）

请求鉴权头：

```http
Authorization: Bearer <token>
```

## 3. 用户认证

### 3.1 用户注册

- Method: `POST`
- Path: `/api/auth/register`

请求体：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456",
  "inviteCode": "AB12CD34"
}
```

说明：

- `inviteCode` 可选，用于建立邀请关系。

### 3.2 用户登录

- Method: `POST`
- Path: `/api/auth/login`

请求体：

```json
{
  "account": "alice@example.com",
  "password": "123456"
}
```

### 3.3 获取当前用户信息

- Method: `GET`
- Path: `/api/auth/me`
- Header:

```http
Authorization: Bearer <token>
```

## 4. 管理员认证

### 4.1 管理员注册

- Method: `POST`
- Path: `/api/admin/auth/register`

请求体：

```json
{
  "username": "admin01",
  "email": "admin01@example.com",
  "password": "123456",
  "registerInviteCode": "ADMIN2026"
}
```

### 4.2 管理员登录

- Method: `POST`
- Path: `/api/admin/auth/login`

请求体：

```json
{
  "account": "admin01",
  "password": "123456"
}
```

### 4.3 获取当前管理员信息

- Method: `GET`
- Path: `/api/admin/auth/me`
- Header:

```http
Authorization: Bearer <token>
```

## 5. 常见错误

- `username already exists`
- `email already exists`
- `invite code not found`
- `register invite code is incorrect`
- `password is incorrect`
- `account is disabled`
