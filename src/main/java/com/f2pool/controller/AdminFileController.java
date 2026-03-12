package com.f2pool.controller;

import com.f2pool.common.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Api(tags = "管理端文件接口")
@RestController
@RequestMapping("/api/admin/file")
public class AdminFileController {

    private static final String UPLOAD_DIR = "/www/wwwroot/file";
    private static final String WEB_FILE_PREFIX = "/file/";

    @Value("${app.image-domain:https://api.kuaiyi.info}")
    private String imageDomain;

    @ApiOperation("上传文件到 /www/wwwroot/file")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = WEB_FILE_PREFIX + fileName;
        String fullUrl = buildFullUrl(relativePath);

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", fileName);
        data.put("path", target.toString().replace("\\", "/"));
        data.put("relativePath", relativePath);
        data.put("image", relativePath);
        data.put("url", fullUrl);
        return R.ok(data);
    }

    private String buildFullUrl(String path) {
        String domain = imageDomain == null ? "" : imageDomain.trim();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (path == null || path.isEmpty()) {
            return domain;
        }
        return domain + (path.startsWith("/") ? path : "/" + path);
    }
}
