# 认证接口文档

更新时间：2026-03-06

## 通用说明

- Base URL: `https://api.kuaiyi.info`
- Content-Type: `application/json`
- 统一返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

## JWT 说明

- 登录/注册成功后，`data.token` 返回 JWT。
- 同时返回：
  - `tokenType`: `Bearer`
  - `expiresIn`: 过期秒数（默认 `604800`，即 7 天）
- 业务接口调用时，HTTP Header 使用：

```http
Authorization: Bearer <token>
```

---

## 用户认证（`user`）

### 1) 注册

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

成功响应：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.yyy",
    "tokenType": "Bearer",
    "expiresIn": 604800,
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

### 2) 登录

- Method: `POST`
- Path: `/api/auth/login`

请求体：

```json
{
  "account": "alice@example.com",
  "password": "123456"
}
```

---

## 管理员认证（`sys_user`）

### 1) 注册

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

### 2) 登录

- Method: `POST`
- Path: `/api/admin/auth/login`

请求体：

```json
{
  "account": "admin01",
  "password": "123456"
}
```
