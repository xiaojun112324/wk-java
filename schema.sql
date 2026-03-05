-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `status` int(2) DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- Table structure for mining_coin
-- ----------------------------
DROP TABLE IF EXISTS `mining_coin`;
CREATE TABLE `mining_coin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `symbol` varchar(16) NOT NULL COMMENT '币种符号 BTC, LTC',
  `name` varchar(64) NOT NULL COMMENT '全称 Bitcoin',
  `algorithm` varchar(32) NOT NULL COMMENT '算法 SHA256d',
  `pool_hashrate` varchar(32) DEFAULT '0' COMMENT '矿池算力',
  `network_hashrate` varchar(32) DEFAULT '0' COMMENT '全网算力',
  `price_cny` decimal(20,8) DEFAULT '0.00000000' COMMENT '人民币价格',
  `daily_revenue_per_t` decimal(20,8) DEFAULT '0.00000000' COMMENT '每日每T收益',
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
INSERT INTO `mining_coin` (`symbol`, `name`, `algorithm`, `pool_hashrate`, `network_hashrate`, `price_cny`, `daily_revenue_per_t`) VALUES
('BTC', 'Bitcoin', 'SHA256d', '115.47 EH/s', '1017.05 EH/s', 454691.41, 0.20),
('LTC', 'Litecoin', 'Scrypt', '927.28 TH/s', '2.83 PH/s', 365.92, 3.17),
('ETHW', 'EthereumPoW', 'Ethash', '907.20 GH/s', '4.89 TH/s', 2.08, 0.0053);
