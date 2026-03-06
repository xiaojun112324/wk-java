# 系统配置接口文档（sys_config）

更新时间：2026-03-06

## 1. 功能介绍

提供后台对 `sys_config` 的查询和修改能力，用于管理返利比例、管理员注册邀请码等配置。

安全限制：

- `recharge_*` 收款地址类配置禁止通过接口修改。
- 收款地址只能通过数据库手工修改。

## 2. 接口列表

### 2.1 配置列表查询

- Method: `GET`
- Path: `/api/admin/config/list`
- Query:
  - `keyLike`（可选）：配置键模糊搜索

示例：

```bash
curl -G 'https://api.kuaiyi.info/api/admin/config/list' --data-urlencode 'keyLike=invite_rebate'
```

### 2.2 按 key 查询

- Method: `GET`
- Path: `/api/admin/config/{key}`

示例：

```bash
curl 'https://api.kuaiyi.info/api/admin/config/invite_rebate_level1_rate'
```

### 2.3 按 key 修改

- Method: `PUT`
- Path: `/api/admin/config/{key}`
- Body（可部分更新）：

```json
{
  "configValue": "0.08",
  "status": 1,
  "remark": "一级返利调整为8%"
}
```

规则：

- `status` 仅允许 `0/1`
- `recharge_*` 键禁止接口修改，会返回错误

## 3. 返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 12,
    "configKey": "invite_rebate_level1_rate",
    "configValue": "0.08",
    "status": 1,
    "remark": "一级返利调整为8%",
    "createTime": "2026-03-06T10:00:00",
    "updateTime": "2026-03-06T12:00:00"
  }
}
```

## 4. 推荐管理项

建议通过此接口管理的键：

- `admin_register_invite_code`
- `invite_rebate_level1_rate`
- `invite_rebate_level2_rate`

建议仅数据库管理的键：

- `recharge_usdt_trc20_address`
- `recharge_usdt_erc20_address`
- `recharge_usdc_trc20_address`
- `recharge_usdc_erc20_address`
