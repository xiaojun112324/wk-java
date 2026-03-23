package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.dto.auth.ResetPasswordByEmailRequest;
import com.f2pool.dto.auth.SendEmailCodeRequest;
import com.f2pool.dto.auth.UpdateLoginPasswordRequest;
import com.f2pool.dto.auth.UpdateWithdrawPasswordRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.EmailVerificationService;
import com.f2pool.service.IUserAuthService;
import com.f2pool.service.UserFeatureRestrictionService;
import com.f2pool.util.VisibleIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserAuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserFeatureRestrictionService userFeatureRestrictionService;
    @Autowired
    private VisibleIdGenerator visibleIdGenerator;
    @Autowired
    private EmailVerificationService emailVerificationService;

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw ApiException.badRequest("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw ApiException.badRequest("邮箱不能为空");
        }
        if (!StringUtils.hasText(request.getEmailCode())) {
            throw ApiException.badRequest("邮箱验证码不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw ApiException.badRequest("密码不能为空");
        }
        if (request.getPassword().trim().length() < 6) {
            throw ApiException.badRequest("密码长度不能少于6位");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        emailVerificationService.verifyRegisterCode(email, request.getEmailCode());

        long usernameExists = count(new QueryWrapper<SysUser>().eq("username", username));
        if (usernameExists > 0) {
            throw ApiException.conflict("用户名已存在");
        }

        long emailExists = count(new QueryWrapper<SysUser>().eq("email", email));
        if (emailExists > 0) {
            throw ApiException.conflict("邮箱已存在");
        }

        Long inviterId = null;
        if (StringUtils.hasText(request.getInviteCode())) {
            SysUser inviter = getOne(new QueryWrapper<SysUser>().eq("invite_code", request.getInviteCode().trim()));
            if (inviter == null) {
                throw ApiException.badRequest("邀请码不存在");
            }
            inviterId = inviter.getId();
        }

        SysUser user = new SysUser();
        user.setId(visibleIdGenerator.nextId("user"));
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setInviteCode(generateInviteCode());
        user.setInviterId(inviterId);
        user.setStatus(1);
        save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "USER"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds("USER"));
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw ApiException.badRequest("账号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw ApiException.badRequest("密码不能为空");
        }

        String account = request.getAccount().trim();
        String normalizedEmail = account.toLowerCase(Locale.ROOT);
        SysUser user = getOne(
                new QueryWrapper<SysUser>()
                        .nested(wrapper -> wrapper.eq("username", account).or().eq("email", normalizedEmail))
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
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds("USER"));
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> sendEmailCode(SendEmailCodeRequest request) {
        if (request == null || !StringUtils.hasText(request.getScene())) {
            throw ApiException.badRequest("发送场景不能为空");
        }
        String scene = request.getScene().trim();
        if ("register".equalsIgnoreCase(scene)) {
            emailVerificationService.sendRegisterCode(request.getEmail());
        } else if ("reset-password".equalsIgnoreCase(scene)) {
            emailVerificationService.sendResetPasswordCode(request.getAccount());
        } else {
            throw ApiException.badRequest("不支持的发送场景");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        return result;
    }

    @Override
    public Map<String, Object> resetPasswordByEmail(ResetPasswordByEmailRequest request) {
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw ApiException.badRequest("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw ApiException.badRequest("新密码长度不能少于6位");
        }

        SysUser user = emailVerificationService.verifyResetPasswordCode(request.getAccount(), request.getEmailCode());
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("updated", true);
        return result;
    }

    @Override
    public Map<String, Object> updateLoginPassword(Long userId, UpdateLoginPasswordRequest request) {
        SysUser user = requireActiveUser(userId);
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getEmailCode())) {
            throw ApiException.badRequest("邮箱验证码不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw ApiException.badRequest("新密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw ApiException.badRequest("新密码长度不能少于6位");
        }

        emailVerificationService.verifyResetPasswordCode(user.getEmail(), request.getEmailCode());
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
            throw ApiException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getEmailCode())) {
            throw ApiException.badRequest("邮箱验证码不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw ApiException.badRequest("密码不能为空");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw ApiException.badRequest("密码长度不能少于6位");
        }

        emailVerificationService.verifyResetPasswordCode(user.getEmail(), request.getEmailCode());
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
            throw ApiException.unauthorized("无效令牌");
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
