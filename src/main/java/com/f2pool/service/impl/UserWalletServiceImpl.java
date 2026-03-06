package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.f2pool.mapper.RechargeOrderMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.mapper.UserWalletMapper;
import com.f2pool.mapper.WithdrawOrderMapper;
import com.f2pool.service.IUserWalletService;
import org.springframework.beans.factory.annotation.Autowired;
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
        map.put("USDC_TRC20", getConfig("recharge_usdc_trc20_address"));
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
        order.setAmountCny(request.getAmountCny());
        order.setVoucherImage(request.getVoucherImage().trim());
        order.setStatus(0);
        rechargeOrderMapper.insert(order);
        return buildRecharge(order);
    }

    @Override
    @Transactional
    public Map<String, Object> submitWithdraw(WithdrawSubmitRequest request) {
        validateWithdrawRequest(request);
        UserWallet wallet = getOrCreateWallet(request.getUserId());
        if (wallet.getBalanceCny().compareTo(request.getAmountCny()) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        wallet.setBalanceCny(wallet.getBalanceCny().subtract(request.getAmountCny()));
        wallet.setFreezeCny(wallet.getFreezeCny().add(request.getAmountCny()));
        userWalletMapper.updateById(wallet);

        WithdrawOrder order = new WithdrawOrder();
        order.setUserId(request.getUserId());
        order.setAsset(request.getAsset().trim().toUpperCase());
        order.setNetwork(request.getNetwork().trim().toUpperCase());
        order.setAmountCny(request.getAmountCny());
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
            wallet.setBalanceCny(wallet.getBalanceCny().add(order.getAmountCny()));
            wallet.setTotalRechargeCny(wallet.getTotalRechargeCny().add(order.getAmountCny()));
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
        if (request.getStatus() == 1) {
            wallet.setFreezeCny(wallet.getFreezeCny().subtract(order.getAmountCny()));
            wallet.setTotalWithdrawCny(wallet.getTotalWithdrawCny().add(order.getAmountCny()));
        } else {
            wallet.setFreezeCny(wallet.getFreezeCny().subtract(order.getAmountCny()));
            wallet.setBalanceCny(wallet.getBalanceCny().add(order.getAmountCny()));
        }
        userWalletMapper.updateById(wallet);
        return buildWithdraw(order);
    }

    @Override
    @Transactional
    public void decreaseBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalanceCny().compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        wallet.setBalanceCny(wallet.getBalanceCny().subtract(amount));
        userWalletMapper.updateById(wallet);
    }

    @Override
    @Transactional
    public void increaseBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.setBalanceCny(wallet.getBalanceCny().add(amount));
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
        if (request.getAmountCny() == null || request.getAmountCny().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountCny must be greater than 0");
        }
        if (!StringUtils.hasText(request.getVoucherImage())) {
            throw new IllegalArgumentException("voucherImage is required");
        }
    }

    private void validateWithdrawRequest(WithdrawSubmitRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        if (request.getUserId() == null) throw new IllegalArgumentException("userId is required");
        validateAssetNetwork(request.getAsset(), request.getNetwork());
        if (request.getAmountCny() == null || request.getAmountCny().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountCny must be greater than 0");
        }
        if (!StringUtils.hasText(request.getReceiveAddress())) {
            throw new IllegalArgumentException("receiveAddress is required");
        }
    }

    private void validateAssetNetwork(String asset, String network) {
        if (!StringUtils.hasText(asset) || !StringUtils.hasText(network)) {
            throw new IllegalArgumentException("asset and network are required");
        }
        String a = asset.trim().toUpperCase();
        String n = network.trim().toUpperCase();
        boolean assetOk = "USDT".equals(a) || "USDC".equals(a);
        boolean netOk = "TRC20".equals(n) || "ERC20".equals(n);
        if (!assetOk || !netOk) {
            throw new IllegalArgumentException("only USDT/USDC and TRC20/ERC20 are supported");
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
            if (wallet.getBalanceCny() == null) wallet.setBalanceCny(BigDecimal.ZERO);
            if (wallet.getFreezeCny() == null) wallet.setFreezeCny(BigDecimal.ZERO);
            if (wallet.getTotalRechargeCny() == null) wallet.setTotalRechargeCny(BigDecimal.ZERO);
            if (wallet.getTotalWithdrawCny() == null) wallet.setTotalWithdrawCny(BigDecimal.ZERO);
            return wallet;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalanceCny(BigDecimal.ZERO);
        wallet.setFreezeCny(BigDecimal.ZERO);
        wallet.setTotalRechargeCny(BigDecimal.ZERO);
        wallet.setTotalWithdrawCny(BigDecimal.ZERO);
        userWalletMapper.insert(wallet);
        return wallet;
    }

    private Map<String, Object> buildWallet(UserWallet wallet) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", wallet.getUserId());
        map.put("balanceCny", wallet.getBalanceCny());
        map.put("freezeCny", wallet.getFreezeCny());
        map.put("totalRechargeCny", wallet.getTotalRechargeCny());
        map.put("totalWithdrawCny", wallet.getTotalWithdrawCny());
        return map;
    }

    private Map<String, Object> buildRecharge(RechargeOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("asset", order.getAsset());
        map.put("network", order.getNetwork());
        map.put("amountCny", order.getAmountCny());
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
        map.put("amountCny", order.getAmountCny());
        map.put("receiveAddress", order.getReceiveAddress());
        map.put("status", order.getStatus());
        map.put("auditRemark", order.getAuditRemark());
        map.put("auditTime", order.getAuditTime());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private void processInviteRebate(RechargeOrder order) {
        if (order == null || order.getId() == null || order.getUserId() == null || order.getAmountCny() == null) {
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

        BigDecimal rebateAmount = order.getAmountCny()
                .multiply(rate)
                .setScale(8, RoundingMode.HALF_UP);
        if (rebateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        UserWallet beneficiaryWallet = getOrCreateWallet(beneficiaryUserId);
        beneficiaryWallet.setBalanceCny(beneficiaryWallet.getBalanceCny().add(rebateAmount));
        userWalletMapper.updateById(beneficiaryWallet);

        InviteRebateOrder rebateOrder = new InviteRebateOrder();
        rebateOrder.setBeneficiaryUserId(beneficiaryUserId);
        rebateOrder.setSourceUserId(sourceUserId);
        rebateOrder.setRechargeOrderId(order.getId());
        rebateOrder.setLevel(level);
        rebateOrder.setSourceRechargeAmountCny(order.getAmountCny());
        rebateOrder.setRebateRate(rate);
        rebateOrder.setRebateAmountCny(rebateAmount);
        inviteRebateOrderMapper.insert(rebateOrder);
    }
}
