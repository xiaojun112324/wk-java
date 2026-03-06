# 管理后台接口文档（完整返回字段）

更新时间：2026-03-06

## 0. 通用说明
- Base URL: `https://api.kuaiyi.info`
- 建议请求头：`Authorization: Bearer <admin token>`
- 统一响应结构：`{ code, msg, data }`

## 1. 管理员认证

### 1.1 注册
- Method: `POST`
- Path: `/api/admin/auth/register`
- 请求体：

```json
{
  "username": "admin01",
  "email": "admin01@example.com",
  "password": "123456",
  "registerInviteCode": "ADMIN2026"
}
```

- 返回 `data`:

```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "user": {
    "id": 1,
    "username": "admin01",
    "email": "admin01@example.com",
    "status": 1
  }
}
```

### 1.2 登录
- Method: `POST`
- Path: `/api/admin/auth/login`
- 请求体：

```json
{
  "account": "admin01",
  "password": "123456"
}
```

- 返回 `data`：同 1.1

### 1.3 当前管理员
- Method: `GET`
- Path: `/api/admin/auth/me`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "id": 1,
  "username": "admin01",
  "email": "admin01@example.com",
  "status": 1,
  "role": "ADMIN",
  "subject": "admin01",
  "expireAt": "2026-03-13T10:00:00"
}
```

## 2. 矿机管理

### 2.1 算力单位
- Method: `GET`
- Path: `/api/admin/machine/units`
- 返回 `data`:

```json
["H", "KH", "MH", "GH", "TH", "PH", "EH"]
```

### 2.2 新增矿机
- Method: `POST`
- Path: `/api/admin/machine`
- 请求体：

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

- 返回 `data`（MiningMachine）：

```json
{
  "id": 1,
  "name": "S21 XP",
  "coinSymbol": "BTC",
  "hashrateValue": 1.20000000,
  "hashrateUnit": "PH",
  "pricePerUnit": 120000.00000000,
  "lockDays": 30,
  "status": 1,
  "createTime": "2026-03-06T10:00:00",
  "updateTime": "2026-03-06T10:00:00"
}
```

### 2.3 修改矿机
- Method: `PUT`
- Path: `/api/admin/machine/{id}`
- 请求体同 2.2
- 返回 `data`：MiningMachine（字段同 2.2）

### 2.4 删除矿机
- Method: `DELETE`
- Path: `/api/admin/machine/{id}`
- 返回 `data`:

```json
true
```

### 2.5 矿机详情
- Method: `GET`
- Path: `/api/admin/machine/{id}`
- 返回 `data`：MiningMachine（字段同 2.2）

### 2.6 矿机列表（含收益估算）
- Method: `GET`
- Path: `/api/admin/machine/list`
- 返回 `data`: `MachineListItem[]`

```json
[
  {
    "id": 1,
    "name": "S21 XP",
    "coinSymbol": "BTC",
    "hashrateValue": 1.20000000,
    "hashrateUnit": "PH",
    "pricePerUnit": 120000.00000000,
    "lockDays": 30,
    "status": 1,
    "hashrateTH": 1200.00000000,
    "dailyRevenueCoinPerUnit": 0.240000000000,
    "dailyRevenueCnyPerUnit": 10896.00000000
  }
]
```

## 3. 钱包审核

### 3.1 待审核充值列表
- Method: `GET`
- Path: `/api/admin/wallet/recharge/pending`
- 返回 `data`: `RechargeOrderView[]`

```json
[
  {
    "id": 1,
    "userId": 1001,
    "asset": "USDT",
    "network": "TRC20",
    "amountCny": 1000.00000000,
    "voucherImage": "/upload/recharge-proof-001.png",
    "status": 0,
    "auditRemark": null,
    "auditTime": null,
    "createTime": "2026-03-06T10:00:00"
  }
]
```

### 3.2 待审核提现列表
- Method: `GET`
- Path: `/api/admin/wallet/withdraw/pending`
- 返回 `data`: `WithdrawOrderView[]`

```json
[
  {
    "id": 1,
    "userId": 1001,
    "asset": "USDC",
    "network": "ERC20",
    "amountCny": 500.00000000,
    "receiveAddress": "0xabc...",
    "status": 0,
    "auditRemark": null,
    "auditTime": null,
    "createTime": "2026-03-06T10:00:00"
  }
]
```

### 3.3 审核充值
- Method: `POST`
- Path: `/api/admin/wallet/recharge/{id}/audit`
- 请求体：

```json
{
  "status": 1,
  "remark": "ok"
}
```

- 返回 `data`: `RechargeOrderView`（字段同 3.1）
- 规则：`status=1` 通过、`status=2` 拒绝
- 说明：充值通过会触发两级返利发放

### 3.4 审核提现
- Method: `POST`
- Path: `/api/admin/wallet/withdraw/{id}/audit`
- 请求体同 3.3
- 返回 `data`: `WithdrawOrderView`（字段同 3.2）

## 4. 轮播图与文件

### 4.1 文件上传
- Method: `POST`
- Path: `/api/admin/file/upload`
- Content-Type: `multipart/form-data`
- 参数：`file`
- 返回 `data`:

```json
{
  "fileName": "a4e5c1f2.png",
  "path": "/www/wwwroot/upload/a4e5c1f2.png",
  "relativePath": "/upload/a4e5c1f2.png"
}
```

### 4.2 新增轮播图
- Method: `POST`
- Path: `/api/admin/banner`
- 请求体：

```json
{
  "image": "/upload/banner-001.png"
}
```

- 返回 `data`（Banner）：

```json
{
  "id": 1,
  "image": "/upload/banner-001.png",
  "createTime": "2026-03-06T10:00:00",
  "updateTime": "2026-03-06T10:00:00"
}
```

### 4.3 修改轮播图
- Method: `PUT`
- Path: `/api/admin/banner/{id}`
- 请求体同 4.2
- 返回 `data`：Banner（字段同 4.2）

### 4.4 删除轮播图
- Method: `DELETE`
- Path: `/api/admin/banner/{id}`
- 返回 `data`:

```json
true
```

### 4.5 轮播图详情
- Method: `GET`
- Path: `/api/admin/banner/{id}`
- 返回 `data`：Banner（字段同 4.2）

### 4.6 轮播图列表
- Method: `GET`
- Path: `/api/admin/banner/list`
- 返回 `data`: `Banner[]`（字段同 4.2）

## 5. 系统配置（sys_config）

### 5.1 配置列表
- Method: `GET`
- Path: `/api/admin/config/list`
- Query: `keyLike`（可选）
- 返回 `data`: `SysConfigView[]`

```json
[
  {
    "id": 1,
    "configKey": "invite_rebate_level1_rate",
    "configValue": "0.05",
    "status": 1,
    "remark": "一级返利比例",
    "createTime": "2026-03-06T10:00:00",
    "updateTime": "2026-03-06T10:00:00"
  }
]
```

### 5.2 按 key 查询
- Method: `GET`
- Path: `/api/admin/config/{key}`
- 返回 `data`: `SysConfigView`（字段同 5.1）

### 5.3 按 key 修改
- Method: `PUT`
- Path: `/api/admin/config/{key}`
- 请求体：

```json
{
  "configValue": "0.08",
  "status": 1,
  "remark": "一级返利调整为8%"
}
```

- 返回 `data`: `SysConfigView`（字段同 5.1）

限制：
- `recharge_*` 收款地址类配置禁止接口修改（仅数据库可改）。


