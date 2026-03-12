package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.admin.AdminAssistOrderCreateRequest;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderBuyByPRequest;
import com.f2pool.dto.machine.UserMachineRevenueWithdrawRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.service.IUserMachineOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "管理后台订单管理")
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {
    @Autowired
    private IUserMachineOrderService userMachineOrderService;
    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("订单列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestHeader("Authorization") String authorization,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") Integer pageNo,
                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        tokenContextUtil.requireAdminId(authorization);
        int safePageNo = Math.max(pageNo == null ? 1 : pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 200);

        QueryWrapper<UserMachineOrder> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("coin_symbol", kw).or().like("machine_name", kw).or().like("receive_address", kw));
        }
        wrapper.orderByDesc("id");
        List<UserMachineOrder> all = userMachineOrderMapper.selectList(wrapper);

        int from = (safePageNo - 1) * safePageSize;
        int to = Math.min(from + safePageSize, all.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < all.size()) {
            for (UserMachineOrder order : all.subList(from, to)) {
                rows.add(buildOrderRow(order));
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("pageNo", safePageNo);
        data.put("pageSize", safePageSize);
        data.put("list", rows);
        return R.ok(data);
    }

    @ApiOperation("订单详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@RequestHeader("Authorization") String authorization,
                                         @PathVariable Long id) {
        tokenContextUtil.requireAdminId(authorization);
        return R.ok(userMachineOrderService.detail(id));
    }

    @ApiOperation("后台代用户下单")
    @PostMapping("/assist-buy")
    public R<Map<String, Object>> assistBuy(@RequestHeader("Authorization") String authorization,
                                            @RequestBody AdminAssistOrderCreateRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        UserMachineOrderBuyByPRequest buy = new UserMachineOrderBuyByPRequest();
        buy.setUserId(request.getUserId());
        buy.setCoinSymbol(StringUtils.hasText(request.getCoinSymbol()) ? request.getCoinSymbol().trim().toUpperCase() : "BTC");
        buy.setPCount(request.getPCount());
        buy.setTotalAmountUsd(request.getTotalAmountUsd());
        buy.setUsdtPay(request.getUsdtPay());
        buy.setUsdcPay(request.getUsdcPay());
        buy.setReceiveAddress(request.getReceiveAddress());
        return R.ok(userMachineOrderService.createOrderByP(buy));
    }

    @ApiOperation("后台回收算力")
    @PostMapping("/{id}/recover")
    public R<Map<String, Object>> recover(@RequestHeader("Authorization") String authorization,
                                          @PathVariable Long id) {
        tokenContextUtil.requireAdminId(authorization);
        UserMachineOrder order = userMachineOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        UserMachineOrderActionRequest request = new UserMachineOrderActionRequest();
        request.setUserId(order.getUserId());
        return R.ok(userMachineOrderService.sell(id, request));
    }

    @ApiOperation("后台提取单个订单收益")
    @PostMapping("/{id}/withdraw-revenue")
    public R<Map<String, Object>> withdrawRevenue(@RequestHeader("Authorization") String authorization,
                                                  @PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        tokenContextUtil.requireAdminId(authorization);
        UserMachineOrder order = userMachineOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        UserMachineRevenueWithdrawRequest request = new UserMachineRevenueWithdrawRequest();
        request.setUserId(order.getUserId());
        if (body != null && body.get("receiveAddress") != null) {
            request.setReceiveAddress(String.valueOf(body.get("receiveAddress")));
        }
        return R.ok(userMachineOrderService.withdrawRevenue(id, request));
    }

    private Map<String, Object> buildOrderRow(UserMachineOrder order) {
        Map<String, Object> map = userMachineOrderService.detail(order.getId());
        SysUser user = sysUserMapper.selectById(order.getUserId());
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
        }
        return map;
    }
}
