package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.EmailSenderService;
import com.f2pool.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "f2pool:auth:email-code:";
    private static final String SEND_LOCK_KEY_PREFIX = "f2pool:auth:email-send-lock:";
    private static final long CODE_TTL_MINUTES = 10;
    private static final long SEND_LOCK_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private EmailSenderService emailSenderService;

    @Override
    public void sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        long emailExists = sysUserMapper.selectCount(new QueryWrapper<SysUser>().eq("email", normalizedEmail));
        if (emailExists > 0) {
            throw ApiException.conflict("邮箱已存在");
        }
        sendCode("register", normalizedEmail, normalizedEmail);
    }

    @Override
    public void sendResetPasswordCode(String account) {
        SysUser user = requireUserByAccount(account);
        sendCode("reset-password", normalizeEmail(user.getEmail()), buildResetSubjectKey(account));
    }

    @Override
    public void verifyRegisterCode(String email, String code) {
        verifyCode("register", normalizeEmail(email), normalizeCode(code));
    }

    @Override
    public SysUser verifyResetPasswordCode(String account, String code) {
        SysUser user = requireUserByAccount(account);
        verifyCode("reset-password", buildResetSubjectKey(account), normalizeCode(code));
        return user;
    }

    private void sendCode(String scene, String targetEmail, String subjectKey) {
        String lockKey = SEND_LOCK_KEY_PREFIX + scene + ":" + subjectKey;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", SEND_LOCK_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw ApiException.badRequest("验证码发送过于频繁，请稍后再试");
        }

        String code = generateCode();
        String codeKey = buildCodeKey(scene, subjectKey);
        try {
            stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
            emailSenderService.sendVerificationCode(targetEmail, code, scene);
        } catch (RuntimeException ex) {
            stringRedisTemplate.delete(lockKey);
            stringRedisTemplate.delete(codeKey);
            throw ex;
        }
    }

    private void verifyCode(String scene, String subjectKey, String code) {
        if (!StringUtils.hasText(code)) {
            throw ApiException.badRequest("邮箱验证码不能为空");
        }
        String redisKey = buildCodeKey(scene, subjectKey);
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(cachedCode)) {
            throw ApiException.badRequest("验证码已过期");
        }
        if (!cachedCode.equals(code)) {
            throw ApiException.badRequest("验证码错误");
        }
        stringRedisTemplate.delete(redisKey);
    }

    private SysUser requireUserByAccount(String account) {
        if (!StringUtils.hasText(account)) {
            throw ApiException.badRequest("账号或邮箱不能为空");
        }
        String normalized = account.trim();
        String normalizedEmail = normalized.toLowerCase(Locale.ROOT);
        SysUser user = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>()
                        .nested(wrapper -> wrapper.eq("username", normalized).or().eq("email", normalizedEmail))
        );
        if (user == null) {
            throw ApiException.notFound("没有这个用户或邮箱");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw ApiException.badRequest("该用户未绑定邮箱");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw ApiException.badRequest("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    private String buildCodeKey(String scene, String subjectKey) {
        return CODE_KEY_PREFIX + scene + ":" + subjectKey;
    }

    private String buildResetSubjectKey(String account) {
        if (!StringUtils.hasText(account)) {
            throw ApiException.badRequest("账号或邮箱不能为空");
        }
        return account.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}
