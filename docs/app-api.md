# APP 接口文档（完整返回字段）

更新时间：2026-03-06

## 0. 通用说明
- Base URL: `https://api.kuaiyi.info`
- 用户侧登录后请求头：`Authorization: Bearer <token>`
- 用户侧接口不再传 `userId`。
- 统一响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

## 1. 首页

### 1.1 矿池统计
- Method: `GET`
- Path: `/api/public/pool/stats`
- 数据来源：优先读取 Redis 币价缓存（缓存刷新频率 2 秒）
- 返回 `data`: `MiningCoin[]`

```json
[
  {
    "id": 3, // 币种ID
    "symbol": "ETHW", // 币种符号
    "name": "EthereumPoW", // 币种名称
    "logo": "https://...png", // 图标URL
    "algorithm": "Ethash", // 挖矿算法
    "poolHashrate": "2.38 TH/s", // 矿池算力
    "networkHashrate": "19.84 TH/s", // 全网算力
    "priceCny": 2.11000000, // 当前价格(CNY)
    "dailyRevenuePerP": 200.00000000, // 每P每日收益
    "status": 1, // 状态(1启用/0禁用)
    "marketCap": 228049294.00000000, // 市值
    "totalVolume": 16780485.00000000, // 24h成交量
    "priceChange24h": -4.1159, // 24h涨跌幅(%)
    "circulatingSupply": 107818717.04993000, // 流通量
    "totalSupply": 107818717.04993000, // 总供应量
    "currentBlockHeight": null, // 当前区块高度
    "networkDifficulty": null, // 当前网络难度
    "high24h": 2.21000000, // 24h最高价
    "low24h": 2.10000000, // 24h最低价
    "nextDifficulty": null, // 下一次难度
    "nextDifficultyChange": 0.0000, // 下一次难度变化(%)
    "difficultyAdjustmentTime": null // 难度调整时间
  }
]
```

### 1.2 PoW 收益排行
- Method: `GET`
- Path: `/api/public/rank/pow`
- 返回 `data`: `MiningCoin[]`（字段与 1.1 完全一致）

### 1.3 矿池排行
- Method: `GET`
- Path: `/api/public/pool/rankings`
- 返回 `data`: `PoolRankingItem[]`

```json
[
  {
    "name": "F2Pool",
    "hashrate": "115.47 EH/s",
    "share": "11.35%",
    "icon": "https://...png"
  }
]
```

### 1.4 挖矿收益计算器
- Method: `GET`
- Path: `/api/public/tool/calculator`
- Query:
  - `symbol`（必填）
  - `hashrate`（必填）
  - `unitFactor`（可选，默认 1）
- 返回 `data`:

```json
{
  "symbol": "BTC",
  "dailyRevenueCoin": 0.02000000,
  "dailyRevenueCny": 908.00000000,
  "price": 45400.00000000
}
```

### 1.5 在线/离线统计
- Method: `GET`
- Path: `/api/dashboard/worker/stats`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "total": 100,
  "online": 86,
  "offline": 14,
  "totalHashrate": "145.2 TH/s",
  "yesterdayRevenue": "0.12345678 BTC",
  "totalRevenue": "12.34567890 BTC",
  "totalPaid": "11.23456789 BTC",
  "balance": "1.11111101 BTC",
  "todayEstimated": "0.07407406 BTC"
}
```

### 1.6 算力图表
- Method: `GET`
- Path: `/api/dashboard/hashrate/chart`
- Header: `Authorization`
- Query: `timeRange`
- 返回 `data`: `HashratePoint[]`

```json
[
  {
    "time": 1741248000000,
    "hashrate": 102.34
  }
]
```

### 1.7 收益总览
- Method: `GET`
- Path: `/api/dashboard/revenue/overview`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "totalWorkers": 100,
  "onlineWorkers": 86,
  "offlineWorkers": 14,
  "todayMinedCoin": 0.02340000,
  "yesterdayRevenueCoin": 0.03900000,
  "totalRevenueCoin": 12.34567890,
  "avgHashrate24h": 145.20000000,
  "hashrateUnit": "TH/s",
  "coinSymbol": "BTC"
}
```

### 1.8 币种详情
- Method: `GET`
- Path: `/api/public/coin/detail`
- Query:
  - `id`（可选，币种ID）
  - `symbol`（可选，币种符号，如 BTC）
- 返回 `data`: `MiningCoin`（字段同 1.1 单项）

### 1.9 币价走势
- Method: `GET`
- Path: `/api/public/coin/chart`
- 数据源：外部真实币价数据源（OKX），仅日线（`1D` K线）
- 读取策略：接口只读 Redis 走势缓存（后台每 1 小时刷新一次）
- Query:
  - `id`（可选，币种ID）
  - `symbol`（可选，币种符号）
  - `days`（可选，支持 `7`、`30`、`180`、`365`，默认 `7`）
- 返回 `data`: `CoinChartPoint[]`（若数据源不可用或该币未映射，返回空数组，不做本地估算）

```json
[
  {
    "time": 1741248000000,
    "priceCny": 454321.12000000,
    "changePct": -1.2345
  }
]
```

## 2. 矿机面板

### 2.1 矿机列表
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

### 2.2 创建订单
- Method: `POST`
- Path: `/api/order/machine`
- Header: `Authorization`
- 请求体：

```json
{
  "machineId": 1,
  "quantity": 2
}
```

- 返回 `data`: `UserMachineOrderView`

```json
{
  "id": 10,
  "userId": 1001,
  "machineId": 1,
  "machineName": "S21 XP",
  "coinSymbol": "BTC",
  "hashrateValue": 1.20000000,
  "hashrateUnit": "PH",
  "quantity": 2,
  "unitPrice": 120000.00000000,
  "totalInvest": 240000.00000000,
  "totalHashrateTH": 2400.00000000,
  "todayRevenueCoin": 0.480000000000,
  "todayRevenueCny": 21792.00000000,
  "totalRevenueCoin": 0.000000000000000000,
  "totalRevenueCny": 0.00000000,
  "lockUntil": "2026-04-06T00:00:00",
  "sellAmountCny": 0.00000000,
  "sellTime": null,
  "status": 1,
  "createTime": "2026-03-06T12:00:00"
}
```

### 2.3 订单列表
- Method: `GET`
- Path: `/api/order/machine/list`
- Header: `Authorization`
- 返回 `data`: `UserMachineOrderView[]`（字段同 2.2）

### 2.4 订单详情
- Method: `GET`
- Path: `/api/order/machine/{id}`
- Header: `Authorization`
- 返回 `data`: `UserMachineOrderView`（字段同 2.2）

### 2.5 卖出订单
- Method: `POST`
- Path: `/api/order/machine/{id}/sell`
- Header: `Authorization`
- 返回 `data`: `UserMachineOrderView`（字段同 2.2，`status=2`）

### 2.6 取消订单
- Method: `POST`
- Path: `/api/order/machine/{id}/cancel`
- Header: `Authorization`
- 返回 `data`: `UserMachineOrderView`（字段同 2.2，`status=3`）

### 2.7 按P购买（币种详情页）
- Method: `POST`
- Path: `/api/order/machine/buy-by-p`
- Header: `Authorization`
- 说明：按币种直接购买算力，单位为 `P`。每P单价来自 `sys_config.machine_price_per_p_usd`（USD）。
- 规则：
  - 当前仅支持 `BTC`
  - 必须选择已绑定的收款地址 `receiveAddress`
  - 若未绑定地址，返回错误：`please bind receive address before buying`
- 请求体：

```json
{
  "coinSymbol": "BTC",
  "pCount": 1.5,
  "receiveAddress": "bc1qxxxx",
  "usdtPay": 100.00000000,
  "usdcPay": 50.00000000
}
```

- 返回 `data`: `UserMachineOrderView`（字段同 2.2）并附加：

```json
{
  "pricePerPUsd": 120.00000000,
  "usdtPay": 80.00000000,
  "usdcPay": 40.00000000
}
```

### 2.8 提取单个订单收益
- Method: `POST`
- Path: `/api/order/machine/{id}/revenue/withdraw`
- Header: `Authorization`
- 请求体：

```json
{
  "receiveAddress": "bc1qxxxxxx"
}
```

- 说明：发起后生成 `BTC/BTC` 提现审核单，后台审核通过后完成提现；审核拒绝会自动回滚该订单可提取收益。

### 2.9 一键提取收益
- Method: `POST`
- Path: `/api/order/machine/revenue/withdraw-all`
- Header: `Authorization`
- 请求体：

```json
{
  "receiveAddress": "bc1qxxxxxx",
  "orderIds": [101, 102]
}
```

### 2.10 可提取收益汇总
- Method: `GET`
- Path: `/api/order/machine/revenue/summary`
- Header: `Authorization`

## 3. 收益

### 3.1 账户资产
- Method: `GET`
- Path: `/api/finance/account`
- Header: `Authorization`
- Query: `coin`
- 返回 `data`: `FinanceAccount`

```json
{
  "id": 1,
  "userId": 1001,
  "coinSymbol": "BTC",
  "balance": 1.23450000,
  "totalRevenue": 12.34560000,
  "totalPaid": 11.11110000,
  "walletAddress": "bc1q...",
  "minPayout": 0.00100000
}
```

### 3.2 账单流水
- Method: `GET`
- Path: `/api/finance/bill/list`
- Header: `Authorization`
- Query: `coin`, `type`
- 返回 `data`: `FinanceBill[]`

```json
[
  {
    "id": 1,
    "userId": 1001,
    "coinSymbol": "BTC",
    "type": 1,
    "amount": 0.00230000,
    "createTime": "2026-03-06T10:00:00",
    "txId": "0xabc..."
  }
]
```

### 3.3 邀请统计
- Method: `GET`
- Path: `/api/wallet/invite/summary`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "userId": 1001,
  "level1Count": 12,
  "level2Count": 35,
  "totalInviteCount": 47,
  "level1Rate": 0.05000000,
  "level2Rate": 0.02000000,
  "totalRebateCny": 1234.56000000,
  "level1RebateTotalCny": 980.12000000,
  "level2RebateTotalCny": 254.44000000,
  "totalSourceRechargeCny": 32000.00000000
}
```

### 3.4 两级邀请关系
- Method: `GET`
- Path: `/api/wallet/invite/hierarchy`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "userId": 1001,
  "level1Count": 2,
  "level2Count": 3,
  "totalInviteCount": 5,
  "level1Users": [
    {
      "id": 2001,
      "username": "u1",
      "email": "u1@example.com",
      "inviteCode": "ABCD1234",
      "inviterId": 1001,
      "status": 1,
      "createTime": "2026-03-01T00:00:00",
      "level2Count": 2,
      "level2Users": [
        {
          "id": 3001,
          "username": "u2",
          "email": "u2@example.com",
          "inviteCode": "EFGH1234",
          "inviterId": 2001,
          "status": 1,
          "createTime": "2026-03-02T00:00:00"
        }
      ]
    }
  ]
}
```

### 3.5 返利明细
- Method: `GET`
- Path: `/api/wallet/invite/rebate/list`
- Header: `Authorization`
- 返回 `data`: `InviteRebateRecord[]`

```json
[
  {
    "id": 1,
    "beneficiaryUserId": 1001,
    "sourceUserId": 2001,
    "sourceUsername": "alice",
    "sourceEmail": "alice@example.com",
    "rechargeOrderId": 99,
    "level": 1,
    "sourceRechargeAmountCny": 1000.00000000,
    "rebateRate": 0.05000000,
    "rebateAmountCny": 50.00000000,
    "createTime": "2026-03-06T10:00:00"
  }
]
```

## 4. 我的

### 4.1 用户注册
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

- 返回 `data`:

```json
{
  "token": "eyJhbGciOi...",
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
```

### 4.2 用户登录
- Method: `POST`
- Path: `/api/auth/login`
- 请求体：

```json
{
  "account": "alice@example.com",
  "password": "123456"
}
```

- 返回 `data`：同 4.1

### 4.3 当前用户
- Method: `GET`
- Path: `/api/auth/me`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "inviteCode": "Q7M2K8P1",
  "inviterId": 10,
  "status": 1,
  "role": "USER",
  "subject": "alice",
  "expireAt": "2026-03-13T10:00:00"
}
```

### 4.4 钱包账户
- Method: `GET`
- Path: `/api/wallet/account`
- Header: `Authorization`
- 返回 `data`:

```json
{
  "userId": 1001,
  "balanceCny": 5000.00000000,
  "freezeCny": 1000.00000000,
  "totalRechargeCny": 12000.00000000,
  "totalWithdrawCny": 3000.00000000
}
```

### 4.5 充值地址
- Method: `GET`
- Path: `/api/wallet/recharge/address`
- 返回 `data`:

```json
{
  "USDT_TRC20": "TRC20_DEMO_ADDRESS",
  "USDT_ERC20": "ERC20_DEMO_ADDRESS",
  "USDC_TRC20": "TRC20_DEMO_ADDRESS",
  "USDC_ERC20": "ERC20_DEMO_ADDRESS"
}
```

### 4.6 提交充值
- Method: `POST`
- Path: `/api/wallet/recharge/submit`
- Header: `Authorization`
- 请求体：

```json
{
  "asset": "USDT",
  "network": "TRC20",
  "amountCny": 1000,
  "voucherImage": "/upload/recharge-proof-001.png"
}
```

- 返回 `data`: `RechargeOrderView`

```json
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
```

### 4.7 提交提现
- Method: `POST`
- Path: `/api/wallet/withdraw/submit`
- Header: `Authorization`
- 规则：
  - 资金密码必须已设置并校验通过
  - `receiveAddress` 必须是当前用户已绑定地址
  - 若未绑定地址，返回错误：`please bind receive address before withdraw`
- 请求体：

```json
{
  "asset": "USDC",
  "network": "ERC20",
  "amountCny": 500,
  "receiveAddress": "0xabc...",
  "withdrawPassword": "123456"
}
```

- 返回 `data`: `WithdrawOrderView`

```json
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
```

### 4.8 充值列表
- Method: `GET`
- Path: `/api/wallet/recharge/list`
- Header: `Authorization`
- 返回 `data`: `RechargeOrderView[]`（字段同 4.6）

### 4.9 提现列表
- Method: `GET`
- Path: `/api/wallet/withdraw/list`
- Header: `Authorization`
- 返回 `data`: `WithdrawOrderView[]`（字段同 4.7）

### 4.10 收款地址绑定
- Method: `POST`
- Path: `/api/wallet/receive-address/add`
- Header: `Authorization`
- 说明：新增用户收款地址，新增时必须验证资金密码。
- 请求体：

```json
{
  "receiveAddress": "bc1qxxxxxx",
  "remark": "币安主账户",
  "fundPassword": "123456"
}
```

### 4.11 收款地址列表
- Method: `GET`
- Path: `/api/wallet/receive-address/list`
- Header: `Authorization`
- 返回 `data`: `ReceiveAddressView[]`

```json
[
  {
    "id": 1,
    "userId": 1001,
    "receiveAddress": "bc1qxxxxxx",
    "remark": "币安主账户",
    "status": 1,
    "createTime": "2026-03-09T10:00:00",
    "updateTime": "2026-03-09T10:00:00"
  }
]
```

## 客服聊天

### 用户初始化会话
- Method: `POST`
- Path: `/api/chat/room/init`
- Header: `Authorization`

```json
{
  "roomId": 1,
  "userId": 1001,
  "adminId": null,
  "lastMessage": "",
  "lastMessageType": 1,
  "lastMessageTime": null,
  "unreadUser": 0,
  "unreadAdmin": 0,
  "status": 1,
  "createTime": "2026-03-07 08:00:00",
  "updateTime": "2026-03-07 08:00:00"
}
```

### 用户轮询消息
- Method: `GET`
- Path: `/api/chat/messages`
- Query: `roomId`(必填), `afterId`(选填), `limit`(选填)

```json
[
  {
    "id": 101,
    "roomId": 1,
    "senderType": 1,
    "senderId": 1001,
    "messageType": 1,
    "messageContent": "你好",
    "isRead": true,
    "createTime": "2026-03-07 08:01:00",
    "isSelf": true
  }
]
```

### 用户发送消息（文本/图片）
- Method: `POST`
- Path: `/api/chat/send`

```json
{
  "roomId": 1,
  "messageType": 1,
  "messageContent": "请问充值多久到账"
}
```

```json
{
  "roomId": 1,
  "messageType": 2,
  "messageContent": "https://api.kuaiyi.info/file/abc.png"
}
```

### 用户标记已读
- Method: `POST`
- Path: `/api/chat/read?roomId=1`

## 我的自选

### 添加自选
- Method: `POST`
- Path: `/api/favorite/{symbol}`

### 取消自选
- Method: `DELETE`
- Path: `/api/favorite/{symbol}`

### 检查是否已自选
- Method: `GET`
- Path: `/api/favorite/check?symbol=BTC`

```json
{
  "symbol": "BTC",
  "favorite": true
}
```

### 我的自选列表
- Method: `GET`
- Path: `/api/favorite/list`
- 返回字段：与矿池统计币种字段一致，附加 `favorite=true` 和 `favoriteTime`
