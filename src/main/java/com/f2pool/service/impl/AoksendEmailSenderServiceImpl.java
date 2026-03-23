package com.f2pool.service.impl;

import com.alibaba.fastjson.JSON;
import com.f2pool.common.ApiException;
import com.f2pool.service.EmailSenderService;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class AoksendEmailSenderServiceImpl implements EmailSenderService {

    private static final String API_URL = "https://www.aoksend.com/index/api/send_email";

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Value("${app.email.aoksend.app-key:}")
    private String appKey;

    @Value("${app.email.aoksend.template-id:}")
    private String templateId;

    @Value("${app.email.aoksend.reply-to:}")
    private String replyTo;

    @Value("${app.email.aoksend.alias:}")
    private String alias;

    @Override
    public void sendVerificationCode(String to, String code, String scene) {
        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(templateId)) {
            throw ApiException.badRequest("邮箱服务未配置");
        }
        if (!StringUtils.hasText(to)) {
            throw ApiException.badRequest("邮箱不能为空");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("code", code);

        FormBody.Builder builder = new FormBody.Builder()
                .add("app_key", appKey.trim())
                .add("template_id", templateId.trim())
                .add("to", to.trim())
                .add("data", JSON.toJSONString(data));

        if (StringUtils.hasText(replyTo)) {
            builder.add("reply_to", replyTo.trim());
        }
        if (StringUtils.hasText(alias)) {
            builder.add("alias", alias.trim());
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .post(builder.build())
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw ApiException.badRequest("邮件发送失败");
            }
            Map<?, ?> parsed = JSON.parseObject(body, Map.class);
            Object codeValue = parsed == null ? null : parsed.get("code");
            if (codeValue != null && !"200".equals(String.valueOf(codeValue)) && !"1".equals(String.valueOf(codeValue))) {
                throw ApiException.badRequest("邮件发送失败");
            }
        } catch (IOException e) {
            throw ApiException.badRequest("邮件发送失败");
        }
    }
}
