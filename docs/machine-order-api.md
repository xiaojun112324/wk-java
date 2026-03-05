# 矿机与购买订单接口文档（前端联调）

更新时间：2026-03-05

## 1. 算力单位说明

系统支持单位：

- `H`
- `KH`
- `MH`
- `GH`
- `TH`
- `PH`
- `EH`

换算到 TH 的系数：

- `1 H = 0.000000000001 TH`
- `1 KH = 0.000000001 TH`
- `1 MH = 0.000001 TH`
- `1 GH = 0.001 TH`
- `1 TH = 1 TH`
- `1 PH = 1000 TH`
- `1 EH = 1000000 TH`

---

## 2. 管理员矿机接口

前缀：`/api/admin/machine`

### 2.1 查询支持单位

- `GET /api/admin/machine/units`

### 2.2 新增矿机

- `POST /api/admin/machine`

请求体：

```json
{
  "name": "S21 XP",
  "coinSymbol": "BTC",
  "hashrateValue": 1.2,
  "hashrateUnit": "PH",
  "pricePerUnit": 120000.00,
  "status": 1
}
```

### 2.3 修改矿机

- `PUT /api/admin/machine/{id}`

### 2.4 删除矿机

- `DELETE /api/admin/machine/{id}`

### 2.5 矿机详情

- `GET /api/admin/machine/{id}`

### 2.6 矿机列表（含收益估算）

- `GET /api/admin/machine/list`

返回字段重点：

- `pricePerUnit`：单台单价
- `hashrateValue + hashrateUnit`：单台算力
- `hashrateTH`：折算 TH
- `dailyRevenueCoinPerUnit`：单台预计今日币收益
- `dailyRevenueCnyPerUnit`：单台预计今日法币收益

---

## 3. 用户矿机订单接口

前缀：`/api/order/machine`

### 3.1 创建订单（购买矿机）

- `POST /api/order/machine`

请求体：

```json
{
  "userId": 1001,
  "machineId": 1,
  "quantity": 2
}
```

返回字段重点：

- `quantity`：买入台数/份数
- `unitPrice`：下单时单价快照
- `totalInvest`：总投入（`unitPrice * quantity`）
- `totalHashrateTH`：总算力（统一折算 TH）
- `todayRevenueCoin`：今日收益（币）
- `todayRevenueCny`：今日收益（法币）
- `totalRevenueCoin`：累计收益（币）
- `totalRevenueCny`：累计收益（法币）

### 3.2 用户订单列表

- `GET /api/order/machine/list?userId=1001`

### 3.3 订单详情

- `GET /api/order/machine/{id}`

---

## 4. 数据表

- 矿机表：`mining_machine`
- 用户矿机订单表：`user_machine_order`

你要求的字段都已落库：

- 买入单位（算力单位）/数量
- 单价
- 总投入
- 累计收益
- 今日收益
