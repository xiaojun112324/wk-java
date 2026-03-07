package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.f2pool.dto.wallet.AuditRequest;
import com.f2pool.dto.wallet.RechargeSubmitRequest;
import com.f2pool.dto.wallet.WithdrawSubmitRequest;
import com.f2pool.entity.InviteRebateOrder;
import com.f2pool.entity.RechargeOrder;
import com.f2pool.entity.SysConfig;
import com.f2pool.entity.SysUser;
import com.f2pool.entity.UserWallet;
import com.f2pool.entity.WithdrawOrder;
import com.f2pool.mapper.InviteRebateOrderMapper;
import com.f2pool.mapper.MiningCoinMapper;
import com.f2pool.mapper.RechargeOrderMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.mapper.UserWalletMapper;
import com.f2pool.mapper.WithdrawOrderMapper;
import com.f2pool.service.IUserWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserWalletServiceImpl implements IUserWalletService {
    private static final String LEVEL1_RATE_KEY = "invite_rebate_level1_rate";
    private static final String LEVEL2_RATE_KEY = "invite_rebate_level2_rate";
    private static final String CACHE_USD_CNY_KEY = "f2pool:cache:fx:usd_cny";
    private static final String CACHE_USDC_USDT_KEY = "f2pool:cache:fx:usdc_usdt";
    private static final String CACHE_MARKET_KEY = "f2pool:cache:coin:market";
    private static final BigDecimal DEFAULT_USD_CNY_RATE = new BigDecimal("7.2");
    private static final BigDecimal DEFAULT_USDC_USDT_RATE = BigDecimal.ONE;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserWalletMapper userWalletMapper;
    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    @Autowired
    private WithdrawOrderMapper withdrawOrderMapper;
    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private InviteRebateOrderMapper inviteRebateOrderMapper;
    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private MiningCoinMapper miningCoinMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Map<String, Object> getWallet(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        return buildWallet(wallet);
    }

    @Override
    public Map<String, Object> getRechargeAddressConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("USDT_TRC20", getConfig("recharge_usdt_trc20_address"));
        map.put("USDT_ERC20", getConfig("recharge_usdt_erc20_address"));
        map.put("USDC_ERC20", getConfig("recharge_usdc_erc20_address"));
        return map;
    }

    @Override
    @Transactional
    public Map<String, Object> submitRecharge(RechargeSubmitRequest request) {
        validateRechargeRequest(request);
        RechargeOrder order = new RechargeOrder();
        order.setUserId(request.getUserId());
        order.setAsset(request.getAsset().trim().toUpperCase());
        order.setNetwork(request.getNetwork().trim().toUpperCase());
        order.setAmount(resolveAmount(request.getAmount(), request.getAmountCny()));
        order.setVoucherImage(request.getVoucherImage().trim());
        order.setStatus(0);
        rechargeOrderMapper.insert(order);
        return buildRecharge(order);
    }

    @Override
    @Transactional
    public Map<String, Object> submitWithdraw(WithdrawSubmitRequest request) {
        validateWithdrawRequest(request);
        String asset = normalizeAsset(request.getAsset());
        BigDecimal amount = resolveAmount(request.getAmount(), request.getAmountCny());
        UserWallet wallet = getOrCreateWallet(request.getUserId());
        if (getBalanceByAsset(wallet, asset).compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        setBalanceByAsset(wallet, asset, getBalanceByAsset(wallet, asset).subtract(amount));
        setFreezeByAsset(wallet, asset, getFreezeByAsset(wallet, asset).add(amount));
        userWalletMapper.updateById(wallet);

        WithdrawOrder order = new WithdrawOrder();
        order.setUserId(request.getUserId());
        order.setAsset(asset);
        order.setNetwork(request.getNetwork().trim().toUpperCase());
        order.setAmount(amount);
        order.setReceiveAddress(request.getReceiveAddress().trim());
        order.setStatus(0);
        withdrawOrderMapper.insert(order);
        return buildWithdraw(order);
    }

    @Override
    public List<Map<String, Object>> listRechargeByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<RechargeOrder> list = rechargeOrderMapper.selectList(new QueryWrapper<RechargeOrder>().eq("user_id", userId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RechargeOrder item : list) {
            result.add(buildRecharge(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWithdrawByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<WithdrawOrder> list = withdrawOrderMapper.selectList(new QueryWrapper<WithdrawOrder>().eq("user_id", userId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WithdrawOrder item : list) {
            result.add(buildWithdraw(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listRechargePending() {
        List<RechargeOrder> list = rechargeOrderMapper.selectList(new QueryWrapper<RechargeOrder>().eq("status", 0).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RechargeOrder item : list) {
            result.add(buildRecharge(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWithdrawPending() {
        List<WithdrawOrder> list = withdrawOrderMapper.selectList(new QueryWrapper<WithdrawOrder>().eq("status", 0).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WithdrawOrder item : list) {
            result.add(buildWithdraw(item));
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> auditRecharge(Long id, AuditRequest request) {
        RechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("recharge order not found");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new IllegalArgumentException("recharge order already audited");
        }
        validateAuditRequest(request);
        order.setStatus(request.getStatus());
        order.setAuditRemark(request.getRemark());
        order.setAuditTime(new Date());
        rechargeOrderMapper.updateById(order);

        if (request.getStatus() == 1) {
            UserWallet wallet = getOrCreateWallet(order.getUserId());
            String asset = normalizeAsset(order.getAsset());
            setBalanceByAsset(wallet, asset, getBalanceByAsset(wallet, asset).add(order.getAmount()));
            setTotalRechargeByAsset(wallet, asset, getTotalRechargeByAsset(wallet, asset).add(order.getAmount()));
            userWalletMapper.updateById(wallet);
            processInviteRebate(order);
        }
        return buildRecharge(order);
    }

    @Override
    @Transactional
    public Map<String, Object> auditWithdraw(Long id, AuditRequest request) {
        WithdrawOrder order = withdrawOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("withdraw order not found");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new IllegalArgumentException("withdraw order already audited");
        }
        validateAuditRequest(request);
        order.setStatus(request.getStatus());
        order.setAuditRemark(request.getRemark());
        order.setAuditTime(new Date());
        withdrawOrderMapper.updateById(order);

        UserWallet wallet = getOrCreateWallet(order.getUserId());
        String asset = normalizeAsset(order.getAsset());
        if (request.getStatus() == 1) {
            setFreezeByAsset(wallet, asset, getFreezeByAsset(wallet, asset).subtract(order.getAmount()));
            setTotalWithdrawByAsset(wallet, asset, getTotalWithdrawByAsset(wallet, asset).add(order.getAmount()));
        } else {
            setFreezeByAsset(wallet, asset, getFreezeByAsset(wallet, asset).subtract(order.getAmount()));
            setBalanceByAsset(wallet, asset, getBalanceByAsset(wallet, asset).add(order.getAmount()));
        }
        userWalletMapper.updateById(wallet);
        return buildWithdraw(order);
    }

    @Override
    @Transactional
    public void decreaseBalance(Long userId, String asset, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        String normalizedAsset = normalizeAsset(asset);
        if (getBalanceByAsset(wallet, normalizedAsset).compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        setBalanceByAsset(wallet, normalizedAsset, getBalanceByAsset(wallet, normalizedAsset).subtract(amount));
        userWalletMapper.updateById(wallet);
    }

    @Override
    @Transactional
    public void increaseBalance(Long userId, String asset, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        String normalizedAsset = normalizeAsset(asset);
        setBalanceByAsset(wallet, normalizedAsset, getBalanceByAsset(wallet, normalizedAsset).add(amount));
        userWalletMapper.updateById(wallet);
    }

    @Override
    public Map<String, Object> getInviteSummary(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<SysUser> level1Users = sysUserMapper.selectList(new QueryWrapper<SysUser>().eq("inviter_id", userId));
        int level1Count = level1Users.size();
        List<Long> level1Ids = level1Users.stream().map(SysUser::getId).collect(Collectors.toList());

        int level2Count = 0;
        if (!level1Ids.isEmpty()) {
            level2Count = Math.toIntExact(sysUserMapper.selectCount(new QueryWrapper<SysUser>().in("inviter_id", level1Ids)));
        }

        List<InviteRebateOrder> rebateOrders =
                inviteRebateOrderMapper.selectList(new QueryWrapper<InviteRebateOrder>().eq("beneficiary_user_id", userId));
        BigDecimal totalRebateCny = rebateOrders.stream()
                .map(InviteRebateOrder::getRebateAmountCny)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal level1RebateTotalCny = rebateOrders.stream()
                .filter(x -> x.getLevel() != null && x.getLevel() == 1)
                .map(InviteRebateOrder::getRebateAmountCny)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal level2RebateTotalCny = rebateOrders.stream()
                .filter(x -> x.getLevel() != null && x.getLevel() == 2)
                .map(InviteRebateOrder::getRebateAmountCny)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSourceRechargeCny = rebateOrders.stream()
                .map(InviteRebateOrder::getSourceRechargeAmountCny)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("level1Count", level1Count);
        map.put("level2Count", level2Count);
        map.put("totalInviteCount", level1Count + level2Count);
        map.put("level1Rate", getConfigDecimal(LEVEL1_RATE_KEY, BigDecimal.ZERO));
        map.put("level2Rate", getConfigDecimal(LEVEL2_RATE_KEY, BigDecimal.ZERO));
        map.put("totalRebateCny", totalRebateCny);
        map.put("level1RebateTotalCny", level1RebateTotalCny);
        map.put("level2RebateTotalCny", level2RebateTotalCny);
        map.put("totalSourceRechargeCny", totalSourceRechargeCny);
        return map;
    }

    @Override
    public Map<String, Object> getInviteHierarchy(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<SysUser> level1Users = sysUserMapper.selectList(
                new QueryWrapper<SysUser>().eq("inviter_id", userId).orderByDesc("id")
        );
        List<Map<String, Object>> level1List = new ArrayList<>();
        int level2Total = 0;
        for (SysUser level1 : level1Users) {
            List<SysUser> level2Users = sysUserMapper.selectList(
                    new QueryWrapper<SysUser>().eq("inviter_id", level1.getId()).orderByDesc("id")
            );
            level2Total += level2Users.size();
            Map<String, Object> level1Map = buildInviteUser(level1);
            level1Map.put("level2Users", level2Users.stream().map(this::buildInviteUser).collect(Collectors.toList()));
            level1Map.put("level2Count", level2Users.size());
            level1List.add(level1Map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("level1Count", level1Users.size());
        result.put("level2Count", level2Total);
        result.put("totalInviteCount", level1Users.size() + level2Total);
        result.put("level1Users", level1List);
        return result;
    }

    @Override
    public List<Map<String, Object>> listInviteRebateRecords(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<InviteRebateOrder> list = inviteRebateOrderMapper.selectList(
                new QueryWrapper<InviteRebateOrder>()
                        .eq("beneficiary_user_id", userId)
                        .orderByDesc("id")
        );
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> sourceUserIds = list.stream()
                .map(InviteRebateOrder::getSourceUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!sourceUserIds.isEmpty()) {
            List<SysUser> sourceUsers = sysUserMapper.selectList(new QueryWrapper<SysUser>().in("id", sourceUserIds));
            for (SysUser user : sourceUsers) {
                userMap.put(user.getId(), user);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (InviteRebateOrder item : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("beneficiaryUserId", item.getBeneficiaryUserId());
            map.put("sourceUserId", item.getSourceUserId());
            map.put("rechargeOrderId", item.getRechargeOrderId());
            map.put("level", item.getLevel());
            map.put("sourceRechargeAmountCny", item.getSourceRechargeAmountCny());
            map.put("rebateRate", item.getRebateRate());
            map.put("rebateAmountCny", item.getRebateAmountCny());
            map.put("createTime", item.getCreateTime());
            SysUser sourceUser = userMap.get(item.getSourceUserId());
            map.put("sourceUsername", sourceUser == null ? null : sourceUser.getUsername());
            map.put("sourceEmail", sourceUser == null ? null : sourceUser.getEmail());
            result.add(map);
        }
        return result;
    }

    private void validateRechargeRequest(RechargeSubmitRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        if (request.getUserId() == null) throw new IllegalArgumentException("userId is required");
        validateAssetNetwork(request.getAsset(), request.getNetwork());
        BigDecimal amount = resolveAmount(request.getAmount(), request.getAmountCny());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (!StringUtils.hasText(request.getVoucherImage())) {
            throw new IllegalArgumentException("voucherImage is required");
        }
    }

    private void validateWithdrawRequest(WithdrawSubmitRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        if (request.getUserId() == null) throw new IllegalArgumentException("userId is required");
        validateAssetNetwork(request.getAsset(), request.getNetwork());
        BigDecimal amount = resolveAmount(request.getAmount(), request.getAmountCny());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (!StringUtils.hasText(request.getReceiveAddress())) {
            throw new IllegalArgumentException("receiveAddress is required");
        }
        if (!StringUtils.hasText(request.getWithdrawPassword())) {
            throw new IllegalArgumentException("withdrawPassword is required");
        }

        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        if (!StringUtils.hasText(user.getWithdrawPassword())) {
            throw new IllegalArgumentException("withdraw password not set, please set it in settings first");
        }
        if (!passwordEncoder.matches(request.getWithdrawPassword().trim(), user.getWithdrawPassword())) {
            throw new IllegalArgumentException("withdraw password is incorrect");
        }
    }

    private void validateAssetNetwork(String asset, String network) {
        if (!StringUtils.hasText(asset) || !StringUtils.hasText(network)) {
            throw new IllegalArgumentException("asset and network are required");
        }
        String a = asset.trim().toUpperCase();
        String n = network.trim().toUpperCase();
        boolean valid = ("USDT".equals(a) && ("TRC20".equals(n) || "ERC20".equals(n)))
                || ("USDC".equals(a) && "ERC20".equals(n))
                || ("BTC".equals(a) && "BTC".equals(n));
        if (!valid) {
            throw new IllegalArgumentException("invalid asset/network pair");
        }
    }

    private void validateAuditRequest(AuditRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            throw new IllegalArgumentException("status must be 1(approve) or 2(reject)");
        }
    }

    private String getConfig(String key) {
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key).eq("status", 1));
        return config == null ? "" : config.getConfigValue();
    }

    private BigDecimal getConfigDecimal(String key, BigDecimal defaultValue) {
        String value = getConfig(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private UserWallet getOrCreateWallet(Long userId) {
        UserWallet wallet = userWalletMapper.selectOne(new QueryWrapper<UserWallet>().eq("user_id", userId));
        if (wallet != null) {
            normalizeWallet(wallet);
            return wallet;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        normalizeWallet(wallet);
        userWalletMapper.insert(wallet);
        return wallet;
    }

    private Map<String, Object> buildWallet(UserWallet wallet) {
        BigDecimal usdtRate = getUsdCnyRateFromCache();
        BigDecimal usdcRate = getUsdcCnyRateFromCache(usdtRate);
        BigDecimal btcRate = getBtcCnyPriceFromCache();
        BigDecimal machineBalanceUsdt = getMachineBalanceUsdt(wallet.getUserId());

        BigDecimal totalAssetCny = safe(wallet.getUsdtBalance()).multiply(usdtRate)
                .add(safe(wallet.getUsdcBalance()).multiply(usdcRate))
                .add(safe(wallet.getBtcBalance()).multiply(btcRate))
                .add(machineBalanceUsdt.multiply(usdtRate))
                .setScale(8, RoundingMode.HALF_UP);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", wallet.getUserId());
        map.put("usdtBalance", wallet.getUsdtBalance());
        map.put("usdcBalance", wallet.getUsdcBalance());
        map.put("btcBalance", wallet.getBtcBalance());
        map.put("machineBalanceUsdt", machineBalanceUsdt);
        map.put("usdtFreeze", wallet.getUsdtFreeze());
        map.put("usdcFreeze", wallet.getUsdcFreeze());
        map.put("btcFreeze", wallet.getBtcFreeze());
        map.put("totalRechargeUsdt", wallet.getTotalRechargeUsdt());
        map.put("totalRechargeUsdc", wallet.getTotalRechargeUsdc());
        map.put("totalRechargeBtc", wallet.getTotalRechargeBtc());
        map.put("totalWithdrawUsdt", wallet.getTotalWithdrawUsdt());
        map.put("totalWithdrawUsdc", wallet.getTotalWithdrawUsdc());
        map.put("totalWithdrawBtc", wallet.getTotalWithdrawBtc());
        map.put("totalAssetCny", totalAssetCny);
        Map<String, Object> exchangeRates = new HashMap<>();
        exchangeRates.put("USDT_CNY", usdtRate);
        exchangeRates.put("USDC_CNY", usdcRate);
        exchangeRates.put("BTC_CNY", btcRate);
        map.put("exchangeRates", exchangeRates);
        return map;
    }

    private BigDecimal getMachineBalanceUsdt(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        List<com.f2pool.entity.UserMachineOrder> holdingOrders = userMachineOrderMapper.selectList(
                new QueryWrapper<com.f2pool.entity.UserMachineOrder>()
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
        BigDecimal sum = BigDecimal.ZERO;
        for (com.f2pool.entity.UserMachineOrder order : holdingOrders) {
            sum = sum.add(safe(order.getTotalInvest()));
        }
        return sum.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal getUsdCnyRateFromCache() {
        try {
            String value = stringRedisTemplate.opsForValue().get(CACHE_USD_CNY_KEY);
            if (StringUtils.hasText(value)) {
                BigDecimal rate = new BigDecimal(value.trim());
                if (rate.compareTo(new BigDecimal("5")) >= 0 && rate.compareTo(new BigDecimal("10")) <= 0) {
                    return rate;
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_USD_CNY_RATE;
    }

    private BigDecimal getUsdcCnyRateFromCache(BigDecimal usdtCny) {
        BigDecimal usdcUsdt = DEFAULT_USDC_USDT_RATE;
        try {
            String value = stringRedisTemplate.opsForValue().get(CACHE_USDC_USDT_KEY);
            if (StringUtils.hasText(value)) {
                BigDecimal rate = new BigDecimal(value.trim());
                if (rate.compareTo(new BigDecimal("0.5")) > 0 && rate.compareTo(new BigDecimal("1.5")) < 0) {
                    usdcUsdt = rate;
                }
            }
        } catch (Exception ignored) {
        }
        return usdtCny.multiply(usdcUsdt).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal getBtcCnyPriceFromCache() {
        try {
            String marketJson = stringRedisTemplate.opsForValue().get(CACHE_MARKET_KEY);
            if (StringUtils.hasText(marketJson)) {
                JSONObject all = JSON.parseObject(marketJson);
                if (all != null) {
                    JSONObject btc = all.getJSONObject("BTC");
                    if (btc != null) {
                        BigDecimal price = btc.getBigDecimal("priceCny");
                        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                            return price;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // DB fallback
        try {
            com.f2pool.entity.MiningCoin btcCoin = miningCoinMapper.selectOne(new QueryWrapper<com.f2pool.entity.MiningCoin>()
                    .eq("symbol", "BTC")
                    .last("limit 1"));
            if (btcCoin != null && btcCoin.getPriceCny() != null && btcCoin.getPriceCny().compareTo(BigDecimal.ZERO) > 0) {
                return btcCoin.getPriceCny();
            }
        } catch (Exception ignored) {
        }
        // Optional manual fallback from config
        SysConfig cfg = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>()
                .eq("config_key", "wallet_btc_cny_rate")
                .eq("status", 1)
                .last("limit 1"));
        if (cfg != null && StringUtils.hasText(cfg.getConfigValue())) {
            try {
                return new BigDecimal(cfg.getConfigValue().trim());
            } catch (Exception ignored) {
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> buildRecharge(RechargeOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("asset", order.getAsset());
        map.put("network", order.getNetwork());
        map.put("amount", order.getAmount());
        map.put("amountCny", order.getAmount());
        map.put("voucherImage", order.getVoucherImage());
        map.put("status", order.getStatus());
        map.put("auditRemark", order.getAuditRemark());
        map.put("auditTime", order.getAuditTime());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private Map<String, Object> buildInviteUser(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("inviteCode", user.getInviteCode());
        map.put("inviterId", user.getInviterId());
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreateTime());
        return map;
    }

    private Map<String, Object> buildWithdraw(WithdrawOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("asset", order.getAsset());
        map.put("network", order.getNetwork());
        map.put("amount", order.getAmount());
        map.put("amountCny", order.getAmount());
        map.put("receiveAddress", order.getReceiveAddress());
        map.put("status", order.getStatus());
        map.put("auditRemark", order.getAuditRemark());
        map.put("auditTime", order.getAuditTime());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private void processInviteRebate(RechargeOrder order) {
        if (order == null || order.getId() == null || order.getUserId() == null || order.getAmount() == null) {
            return;
        }
        SysUser sourceUser = sysUserMapper.selectById(order.getUserId());
        if (sourceUser == null || sourceUser.getInviterId() == null) {
            return;
        }

        BigDecimal level1Rate = getConfigDecimal(LEVEL1_RATE_KEY, BigDecimal.ZERO);
        BigDecimal level2Rate = getConfigDecimal(LEVEL2_RATE_KEY, BigDecimal.ZERO);

        Long level1UserId = sourceUser.getInviterId();
        if (level1Rate.compareTo(BigDecimal.ZERO) > 0) {
            grantInviteRebate(level1UserId, sourceUser.getId(), order, 1, level1Rate);
        }

        SysUser level1User = sysUserMapper.selectById(level1UserId);
        if (level1User != null && level1User.getInviterId() != null && level2Rate.compareTo(BigDecimal.ZERO) > 0) {
            grantInviteRebate(level1User.getInviterId(), sourceUser.getId(), order, 2, level2Rate);
        }
    }

    private void grantInviteRebate(Long beneficiaryUserId, Long sourceUserId, RechargeOrder order, int level, BigDecimal rate) {
        if (beneficiaryUserId == null || sourceUserId == null) {
            return;
        }
        Long exists = inviteRebateOrderMapper.selectCount(
                new QueryWrapper<InviteRebateOrder>()
                        .eq("beneficiary_user_id", beneficiaryUserId)
                        .eq("recharge_order_id", order.getId())
                        .eq("level", level)
        );
        if (exists != null && exists > 0) {
            return;
        }

        BigDecimal rebateAmount = order.getAmount()
                .multiply(rate)
                .setScale(8, RoundingMode.HALF_UP);
        if (rebateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String rebateAsset = "USDT".equals(normalizeAsset(order.getAsset())) ? "USDT" : "USDC";
        UserWallet beneficiaryWallet = getOrCreateWallet(beneficiaryUserId);
        setBalanceByAsset(
                beneficiaryWallet,
                rebateAsset,
                getBalanceByAsset(beneficiaryWallet, rebateAsset).add(rebateAmount)
        );
        userWalletMapper.updateById(beneficiaryWallet);

        InviteRebateOrder rebateOrder = new InviteRebateOrder();
        rebateOrder.setBeneficiaryUserId(beneficiaryUserId);
        rebateOrder.setSourceUserId(sourceUserId);
        rebateOrder.setRechargeOrderId(order.getId());
        rebateOrder.setLevel(level);
        rebateOrder.setSourceRechargeAmountCny(order.getAmount());
        rebateOrder.setRebateRate(rate);
        rebateOrder.setRebateAmountCny(rebateAmount);
        inviteRebateOrderMapper.insert(rebateOrder);
    }

    private void normalizeWallet(UserWallet wallet) {
        if (wallet.getUsdtBalance() == null) wallet.setUsdtBalance(BigDecimal.ZERO);
        if (wallet.getUsdcBalance() == null) wallet.setUsdcBalance(BigDecimal.ZERO);
        if (wallet.getBtcBalance() == null) wallet.setBtcBalance(BigDecimal.ZERO);
        if (wallet.getUsdtFreeze() == null) wallet.setUsdtFreeze(BigDecimal.ZERO);
        if (wallet.getUsdcFreeze() == null) wallet.setUsdcFreeze(BigDecimal.ZERO);
        if (wallet.getBtcFreeze() == null) wallet.setBtcFreeze(BigDecimal.ZERO);
        if (wallet.getTotalRechargeUsdt() == null) wallet.setTotalRechargeUsdt(BigDecimal.ZERO);
        if (wallet.getTotalRechargeUsdc() == null) wallet.setTotalRechargeUsdc(BigDecimal.ZERO);
        if (wallet.getTotalRechargeBtc() == null) wallet.setTotalRechargeBtc(BigDecimal.ZERO);
        if (wallet.getTotalWithdrawUsdt() == null) wallet.setTotalWithdrawUsdt(BigDecimal.ZERO);
        if (wallet.getTotalWithdrawUsdc() == null) wallet.setTotalWithdrawUsdc(BigDecimal.ZERO);
        if (wallet.getTotalWithdrawBtc() == null) wallet.setTotalWithdrawBtc(BigDecimal.ZERO);
    }

    private String normalizeAsset(String asset) {
        if (!StringUtils.hasText(asset)) {
            throw new IllegalArgumentException("asset is required");
        }
        String normalized = asset.trim().toUpperCase();
        if (!"USDT".equals(normalized) && !"USDC".equals(normalized) && !"BTC".equals(normalized)) {
            throw new IllegalArgumentException("asset must be one of USDT/USDC/BTC");
        }
        return normalized;
    }

    private BigDecimal getBalanceByAsset(UserWallet wallet, String asset) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) return wallet.getUsdtBalance();
        if ("USDC".equals(a)) return wallet.getUsdcBalance();
        return wallet.getBtcBalance();
    }

    private void setBalanceByAsset(UserWallet wallet, String asset, BigDecimal value) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) {
            wallet.setUsdtBalance(value);
        } else if ("USDC".equals(a)) {
            wallet.setUsdcBalance(value);
        } else {
            wallet.setBtcBalance(value);
        }
    }

    private BigDecimal getFreezeByAsset(UserWallet wallet, String asset) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) return wallet.getUsdtFreeze();
        if ("USDC".equals(a)) return wallet.getUsdcFreeze();
        return wallet.getBtcFreeze();
    }

    private void setFreezeByAsset(UserWallet wallet, String asset, BigDecimal value) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) {
            wallet.setUsdtFreeze(value);
        } else if ("USDC".equals(a)) {
            wallet.setUsdcFreeze(value);
        } else {
            wallet.setBtcFreeze(value);
        }
    }

    private BigDecimal getTotalRechargeByAsset(UserWallet wallet, String asset) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) return wallet.getTotalRechargeUsdt();
        if ("USDC".equals(a)) return wallet.getTotalRechargeUsdc();
        return wallet.getTotalRechargeBtc();
    }

    private void setTotalRechargeByAsset(UserWallet wallet, String asset, BigDecimal value) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) {
            wallet.setTotalRechargeUsdt(value);
        } else if ("USDC".equals(a)) {
            wallet.setTotalRechargeUsdc(value);
        } else {
            wallet.setTotalRechargeBtc(value);
        }
    }

    private BigDecimal getTotalWithdrawByAsset(UserWallet wallet, String asset) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) return wallet.getTotalWithdrawUsdt();
        if ("USDC".equals(a)) return wallet.getTotalWithdrawUsdc();
        return wallet.getTotalWithdrawBtc();
    }

    private void setTotalWithdrawByAsset(UserWallet wallet, String asset, BigDecimal value) {
        String a = normalizeAsset(asset);
        if ("USDT".equals(a)) {
            wallet.setTotalWithdrawUsdt(value);
        } else if ("USDC".equals(a)) {
            wallet.setTotalWithdrawUsdc(value);
        } else {
            wallet.setTotalWithdrawBtc(value);
        }
    }

    private BigDecimal resolveAmount(BigDecimal amount, BigDecimal amountCny) {
        return amount != null ? amount : amountCny;
    }
}
