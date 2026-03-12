package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.admin.AdminResetPasswordRequest;
import com.f2pool.dto.admin.AdminUserStatusUpdateRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.entity.UserFeatureRestriction;
import com.f2pool.entity.UserWallet;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.mapper.UserFeatureRestrictionMapper;
import com.f2pool.mapper.UserWalletMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "管理后台用户管理")
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private UserWalletMapper userWalletMapper;
    @Autowired
    private UserFeatureRestrictionMapper userFeatureRestrictionMapper;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("用户列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        tokenContextUtil.requireAdminId(authorization);
        int safePageNo = Math.max(pageNo == null ? 1 : pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 200);

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw).or().like("email", kw).or().like("invite_code", kw));
        }
        wrapper.orderByDesc("id");
        List<SysUser> all = sysUserMapper.selectList(wrapper);

        int from = (safePageNo - 1) * safePageSize;
        int to = Math.min(from + safePageSize, all.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < all.size()) {
            for (SysUser user : all.subList(from, to)) {
                rows.add(buildUserRow(user));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("pageNo", safePageNo);
        data.put("pageSize", safePageSize);
        data.put("list", rows);
        return R.ok(data);
    }

    @ApiOperation("修改用户状态")
    @PostMapping("/{id}/status")
    public R<Map<String, Object>> updateStatus(@RequestHeader("Authorization") String authorization,
                                               @PathVariable Long id,
                                               @RequestBody AdminUserStatusUpdateRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new IllegalArgumentException("状态必须是0或1");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
        return R.ok(buildUserRow(user));
    }

    @ApiOperation("重置登录密码")
    @PostMapping("/{id}/password/login/reset")
    public R<Map<String, Object>> resetLoginPassword(@RequestHeader("Authorization") String authorization,
                                                      @PathVariable Long id,
                                                      @RequestBody AdminResetPasswordRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        sysUserMapper.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", id);
        data.put("updated", true);
        return R.ok(data);
    }

    @ApiOperation("重置资金密码")
    @PostMapping("/{id}/password/fund/reset")
    public R<Map<String, Object>> resetFundPassword(@RequestHeader("Authorization") String authorization,
                                                     @PathVariable Long id,
                                                     @RequestBody AdminResetPasswordRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setWithdrawPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        sysUserMapper.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", id);
        data.put("updated", true);
        return R.ok(data);
    }

    private Map<String, Object> buildUserRow(SysUser user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("inviteCode", user.getInviteCode());
        row.put("inviterId", user.getInviterId());
        row.put("status", user.getStatus());
        row.put("hasFundPassword", StringUtils.hasText(user.getWithdrawPassword()));
        row.put("createTime", user.getCreateTime());

        UserWallet wallet = userWalletMapper.selectOne(new QueryWrapper<UserWallet>().eq("user_id", user.getId()).last("limit 1"));
        if (wallet == null) {
            row.put("usdtBalance", BigDecimal.ZERO);
            row.put("usdcBalance", BigDecimal.ZERO);
            row.put("btcBalance", BigDecimal.ZERO);
        } else {
            row.put("usdtBalance", safe(wallet.getUsdtBalance()));
            row.put("usdcBalance", safe(wallet.getUsdcBalance()));
            row.put("btcBalance", safe(wallet.getBtcBalance()));
        }

        UserFeatureRestriction restriction = userFeatureRestrictionMapper.selectOne(
                new QueryWrapper<UserFeatureRestriction>()
                        .eq("user_id", user.getId())
                        .eq("status", 1)
                        .orderByDesc("id")
                        .last("limit 1")
        );
        row.put("restriction", restriction);
        return row;
    }

    private BigDecimal safe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
