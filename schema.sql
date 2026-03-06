-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT 'account username',
  `email` varchar(128) NOT NULL COMMENT 'account email',
  `password` varchar(128) NOT NULL COMMENT 'encrypted password',
  `invite_code` varchar(32) NOT NULL COMMENT 'self invite code',
  `inviter_id` bigint(20) DEFAULT NULL COMMENT 'parent inviter user id',
  `status` int(2) DEFAULT '1' COMMENT '1 active, 0 disabled',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user table';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT 'admin username',
  `email` varchar(128) NOT NULL COMMENT 'admin email',
  `password` varchar(128) NOT NULL COMMENT 'encrypted password',
  `status` int(2) DEFAULT '1' COMMENT '1 active, 0 disabled',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_username` (`username`),
  UNIQUE KEY `uk_sys_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='admin user table';

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) NOT NULL COMMENT 'config key',
  `config_value` varchar(512) NOT NULL COMMENT 'config value',
  `status` int(2) DEFAULT '1' COMMENT '1 active, 0 disabled',
  `remark` varchar(255) DEFAULT NULL COMMENT 'remark',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system config table';

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `image` varchar(512) NOT NULL COMMENT 'banner image url/path',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='banner table';

-- ----------------------------
-- Table structure for mining_machine
-- ----------------------------
DROP TABLE IF EXISTS `mining_machine`;
CREATE TABLE `mining_machine` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT 'machine name',
  `coin_symbol` varchar(16) NOT NULL COMMENT 'coin symbol, e.g. BTC',
  `hashrate_value` decimal(20,8) NOT NULL COMMENT 'single machine hashrate value',
  `hashrate_unit` varchar(8) NOT NULL COMMENT 'H/KH/MH/GH/TH/PH/EH',
  `price_per_unit` decimal(20,8) NOT NULL COMMENT 'single machine price',
  `lock_days` int(11) DEFAULT '30' COMMENT 'lock period days before sell',
  `status` int(2) DEFAULT '1' COMMENT '1 on sale, 0 off sale',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mining machine table';

-- ----------------------------
-- Table structure for user_machine_order
-- ----------------------------
DROP TABLE IF EXISTS `user_machine_order`;
CREATE TABLE `user_machine_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'buyer user id',
  `machine_id` bigint(20) NOT NULL COMMENT 'mining machine id',
  `coin_symbol` varchar(16) NOT NULL COMMENT 'coin symbol snapshot',
  `machine_name` varchar(128) NOT NULL COMMENT 'machine name snapshot',
  `hashrate_value` decimal(20,8) NOT NULL COMMENT 'single machine hashrate snapshot',
  `hashrate_unit` varchar(8) NOT NULL COMMENT 'H/KH/MH/GH/TH/PH/EH',
  `quantity` int(11) NOT NULL COMMENT 'buy quantity',
  `unit_price` decimal(20,8) NOT NULL COMMENT 'unit price snapshot',
  `total_invest` decimal(20,8) NOT NULL COMMENT 'total invested = unit_price * quantity',
  `total_hashrate_th` decimal(30,8) NOT NULL COMMENT 'total hashrate converted to TH',
  `today_revenue_coin` decimal(30,18) DEFAULT '0.000000000000000000' COMMENT 'today revenue in coin',
  `today_revenue_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'today revenue in CNY',
  `total_revenue_coin` decimal(30,18) DEFAULT '0.000000000000000000' COMMENT 'accumulated revenue in coin',
  `total_revenue_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'accumulated revenue in CNY',
  `lock_until` datetime DEFAULT NULL COMMENT 'cannot sell before this time',
  `sell_amount_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'settled amount when sold/canceled',
  `sell_time` datetime DEFAULT NULL COMMENT 'sell/cancel operate time',
  `status` int(2) DEFAULT '1' COMMENT '1 holding, 2 sold, 3 canceled',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_machine_id` (`machine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user mining machine order table';

-- ----------------------------
-- Table structure for user_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `balance_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'available CNY balance',
  `freeze_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'frozen CNY balance',
  `total_recharge_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'approved recharge total',
  `total_withdraw_cny` decimal(30,8) DEFAULT '0.00000000' COMMENT 'approved withdraw total',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user wallet table';

-- ----------------------------
-- Table structure for recharge_order
-- ----------------------------
DROP TABLE IF EXISTS `recharge_order`;
CREATE TABLE `recharge_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `asset` varchar(16) NOT NULL COMMENT 'USDT/USDC',
  `network` varchar(16) NOT NULL COMMENT 'TRC20/ERC20',
  `amount_cny` decimal(30,8) NOT NULL COMMENT 'recharge amount in CNY',
  `voucher_image` varchar(512) NOT NULL COMMENT 'voucher image path/url',
  `status` int(2) DEFAULT '0' COMMENT '0 pending, 1 approved, 2 rejected',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT 'audit remark',
  `audit_time` datetime DEFAULT NULL COMMENT 'audit time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recharge_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='recharge order ticket';

-- ----------------------------
-- Table structure for withdraw_order
-- ----------------------------
DROP TABLE IF EXISTS `withdraw_order`;
CREATE TABLE `withdraw_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `asset` varchar(16) NOT NULL COMMENT 'USDT/USDC',
  `network` varchar(16) NOT NULL COMMENT 'TRC20/ERC20',
  `amount_cny` decimal(30,8) NOT NULL COMMENT 'withdraw amount in CNY',
  `receive_address` varchar(255) NOT NULL COMMENT 'user receive address',
  `status` int(2) DEFAULT '0' COMMENT '0 pending, 1 approved, 2 rejected',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT 'audit remark',
  `audit_time` datetime DEFAULT NULL COMMENT 'audit time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_withdraw_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='withdraw order ticket';

-- ----------------------------
-- Table structure for invite_rebate_order
-- ----------------------------
DROP TABLE IF EXISTS `invite_rebate_order`;
CREATE TABLE `invite_rebate_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `beneficiary_user_id` bigint(20) NOT NULL COMMENT 'inviter user id who receives rebate',
  `source_user_id` bigint(20) NOT NULL COMMENT 'downline user id who recharged',
  `recharge_order_id` bigint(20) NOT NULL COMMENT 'source recharge order id',
  `level` int(2) NOT NULL COMMENT '1 first-level, 2 second-level',
  `source_recharge_amount_cny` decimal(30,8) NOT NULL COMMENT 'source recharge amount',
  `rebate_rate` decimal(12,8) NOT NULL COMMENT 'rebate rate from sys_config',
  `rebate_amount_cny` decimal(30,8) NOT NULL COMMENT 'rebate amount',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rebate_unique` (`beneficiary_user_id`,`recharge_order_id`,`level`),
  KEY `idx_rebate_beneficiary` (`beneficiary_user_id`,`id`),
  KEY `idx_rebate_source` (`source_user_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='invite rebate records';

-- ----------------------------
-- Table structure for mining_coin
-- ----------------------------
DROP TABLE IF EXISTS `mining_coin`;
CREATE TABLE `mining_coin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `symbol` varchar(16) NOT NULL COMMENT '币种符号 BTC, LTC',
  `name` varchar(64) NOT NULL COMMENT '全称 Bitcoin',
  `logo` varchar(512) DEFAULT NULL COMMENT 'coin logo url',
  `algorithm` varchar(32) NOT NULL COMMENT '算法 SHA256d',
  `pool_hashrate` varchar(32) DEFAULT '0' COMMENT '矿池算力',
  `network_hashrate` varchar(32) DEFAULT '0' COMMENT '全网算力',
  `price_cny` decimal(20,8) DEFAULT '0.00000000' COMMENT '人民币价格',
  `daily_revenue_per_p` decimal(20,8) DEFAULT '0.00000000' COMMENT '每日每P收益',
  `status` int(2) DEFAULT '1' COMMENT '状态 1上架 0下架',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol` (`symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='币种配置表';

-- ----------------------------
-- Table structure for mining_worker
-- ----------------------------
DROP TABLE IF EXISTS `mining_worker`;
CREATE TABLE `mining_worker` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '归属用户ID',
  `worker_name` varchar(64) NOT NULL COMMENT '矿工名 username.001',
  `coin_symbol` varchar(16) NOT NULL COMMENT '挖矿币种',
  `status` int(2) DEFAULT '0' COMMENT '状态 1在线 0离线',
  `hashrate` decimal(20,4) DEFAULT '0.0000' COMMENT '当前算力',
  `reject_rate` decimal(10,4) DEFAULT '0.0000' COMMENT '拒绝率',
  `last_share_time` datetime DEFAULT NULL COMMENT '最后提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_coin` (`user_id`, `coin_symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='矿工状态表';

-- ----------------------------
-- Table structure for mining_hashrate_record
-- ----------------------------
DROP TABLE IF EXISTS `mining_hashrate_record`;
CREATE TABLE `mining_hashrate_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `coin_symbol` varchar(16) NOT NULL,
  `hashrate` decimal(20,4) DEFAULT '0.0000' COMMENT '记录时刻的总算力',
  `record_time` datetime NOT NULL COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算力历史记录表(用于画图)';

-- ----------------------------
-- Table structure for finance_account
-- ----------------------------
DROP TABLE IF EXISTS `finance_account`;
CREATE TABLE `finance_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `coin_symbol` varchar(16) NOT NULL,
  `balance` decimal(30,18) DEFAULT '0.000000000000000000' COMMENT '余额',
  `total_revenue` decimal(30,18) DEFAULT '0.000000000000000000' COMMENT '总收入',
  `total_paid` decimal(30,18) DEFAULT '0.000000000000000000' COMMENT '总支出',
  `wallet_address` varchar(128) DEFAULT NULL COMMENT '提币地址',
  `min_payout` decimal(20,8) DEFAULT '0.01000000' COMMENT '起付额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coin` (`user_id`, `coin_symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资产账户表';

-- ----------------------------
-- Table structure for finance_bill
-- ----------------------------
DROP TABLE IF EXISTS `finance_bill`;
CREATE TABLE `finance_bill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `coin_symbol` varchar(16) NOT NULL,
  `type` int(2) NOT NULL COMMENT '1:挖矿收益 2:支付提现',
  `amount` decimal(30,18) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `tx_id` varchar(128) DEFAULT NULL COMMENT '链上交易哈希',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

-- ----------------------------
-- Table structure for mining_farm
-- ----------------------------
DROP TABLE IF EXISTS `mining_farm`;
CREATE TABLE `mining_farm` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '矿场名称',
  `location` varchar(128) NOT NULL COMMENT '地理位置',
  `electricity_price` varchar(64) DEFAULT NULL COMMENT '电价说明',
  `capacity` varchar(64) DEFAULT NULL COMMENT '容量',
  `contact` varchar(64) DEFAULT NULL COMMENT '联系人',
  `images` text COMMENT '图片JSON',
  `status` int(2) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全球矿场表';

-- Insert initial mock data for coins
INSERT INTO `mining_coin` (`symbol`, `name`, `algorithm`, `pool_hashrate`, `network_hashrate`, `price_cny`, `daily_revenue_per_p`) VALUES
('BTC', 'Bitcoin', 'SHA256d', '115.47 EH/s', '1017.05 EH/s', 454691.41, 200.00),
('LTC', 'Litecoin', 'Scrypt', '927.28 TH/s', '2.83 PH/s', 365.92, 3170.00),
('ETHW', 'EthereumPoW', 'Ethash', '907.20 GH/s', '4.89 TH/s', 2.08, 5.30);

-- Insert default system config for admin registration
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('admin_register_invite_code', 'ADMIN2026', 'invite code required for sys_user registration'),
('recharge_usdt_trc20_address', 'TRC20_DEMO_ADDRESS', 'system recharge address'),
('recharge_usdt_erc20_address', 'ERC20_DEMO_ADDRESS', 'system recharge address'),
('recharge_usdc_trc20_address', 'TRC20_DEMO_ADDRESS', 'system recharge address'),
('recharge_usdc_erc20_address', 'ERC20_DEMO_ADDRESS', 'system recharge address'),
('invite_rebate_level1_rate', '0.05000000', 'first-level invite rebate rate for approved recharge'),
('invite_rebate_level2_rate', '0.02000000', 'second-level invite rebate rate for approved recharge');
