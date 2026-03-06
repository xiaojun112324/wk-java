# API 请求方式总览

更新时间：2026-03-06

## Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/admin/auth/register`
- `POST /api/admin/auth/login`

## Public

- `GET /api/public/pool/stats`
- `GET /api/public/rank/pow`
- `GET /api/public/pool/rankings`
- `GET /api/public/tool/calculator`

## Dashboard

- `GET /api/dashboard/worker/stats`
- `GET /api/dashboard/hashrate/chart`

## Finance

- `GET /api/finance/account`
- `GET /api/finance/bill/list`

## Admin Banner

- `POST /api/admin/banner`
- `PUT /api/admin/banner/{id}`
- `DELETE /api/admin/banner/{id}`
- `GET /api/admin/banner/{id}`
- `GET /api/admin/banner/list`

## Admin File

- `POST /api/admin/file/upload`

## Admin Mining Machine

- `GET /api/admin/machine/units`
- `POST /api/admin/machine`
- `PUT /api/admin/machine/{id}`
- `DELETE /api/admin/machine/{id}`
- `GET /api/admin/machine/{id}`
- `GET /api/admin/machine/list`

## User Machine Order

- `POST /api/order/machine`
- `GET /api/order/machine/list`
- `GET /api/order/machine/{id}`
- `POST /api/order/machine/{id}/sell`
- `POST /api/order/machine/{id}/cancel`

## User Wallet

- `GET /api/wallet/account`
- `GET /api/wallet/recharge/address`
- `POST /api/wallet/recharge/submit`
- `POST /api/wallet/withdraw/submit`
- `GET /api/wallet/recharge/list`
- `GET /api/wallet/withdraw/list`

## Admin Wallet Audit

- `GET /api/admin/wallet/recharge/pending`
- `GET /api/admin/wallet/withdraw/pending`
- `POST /api/admin/wallet/recharge/{id}/audit`
- `POST /api/admin/wallet/withdraw/{id}/audit`
