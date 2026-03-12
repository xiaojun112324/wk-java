package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.dto.auth.UpdateLoginPasswordRequest;
import com.f2pool.dto.auth.UpdateWithdrawPasswordRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.IUserAuthService;
import com.f2pool.service.UserFeatureRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserAuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserFeatureRestrictionService userFeatureRestrictionService;

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        long usernameExists = count(new QueryWrapper<SysUser>().eq("username", username));
        if (usernameExists > 0) {
            throw ApiException.conflict("username already exists");
        }

        long emailExists = count(new QueryWrapper<SysUser>().eq("email", email));
        if (emailExists > 0) {
            throw ApiException.conflict("email already exists");
        }

        Long inviterId = null;
        if (StringUtils.hasText(request.getInviteCode())) {
            SysUser inviter = getOne(new QueryWrapper<SysUser>().eq("invite_code", request.getInviteCode()));
            if (inviter == null) {
                throw new IllegalArgumentException("邀请码不存在");
            }
            inviterId = inviter.getId();
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setInviteCode(generateInviteCode());
        user.setInviterId(inviterId);
        user.setStatus(1);
        save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "USER"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        String account = request.getAccount().trim();

        SysUser user = getOne(
                new QueryWrapper<SysUser>()
                        .nested(wrapper -> wrapper.eq("username", account).or().eq("email", account))
        );

        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw ApiException.forbidden("账号已禁用");
        }
        userFeatureRestrictionService.assertLoginAllowed(user.getId());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("密码错误");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "USER"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> updateLoginPassword(Long userId, UpdateLoginPasswordRequest request) {
        SysUser user = requireActiveUser(userId);
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getOldPassword())) {
            throw new IllegalArgumentException("旧密码不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        if (!passwordEncoder.matches(request.getOldPassword().trim(), user.getPassword())) {
            throw ApiException.badRequest("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("updated", true);
        return result;
    }

    @Override
    public Map<String, Object> updateWithdrawPassword(Long userId, UpdateWithdrawPasswordRequest request) {
        SysUser user = requireActiveUser(userId);
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        boolean hasOld = StringUtils.hasText(user.getWithdrawPassword());
        if (hasOld) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new IllegalArgumentException("旧密码不能为空");
            }
            if (!passwordEncoder.matches(request.getOldPassword().trim(), user.getWithdrawPassword())) {
                throw ApiException.badRequest("旧资金密码错误");
            }
        }

        user.setWithdrawPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("updated", true);
        result.put("hasWithdrawPassword", true);
        return result;
    }

    @Override
    public Map<String, Object> getWithdrawPasswordStatus(Long userId) {
        SysUser user = requireActiveUser(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("hasWithdrawPassword", StringUtils.hasText(user.getWithdrawPassword()));
        return result;
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("inviteCode", user.getInviteCode());
        info.put("inviterId", user.getInviterId());
        info.put("status", user.getStatus());
        info.put("hasWithdrawPassword", StringUtils.hasText(user.getWithdrawPassword()));
        return info;
    }

    private SysUser requireActiveUser(Long userId) {
        if (userId == null) {
            throw ApiException.unauthorized("无效令牌：缺少用户标识");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw ApiException.forbidden("账号已禁用");
        }
        return user;
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (count(new QueryWrapper<SysUser>().eq("invite_code", code)) > 0);
        return code;
    }
}
