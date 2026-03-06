# APP 接口文档（详细版）

更新时间：2026-03-06

## 通用说明
- Base URL: `https://api.kuaiyi.info`
- 登录后统一请求头：`Authorization: Bearer <token>`
- 用户侧接口不再传 `userId`，后端从 token 自动解析。
- 统一响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

---

## 一、首页

### 1. 矿池统计
- Method: `GET`
- Path: `/api/public/pool/stats`
- 说明：返回矿池关键统计数据。

### 2. PoW 收益排行
- Method: `GET`
- Path: `/api/public/rank/pow`
- 说明：返回 PoW 收益排名信息。

### 3. 矿池排行
- Method: `GET`
- Path: `/api/public/pool/rankings`

### 4. 挖矿收益计算器
- Method: `GET`
- Path: `/api/public/tool/calculator`
- Query:
  - `symbol`：币种，例如 `BTC`
  - `hashrate`：算力值
  - `unitFactor`：算力系数（默认 `1`）

请求示例：
```bash
curl -G 'https://api.kuaiyi.info/api/public/tool/calculator' \
  --data-urlencode 'symbol=BTC' \
  --data-urlencode 'hashrate=100' \
  --data-urlencode 'unitFactor=1'
```

### 5. 矿工在线离线统计
- Method: `GET`
- Path: `/api/dashboard/worker/stats`
- Header: `Authorization`

### 6. 算力图表
- Method: `GET`
- Path: `/api/dashboard/hashrate/chart`
- Header: `Authorization`
- Query: `timeRange`（默认 `24h`）

### 7. 收益总览
- Method: `GET`
- Path: `/api/dashboard/revenue/overview`
- Header: `Authorization`
- 返回字段：
  - `totalWorkers`：总矿机数
  - `onlineWorkers`：在线台数
  - `offlineWorkers`：离线台数
  - `todayMinedCoin`：今日挖币
  - `yesterdayRevenueCoin`：昨日收益
  - `totalRevenueCoin`：总收益
  - `avgHashrate24h`：24h 平均算力
  - `hashrateUnit`：默认 `TH/s`
  - `coinSymbol`：默认 `BTC`

---

## 二、矿机面板

### 1. 矿机列表
- Method: `GET`
- Path: `/api/admin/machine/list`
- 说明：APP 端直接读取可售矿机列表。

### 2. 创建矿机订单（购买）
- Method: `POST`
- Path: `/api/order/machine`
- Header: `Authorization`
- 请求体（无需 userId）：

```json
{
  "machineId": 1,
  "quantity": 2
}
```

### 3. 我的订单列表
- Method: `GET`
- Path: `/api/order/machine/list`
- Header: `Authorization`

### 4. 订单详情
- Method: `GET`
- Path: `/api/order/machine/{id}`
- Header: `Authorization`
- 说明：后端会校验订单归属。

### 5. 卖出订单
- Method: `POST`
- Path: `/api/order/machine/{id}/sell`
- Header: `Authorization`
- Body：可空 `{}`

### 6. 取消订单
- Method: `POST`
- Path: `/api/order/machine/{id}/cancel`
- Header: `Authorization`
- Body：可空 `{}`

---

## 三、收益

### 1. 账户资产
- Method: `GET`
- Path: `/api/finance/account`
- Header: `Authorization`
- Query: `coin`（如 `BTC`）

### 2. 账单流水
- Method: `GET`
- Path: `/api/finance/bill/list`
- Header: `Authorization`
- Query:
  - `coin`（如 `BTC`）
  - `type`（1 收益 / 2 支出）

### 3. 邀请返利统计
- Method: `GET`
- Path: `/api/wallet/invite/summary`
- Header: `Authorization`
- 返回重点：
  - `totalInviteCount`
  - `level1Count`
  - `level2Count`
  - `level1Rate`
  - `level2Rate`
  - `totalRebateCny`
  - `level1RebateTotalCny`
  - `level2RebateTotalCny`
  - `totalSourceRechargeCny`

### 4. 两级邀请关系
- Method: `GET`
- Path: `/api/wallet/invite/hierarchy`
- Header: `Authorization`

### 5. 返利明细
- Method: `GET`
- Path: `/api/wallet/invite/rebate/list`
- Header: `Authorization`
- 说明：每条记录对应一笔下级充值触发的返利。

---

## 四、我的

### 1. 用户注册
- Method: `POST`
- Path: `/api/auth/register`
- 请求体：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456",
  "inviteCode": "AB12CD34"
}
```

### 2. 用户登录
- Method: `POST`
- Path: `/api/auth/login`
- 请求体：

```json
{
  "account": "alice@example.com",
  "password": "123456"
}
```

登录成功示例：
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

### 3. 当前用户信息
- Method: `GET`
- Path: `/api/auth/me`
- Header: `Authorization`

### 4. 钱包接口
- `GET /api/wallet/account`
- `GET /api/wallet/recharge/address`
- `POST /api/wallet/recharge/submit`
- `POST /api/wallet/withdraw/submit`
- `GET /api/wallet/recharge/list`
- `GET /api/wallet/withdraw/list`

充值请求示例（无需 userId）：
```json
{
  "asset": "USDT",
  "network": "TRC20",
  "amountCny": 1000,
  "voucherImage": "/upload/recharge-proof-001.png"
}
```

提现请求示例（无需 userId）：
```json
{
  "asset": "USDC",
  "network": "ERC20",
  "amountCny": 500,
  "receiveAddress": "0xabc..."
}
```
