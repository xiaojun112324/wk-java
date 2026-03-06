# 钱包、充值提现、审核接口文档

更新时间：2026-03-06

## 1. 功能范围

本模块包含：

- 用户钱包余额查询
- 用户充值工单提交与查询
- 用户提现工单提交与查询
- 管理员审核充值/提现
- 充值审核通过后触发邀请返利

## 2. 用户接口

### 2.1 钱包账户

- Method: `GET`
- Path: `/api/wallet/account`
- Query: `userId`

返回：余额、冻结金额、累计充值、累计提现。

### 2.2 充值地址

- Method: `GET`
- Path: `/api/wallet/recharge/address`

返回系统配置的 USDT/USDC 收款地址（TRC20/ERC20）。

### 2.3 提交充值工单

- Method: `POST`
- Path: `/api/wallet/recharge/submit`

请求体：

```json
{
  "userId": 1001,
  "asset": "USDT",
  "network": "TRC20",
  "amountCny": 1000,
  "voucherImage": "/upload/recharge-proof-001.png"
}
```

### 2.4 提交提现工单

- Method: `POST`
- Path: `/api/wallet/withdraw/submit`

请求体：

```json
{
  "userId": 1001,
  "asset": "USDC",
  "network": "ERC20",
  "amountCny": 500,
  "receiveAddress": "0xabc..."
}
```

### 2.5 充值工单列表

- Method: `GET`
- Path: `/api/wallet/recharge/list`
- Query: `userId`

### 2.6 提现工单列表

- Method: `GET`
- Path: `/api/wallet/withdraw/list`
- Query: `userId`

## 3. 管理员审核接口

### 3.1 待审核充值列表

- Method: `GET`
- Path: `/api/admin/wallet/recharge/pending`

### 3.2 待审核提现列表

- Method: `GET`
- Path: `/api/admin/wallet/withdraw/pending`

### 3.3 审核充值

- Method: `POST`
- Path: `/api/admin/wallet/recharge/{id}/audit`

请求体：

```json
{
  "status": 1,
  "remark": "ok"
}
```

状态：

- `status=1`：通过
- `status=2`：拒绝

### 3.4 审核提现

- Method: `POST`
- Path: `/api/admin/wallet/withdraw/{id}/audit`

请求体同上。

## 4. 返利触发说明（重点）

当充值工单审核通过（`status=1`）时：

1. 给当前充值用户加款。
2. 若充值用户存在上级（`inviterId`），按配置给一级上级返利。
3. 若一级上级还有上级，再按配置给二级上级返利。
4. 每笔返利写入返利记录表，支持后续查询。

## 5. 常见错误

- `userId is required`
- `amountCny must be greater than 0`
- `insufficient balance`
- `recharge order not found`
- `recharge order already audited`
- `status must be 1(approve) or 2(reject)`
