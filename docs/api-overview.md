# 接口总览（导航）

更新时间：2026-03-06

## 使用方式
- APP 端详细说明请看：`docs/app-api.md`
- 管理端详细说明请看：`docs/admin-api.md`

## APP 端模块

### 首页
- GET /api/public/pool/stats
- GET /api/public/rank/pow
- GET /api/public/pool/rankings
- GET /api/public/coin/detail
- GET /api/public/coin/chart
- GET /api/public/tool/calculator
- GET /api/dashboard/worker/stats
- GET /api/dashboard/hashrate/chart
- GET /api/dashboard/revenue/overview

### 矿机面板
- GET /api/admin/machine/list
- POST /api/order/machine
- GET /api/order/machine/list
- GET /api/order/machine/{id}
- POST /api/order/machine/{id}/sell
- POST /api/order/machine/{id}/cancel

### 收益
- GET /api/finance/account
- GET /api/finance/bill/list
- GET /api/wallet/invite/summary
- GET /api/wallet/invite/hierarchy
- GET /api/wallet/invite/rebate/list

### 我的
- POST /api/auth/register
- POST /api/auth/login
- GET /api/auth/me
- GET /api/wallet/account
- GET /api/wallet/recharge/address
- POST /api/wallet/recharge/submit
- POST /api/wallet/withdraw/submit
- GET /api/wallet/recharge/list
- GET /api/wallet/withdraw/list

## 管理后台模块

### 认证
- POST /api/admin/auth/register
- POST /api/admin/auth/login
- GET /api/admin/auth/me

### 矿机管理
- GET /api/admin/machine/units
- POST /api/admin/machine
- PUT /api/admin/machine/{id}
- DELETE /api/admin/machine/{id}
- GET /api/admin/machine/{id}
- GET /api/admin/machine/list

### 钱包审核
- GET /api/admin/wallet/recharge/pending
- GET /api/admin/wallet/withdraw/pending
- POST /api/admin/wallet/recharge/{id}/audit
- POST /api/admin/wallet/withdraw/{id}/audit

### 运营
- POST /api/admin/file/upload
- POST /api/admin/banner
- PUT /api/admin/banner/{id}
- DELETE /api/admin/banner/{id}
- GET /api/admin/banner/{id}
- GET /api/admin/banner/list

### 系统配置
- GET /api/admin/config/list
- GET /api/admin/config/{key}
- PUT /api/admin/config/{key}

