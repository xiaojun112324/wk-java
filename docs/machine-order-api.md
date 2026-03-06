# 矿机与订单接口文档

更新时间：2026-03-06

## 1. 管理端矿机

接口前缀：`/api/admin/machine`

### 1.1 查询算力单位

- Method: `GET`
- Path: `/api/admin/machine/units`

### 1.2 新增矿机

- Method: `POST`
- Path: `/api/admin/machine`

请求体：

```json
{
  "name": "S21 XP",
  "coinSymbol": "BTC",
  "hashrateValue": 1.2,
  "hashrateUnit": "PH",
  "pricePerUnit": 120000.00,
  "lockDays": 30,
  "status": 1
}
```

### 1.3 修改矿机

- Method: `PUT`
- Path: `/api/admin/machine/{id}`

### 1.4 删除矿机

- Method: `DELETE`
- Path: `/api/admin/machine/{id}`

### 1.5 矿机详情

- Method: `GET`
- Path: `/api/admin/machine/{id}`

### 1.6 矿机列表（含收益估算）

- Method: `GET`
- Path: `/api/admin/machine/list`

关键返回字段：

- `pricePerUnit`：单价
- `hashrateValue/hashrateUnit`：算力快照
- `hashrateTH`：折算 TH
- `dailyRevenueCoinPerUnit`：单台日收益（币）
- `dailyRevenueCnyPerUnit`：单台日收益（法币）

## 2. 用户矿机订单

接口前缀：`/api/order/machine`

### 2.1 创建订单

- Method: `POST`
- Path: `/api/order/machine`

请求体：

```json
{
  "userId": 1001,
  "machineId": 1,
  "quantity": 2
}
```

### 2.2 订单列表

- Method: `GET`
- Path: `/api/order/machine/list`

### 2.3 订单详情

- Method: `GET`
- Path: `/api/order/machine/{id}`

### 2.4 卖出订单

- Method: `POST`
- Path: `/api/order/machine/{id}/sell`

### 2.5 取消订单

- Method: `POST`
- Path: `/api/order/machine/{id}/cancel`

## 3. 订单状态

- `1`：持有中
- `2`：已卖出
- `3`：已取消
