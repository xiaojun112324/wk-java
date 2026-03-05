# 钱包、持仓卖出、充值提现工单接口

更新时间：2026-03-06

## 1) 持仓单新增能力

- 卖出：`POST /api/order/machine/{id}/sell`（需过锁仓期）
- 取消：`POST /api/order/machine/{id}/cancel`（仅无累计收益时可取消）
- 请求体：

```json
{
  "userId": 1001
}
```

订单状态：

- `1` 持仓中
- `2` 已卖出
- `3` 已取消

## 2) 用户钱包接口

- 查询钱包：`GET /api/wallet/account?userId=1001`
- 充值地址：`GET /api/wallet/recharge/address`
- 提交充值工单：`POST /api/wallet/recharge/submit`
- 提交提现工单：`POST /api/wallet/withdraw/submit`
- 用户充值单列表：`GET /api/wallet/recharge/list?userId=1001`
- 用户提现单列表：`GET /api/wallet/withdraw/list?userId=1001`

充值提交示例：

```json
{
  "userId": 1001,
  "asset": "USDT",
  "network": "TRC20",
  "amountCny": 1000,
  "voucherImage": "/upload/recharge-proof-001.png"
}
```

提现提交示例：

```json
{
  "userId": 1001,
  "asset": "USDC",
  "network": "ERC20",
  "amountCny": 500,
  "receiveAddress": "0xabc..."
}
```

## 3) 后台审核接口

- 待审核充值：`GET /api/admin/wallet/recharge/pending`
- 待审核提现：`GET /api/admin/wallet/withdraw/pending`
- 审核充值：`POST /api/admin/wallet/recharge/{id}/audit`
- 审核提现：`POST /api/admin/wallet/withdraw/{id}/audit`

审核请求体：

```json
{
  "status": 1,
  "remark": "ok"
}
```

- `status=1` 通过
- `status=2` 驳回

## 4) sys_config 需要配置

- `recharge_usdt_trc20_address`
- `recharge_usdt_erc20_address`
- `recharge_usdc_trc20_address`
- `recharge_usdc_erc20_address`
