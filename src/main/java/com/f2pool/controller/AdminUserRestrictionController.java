package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.admin.AdminUserRestrictionUpsertRequest;
import com.f2pool.entity.UserFeatureRestriction;
import com.f2pool.mapper.UserFeatureRestrictionMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "管理后台用户功能限制")
@RestController
@RequestMapping("/api/admin/user/restriction")
public class AdminUserRestrictionController {
    @Autowired
    private UserFeatureRestrictionMapper userFeatureRestrictionMapper;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("限制列表")
    @GetMapping("/list")
    public R<List<UserFeatureRestriction>> list(@RequestHeader("Authorization") String authorization,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Integer status) {
        tokenContextUtil.requireAdminId(authorization);
        QueryWrapper<UserFeatureRestriction> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("id");
        return R.ok(userFeatureRestrictionMapper.selectList(wrapper));
    }

    @ApiOperation("按用户查询当前生效限制")
    @GetMapping("/active")
    public R<UserFeatureRestriction> active(@RequestHeader("Authorization") String authorization,
                                            @RequestParam Long userId) {
        tokenContextUtil.requireAdminId(authorization);
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserFeatureRestriction row = userFeatureRestrictionMapper.selectOne(
                new QueryWrapper<UserFeatureRestriction>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .orderByDesc("id")
                        .last("limit 1")
        );
        return R.ok(row);
    }

    @ApiOperation("新增或更新限制")
    @PostMapping("/upsert")
    public R<UserFeatureRestriction> upsert(@RequestHeader("Authorization") String authorization,
                                            @RequestBody AdminUserRestrictionUpsertRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserFeatureRestriction row = userFeatureRestrictionMapper.selectOne(
                new QueryWrapper<UserFeatureRestriction>()
                        .eq("user_id", request.getUserId())
                        .orderByDesc("id")
                        .last("limit 1")
        );
        if (row == null) {
            row = new UserFeatureRestriction();
            row.setUserId(request.getUserId());
            row.setDisableLogin(0);
            row.setDisableRecharge(0);
            row.setDisableWithdraw(0);
            row.setDisableOrder(0);
            row.setDisableSellRecover(0);
            row.setDisableRevenueWithdraw(0);
            row.setDisableChatSend(0);
            row.setStatus(1);
            fill(row, request);
            userFeatureRestrictionMapper.insert(row);
        } else {
            fill(row, request);
            userFeatureRestrictionMapper.updateById(row);
        }
        return R.ok(row);
    }

    @ApiOperation("删除限制记录")
    @PostMapping("/delete/{id}")
    public R<Map<String, Object>> delete(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        tokenContextUtil.requireAdminId(authorization);
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        UserFeatureRestriction row = userFeatureRestrictionMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("id is required");
        }
        row.setStatus(0);
        userFeatureRestrictionMapper.updateById(row);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("deleted", true);
        return R.ok(data);
    }

    private void fill(UserFeatureRestriction row, AdminUserRestrictionUpsertRequest request) {
        if (request.getDisableLogin() != null) row.setDisableLogin(normalizeBinary(request.getDisableLogin()));
        if (request.getDisableRecharge() != null) row.setDisableRecharge(normalizeBinary(request.getDisableRecharge()));
        if (request.getDisableWithdraw() != null) row.setDisableWithdraw(normalizeBinary(request.getDisableWithdraw()));
        if (request.getDisableOrder() != null) row.setDisableOrder(normalizeBinary(request.getDisableOrder()));
        if (request.getDisableSellRecover() != null) row.setDisableSellRecover(normalizeBinary(request.getDisableSellRecover()));
        if (request.getDisableRevenueWithdraw() != null) row.setDisableRevenueWithdraw(normalizeBinary(request.getDisableRevenueWithdraw()));
        if (request.getDisableChatSend() != null) row.setDisableChatSend(normalizeBinary(request.getDisableChatSend()));
        if (request.getStatus() != null) row.setStatus(normalizeBinary(request.getStatus()));
        if (request.getRemark() != null) row.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
    }

    private int normalizeBinary(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }
}
