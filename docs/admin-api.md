# 管理后台接口文档（详细版）

更新时间：2026-03-06

## 通用说明
- Base URL: `https://api.kuaiyi.info`
- 建议统一带 `Authorization: Bearer <admin token>`
- 统一响应：`{ code, msg, data }`

---

## 一、管理员认证

### 1. 管理员注册
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

### 2. 管理员登录
- Method: `POST`
- Path: `/api/admin/auth/login`

### 3. 当前管理员信息
- Method: `GET`
- Path: `/api/admin/auth/me`
- Header: `Authorization`

---

## 二、矿机管理

接口前缀：`/api/admin/machine`

- `GET /units`：算力单位列表
- `POST /`：新增矿机
- `PUT /{id}`：修改矿机
- `DELETE /{id}`：删除矿机
- `GET /{id}`：矿机详情
- `GET /list`：矿机列表（含收益估算）

新增/修改请求体示例：
```json
{
  "name": "S21 XP",
  "coinSymbol": "BTC",
  "hashrateValue": 1.2,
  "hashrateUnit": "PH",
  "pricePerUnit": 120000,
  "lockDays": 30,
  "status": 1
}
```

---

## 三、充值提现审核

### 1. 待审核充值
- Method: `GET`
- Path: `/api/admin/wallet/recharge/pending`

### 2. 待审核提现
- Method: `GET`
- Path: `/api/admin/wallet/withdraw/pending`

### 3. 审核充值
- Method: `POST`
- Path: `/api/admin/wallet/recharge/{id}/audit`

请求体：
```json
{
  "status": 1,
  "remark": "ok"
}
```

说明：
- `status=1` 通过，`status=2` 拒绝
- 当充值审核通过时会触发邀请返利（最多两级）

### 4. 审核提现
- Method: `POST`
- Path: `/api/admin/wallet/withdraw/{id}/audit`
- 请求体同上

---

## 四、轮播图与文件

### 1. 文件上传
- Method: `POST`
- Path: `/api/admin/file/upload`
- Content-Type: `multipart/form-data`
- 参数：`file`

### 2. 轮播图管理
- `POST /api/admin/banner`
- `PUT /api/admin/banner/{id}`
- `DELETE /api/admin/banner/{id}`
- `GET /api/admin/banner/{id}`
- `GET /api/admin/banner/list`

轮播图请求体示例：
```json
{
  "image": "/upload/banner-001.png"
}
```

---

## 五、系统配置（sys_config）

### 1. 配置列表
- Method: `GET`
- Path: `/api/admin/config/list`
- Query: `keyLike`（可选模糊查询）

### 2. 按 key 查询
- Method: `GET`
- Path: `/api/admin/config/{key}`

### 3. 按 key 修改
- Method: `PUT`
- Path: `/api/admin/config/{key}`

请求体示例：
```json
{
  "configValue": "0.08",
  "status": 1,
  "remark": "一级返利调整为8%"
}
```

限制：
- `recharge_*`（充值收款地址）禁止接口修改，只能数据库手工改。

建议接口维护项：
- `invite_rebate_level1_rate`
- `invite_rebate_level2_rate`
- `admin_register_invite_code`
