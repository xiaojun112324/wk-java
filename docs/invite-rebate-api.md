# 邀请返利功能文档

更新时间：2026-03-06

## 1. 功能介绍

本功能用于实现“最多两级邀请返利”：

- 当被邀请用户充值工单审核通过后，系统自动给上级发放返利。
- 一级邀请人获得一级返利。
- 一级邀请人的上级获得二级返利。
- 每笔返利都会生成独立返利记录，支持后续查询统计。

## 2. 生效条件

返利发放触发点：

- 管理端审核充值工单接口：`POST /api/admin/wallet/recharge/{id}/audit`
- 且请求体 `status = 1`（审核通过）

只有满足上述条件才会发放返利。

## 3. 返利比例配置（sys_config）

- `invite_rebate_level1_rate`：一级返利比例，例如 `0.05` 表示 5%
- `invite_rebate_level2_rate`：二级返利比例，例如 `0.02` 表示 2%

说明：

- 比例按小数传，范围建议 `0 ~ 1`。
- 缺省或非法值会按 `0` 处理（即不返利）。

## 4. 数据库表

返利记录表：`invite_rebate_order`

关键字段：

- `beneficiary_user_id`：返利收益人（上级）
- `source_user_id`：充值来源用户（下级）
- `recharge_order_id`：关联充值工单
- `level`：返利层级（1 或 2）
- `source_recharge_amount_cny`：来源充值金额
- `rebate_rate`：返利比例
- `rebate_amount_cny`：返利金额

唯一约束：

- `(beneficiary_user_id, recharge_order_id, level)` 唯一
- 防止同一层级重复发放

## 5. 返利计算规则

- 一级返利金额 = `充值金额 * invite_rebate_level1_rate`
- 二级返利金额 = `充值金额 * invite_rebate_level2_rate`
- 金额保留 8 位小数，四舍五入

## 6. 查询接口

### 6.1 邀请统计

- Method: `GET`
- Path: `/api/wallet/invite/summary`
- Query: `userId`

返回字段：

- `totalInviteCount`：总邀请人数（一级+二级）
- `level1Count`：一级人数
- `level2Count`：二级人数
- `level1Rate`：一级返利比例
- `level2Rate`：二级返利比例
- `totalRebateCny`：总返利收益
- `totalSourceRechargeCny`：返利来源总充值金额

### 6.2 两级邀请关系

- Method: `GET`
- Path: `/api/wallet/invite/hierarchy`
- Query: `userId`

返回结构：

- `level1Users`：一级用户数组
- 每个一级用户中包含 `level2Users`
- 最多展示到二级

### 6.3 返利明细

- Method: `GET`
- Path: `/api/wallet/invite/rebate/list`
- Query: `userId`

返回字段：

- `rechargeOrderId`
- `sourceUserId`
- `sourceUsername`
- `sourceEmail`
- `level`
- `sourceRechargeAmountCny`
- `rebateRate`
- `rebateAmountCny`
- `createTime`

## 7. 示例

查询统计：

```bash
curl -G 'https://api.kuaiyi.info/api/wallet/invite/summary' --data-urlencode 'userId=1001'
```

查询返利明细：

```bash
curl -G 'https://api.kuaiyi.info/api/wallet/invite/rebate/list' --data-urlencode 'userId=1001'
```
