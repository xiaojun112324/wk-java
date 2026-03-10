package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.RechargeOrder;
import com.f2pool.entity.SysUser;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.entity.UserWallet;
import com.f2pool.entity.WithdrawOrder;
import com.f2pool.mapper.MiningCoinMapper;
import com.f2pool.mapper.RechargeOrderMapper;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.mapper.UserWalletMapper;
import com.f2pool.mapper.WithdrawOrderMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Api(tags = "管理后台仪表盘接口")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal DEFAULT_USD_CNY = new BigDecimal("7.20");

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    @Autowired
    private WithdrawOrderMapper withdrawOrderMapper;
    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private UserWalletMapper userWalletMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MiningCoinMapper miningCoinMapper;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("资金总览")
    @GetMapping("/finance/overview")
    public R<Map<String, Object>> financeOverview(@RequestHeader("Authorization") String authorization) {
        tokenContextUtil.requireAdminId(authorization);
        BigDecimal totalRecharge = sumRechargeByStatus(1, null, null);
        BigDecimal totalWithdraw = sumWithdrawByStatus(1, null, null);
        BigDecimal netInflow = totalRecharge.subtract(totalWithdraw).setScale(8, RoundingMode.HALF_UP);

        Date todayStart = dayStart(LocalDate.now(CN_ZONE));
        Date todayEnd = dayEnd(LocalDate.now(CN_ZONE));
        BigDecimal todayRecharge = sumRechargeByStatus(1, todayStart, todayEnd);
        BigDecimal todayWithdraw = sumWithdrawByStatus(1, todayStart, todayEnd);

        Long pendingRechargeCount = rechargeOrderMapper.selectCount(new QueryWrapper<RechargeOrder>().eq("status", 0));
        Long pendingWithdrawCount = withdrawOrderMapper.selectCount(new QueryWrapper<WithdrawOrder>().eq("status", 0));
        Long totalHoldingOrders = userMachineOrderMapper.selectCount(new QueryWrapper<UserMachineOrder>().eq("status", 1));

        Map<String, Object> data = new HashMap<>();
        data.put("totalRecharge", totalRecharge);
        data.put("totalWithdraw", totalWithdraw);
        data.put("netInflow", netInflow);
        data.put("todayRecharge", todayRecharge);
        data.put("todayWithdraw", todayWithdraw);
        data.put("todayNetInflow", todayRecharge.subtract(todayWithdraw).setScale(8, RoundingMode.HALF_UP));
        data.put("pendingRechargeCount", safeLong(pendingRechargeCount));
        data.put("pendingWithdrawCount", safeLong(pendingWithdrawCount));
        data.put("holdingOrderCount", safeLong(totalHoldingOrders));
        return R.ok(data);
    }

    @ApiOperation("入金/出金趋势")
    @GetMapping("/finance/trend")
    public R<List<Map<String, Object>>> financeTrend(@RequestHeader("Authorization") String authorization,
                                                     @RequestParam(defaultValue = "15") Integer days) {
        tokenContextUtil.requireAdminId(authorization);
        int safeDays = (days == null || days <= 0) ? 15 : Math.min(days, 90);
        LocalDate end = LocalDate.now(CN_ZONE);
        LocalDate start = end.minusDays(safeDays - 1L);

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Date dayStart = dayStart(d);
            Date dayEnd = dayEnd(d);
            BigDecimal recharge = sumRechargeByStatus(1, dayStart, dayEnd);
            BigDecimal withdraw = sumWithdrawByStatus(1, dayStart, dayEnd);

            Map<String, Object> row = new HashMap<>();
            row.put("date", d.toString());
            row.put("recharge", recharge);
            row.put("withdraw", withdraw);
            row.put("netInflow", recharge.subtract(withdraw).setScale(8, RoundingMode.HALF_UP));
            result.add(row);
        }
        return R.ok(result);
    }

    @ApiOperation("用户资产指标")
    @GetMapping("/user/metrics")
    public R<Map<String, Object>> userMetrics(@RequestHeader("Authorization") String authorization) {
        tokenContextUtil.requireAdminId(authorization);
        List<UserWallet> wallets = userWalletMapper.selectList(new QueryWrapper<UserWallet>().orderByDesc("id"));
        List<RechargeOrder> approvedRecharge = rechargeOrderMapper.selectList(new QueryWrapper<RechargeOrder>().eq("status", 1));

        BigDecimal usdCny = DEFAULT_USD_CNY;
        BigDecimal btcCny = getBtcCnyPrice();
        BigDecimal totalAssetCny = BigDecimal.ZERO;
        for (UserWallet wallet : wallets) {
            totalAssetCny = totalAssetCny.add(calcWalletAssetCny(wallet, usdCny, btcCny));
        }

        Set<Long> rechargeUsers = approvedRecharge.stream().map(RechargeOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        BigDecimal totalRecharge = approvedRecharge.stream().map(RechargeOrder::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(8, RoundingMode.HALF_UP);

        int walletUserCount = wallets.size();
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", safeLong(sysUserMapper.selectCount(new QueryWrapper<SysUser>())));
        data.put("walletUserCount", walletUserCount);
        data.put("rechargeUserCount", rechargeUsers.size());
        data.put("avgRechargePerUser", rechargeUsers.isEmpty() ? BigDecimal.ZERO :
                totalRecharge.divide(new BigDecimal(rechargeUsers.size()), 8, RoundingMode.HALF_UP));
        data.put("avgAssetPerUserCny", walletUserCount == 0 ? BigDecimal.ZERO :
                totalAssetCny.divide(new BigDecimal(walletUserCount), 8, RoundingMode.HALF_UP));
        data.put("totalAssetCny", totalAssetCny.setScale(8, RoundingMode.HALF_UP));
        data.put("usdCny", usdCny);
        data.put("btcCny", btcCny);
        return R.ok(data);
    }

    @ApiOperation("用户总资产排行")
    @GetMapping("/user/asset-ranking")
    public R<List<Map<String, Object>>> assetRanking(@RequestHeader("Authorization") String authorization,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        tokenContextUtil.requireAdminId(authorization);
        int topN = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        BigDecimal usdCny = DEFAULT_USD_CNY;
        BigDecimal btcCny = getBtcCnyPrice();

        List<UserWallet> wallets = userWalletMapper.selectList(new QueryWrapper<UserWallet>().orderByDesc("id"));
        Map<Long, SysUser> userMap = sysUserMapper.selectList(new QueryWrapper<SysUser>())
                .stream().collect(Collectors.toMap(SysUser::getId, x -> x, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserWallet wallet : wallets) {
            if (wallet.getUserId() == null) continue;
            SysUser user = userMap.get(wallet.getUserId());
            BigDecimal assetCny = calcWalletAssetCny(wallet, usdCny, btcCny).setScale(8, RoundingMode.HALF_UP);
            Map<String, Object> row = new HashMap<>();
            row.put("userId", wallet.getUserId());
            row.put("username", user == null ? "" : user.getUsername());
            row.put("email", user == null ? "" : user.getEmail());
            row.put("usdtBalance", safe(wallet.getUsdtBalance()));
            row.put("usdcBalance", safe(wallet.getUsdcBalance()));
            row.put("btcBalance", safe(wallet.getBtcBalance()));
            row.put("machineBalanceUsdt", getHoldingInvestUsdt(wallet.getUserId()));
            row.put("totalAssetCny", assetCny);
            rows.add(row);
        }

        rows.sort(Comparator.comparing((Map<String, Object> m) ->
                (BigDecimal) m.getOrDefault("totalAssetCny", BigDecimal.ZERO)).reversed());
        if (rows.size() > topN) {
            rows = rows.subList(0, topN);
        }
        return R.ok(rows);
    }

    private BigDecimal calcWalletAssetCny(UserWallet wallet, BigDecimal usdCny, BigDecimal btcCny) {
        BigDecimal usdt = safe(wallet.getUsdtBalance()).multiply(usdCny);
        BigDecimal usdc = safe(wallet.getUsdcBalance()).multiply(usdCny);
        BigDecimal btc = safe(wallet.getBtcBalance()).multiply(btcCny);
        BigDecimal machine = getHoldingInvestUsdt(wallet.getUserId()).multiply(usdCny);
        return usdt.add(usdc).add(btc).add(machine);
    }

    private BigDecimal getHoldingInvestUsdt(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        List<UserMachineOrder> list = userMachineOrderMapper.selectList(
                new QueryWrapper<UserMachineOrder>().eq("user_id", userId).eq("status", 1)
        );
        BigDecimal total = BigDecimal.ZERO;
        for (UserMachineOrder order : list) {
            total = total.add(safe(order.getTotalInvest()));
        }
        return total.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal getBtcCnyPrice() {
        MiningCoin btc = miningCoinMapper.selectOne(new QueryWrapper<MiningCoin>()
                .eq("symbol", "BTC")
                .eq("status", 1)
                .last("limit 1"));
        if (btc != null && btc.getPriceCny() != null && btc.getPriceCny().compareTo(BigDecimal.ZERO) > 0) {
            return btc.getPriceCny();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal sumRechargeByStatus(Integer status, Date begin, Date end) {
        QueryWrapper<RechargeOrder> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        if (begin != null) {
            qw.ge("create_time", begin);
        }
        if (end != null) {
            qw.le("create_time", end);
        }
        List<RechargeOrder> rows = rechargeOrderMapper.selectList(qw);
        return rows.stream()
                .map(RechargeOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sumWithdrawByStatus(Integer status, Date begin, Date end) {
        QueryWrapper<WithdrawOrder> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        if (begin != null) {
            qw.ge("create_time", begin);
        }
        if (end != null) {
            qw.le("create_time", end);
        }
        List<WithdrawOrder> rows = withdrawOrderMapper.selectList(qw);
        return rows.stream()
                .map(WithdrawOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private Date dayStart(LocalDate day) {
        return Date.from(day.atStartOfDay(CN_ZONE).toInstant());
    }

    private Date dayEnd(LocalDate day) {
        return Date.from(day.plusDays(1).atStartOfDay(CN_ZONE).minusNanos(1).toInstant());
    }

    private long safeLong(Long n) {
        return n == null ? 0L : n;
    }

    private BigDecimal safe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}

