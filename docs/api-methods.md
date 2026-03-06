# API 请求方式总览（中文说明）

更新时间：2026-03-06

## 说明

- 本文档用于快速查看所有接口的请求方式、路径和用途。
- 详细字段请看对应模块文档（auth-api、wallet-order-audit-api、invite-rebate-api、sys-config-api 等）。

## 一、认证模块

- `POST /api/auth/register`：用户注册，支持填写上级邀请码。
- `POST /api/auth/login`：用户登录，返回 JWT。
- `GET /api/auth/me`：根据 JWT 获取当前登录用户信息。
- `POST /api/admin/auth/register`：管理员注册（需要系统邀请码）。
- `POST /api/admin/auth/login`：管理员登录，返回 JWT。
- `GET /api/admin/auth/me`：根据 JWT 获取当前登录管理员信息。

## 二、公共数据模块

- `GET /api/public/pool/stats`：获取矿池基础统计。
- `GET /api/public/rank/pow`：获取 PoW 收益排行。
- `GET /api/public/pool/rankings`：获取矿池排行。
- `GET /api/public/tool/calculator`：挖矿收益计算器。

## 三、用户看板与财务

- `GET /api/dashboard/worker/stats`：获取矿工在线离线统计。
- `GET /api/dashboard/hashrate/chart`：获取算力图表。
- `GET /api/finance/account`：获取用户账户资产信息。
- `GET /api/finance/bill/list`：获取账单流水列表。

## 四、管理端矿机

- `GET /api/admin/machine/units`：查询支持的算力单位。
- `POST /api/admin/machine`：新增矿机。
- `PUT /api/admin/machine/{id}`：修改矿机。
- `DELETE /api/admin/machine/{id}`：删除矿机。
- `GET /api/admin/machine/{id}`：矿机详情。
- `GET /api/admin/machine/list`：矿机列表（含收益估算）。

## 五、用户矿机订单

- `POST /api/order/machine`：创建矿机订单（购买）。
- `GET /api/order/machine/list`：用户矿机订单列表。
- `GET /api/order/machine/{id}`：订单详情。
- `POST /api/order/machine/{id}/sell`：卖出订单（锁仓期后）。
- `POST /api/order/machine/{id}/cancel`：取消订单（满足条件时）。

## 六、钱包与充值提现

- `GET /api/wallet/account`：钱包资产查询。
- `GET /api/wallet/recharge/address`：充值地址配置读取。
- `POST /api/wallet/recharge/submit`：提交充值工单。
- `POST /api/wallet/withdraw/submit`：提交提现工单。
- `GET /api/wallet/recharge/list`：用户充值工单列表。
- `GET /api/wallet/withdraw/list`：用户提现工单列表。

## 七、邀请返利

- `GET /api/wallet/invite/summary`：邀请统计（总人数、一级二级人数、总返利等）。
- `GET /api/wallet/invite/hierarchy`：两级邀请关系树。
- `GET /api/wallet/invite/rebate/list`：返利明细（每笔充值对应返利记录）。

## 八、后台审核

- `GET /api/admin/wallet/recharge/pending`：待审核充值工单。
- `GET /api/admin/wallet/withdraw/pending`：待审核提现工单。
- `POST /api/admin/wallet/recharge/{id}/audit`：审核充值工单（通过会触发返利）。
- `POST /api/admin/wallet/withdraw/{id}/audit`：审核提现工单。

## 九、管理端运营接口

- `POST /api/admin/file/upload`：上传文件。
- `POST /api/admin/banner`：新增轮播图。
- `PUT /api/admin/banner/{id}`：修改轮播图。
- `DELETE /api/admin/banner/{id}`：删除轮播图。
- `GET /api/admin/banner/{id}`：轮播图详情。
- `GET /api/admin/banner/list`：轮播图列表。

## 十、系统配置

- `GET /api/admin/config/list`：查询配置列表（可按 keyLike 模糊过滤）。
- `GET /api/admin/config/{key}`：按配置键查询。
- `PUT /api/admin/config/{key}`：按配置键修改（`recharge_*` 收款地址配置禁止接口修改）。
