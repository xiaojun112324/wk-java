package com.f2pool.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.dto.banner.BannerSaveRequest;
import com.f2pool.entity.Banner;
import com.f2pool.mapper.BannerMapper;
import com.f2pool.service.IBannerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements IBannerService {

    @Override
    public Banner create(BannerSaveRequest request) {
        validateRequest(request);
        Banner banner = new Banner();
        banner.setImage(normalizeImagePath(request.getImage()));
        save(banner);
        return banner;
    }

    @Override
    public Banner update(Long id, BannerSaveRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        validateRequest(request);
        Banner banner = getById(id);
        if (banner == null) {
            throw new IllegalArgumentException("轮播图不存在");
        }
        banner.setImage(normalizeImagePath(request.getImage()));
        updateById(banner);
        return banner;
    }

    private void validateRequest(BannerSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("图片不能为空");
        }
    }

    private String normalizeImagePath(String raw) {
        String image = raw == null ? "" : raw.trim();

        int idx = image.indexOf("/file/");
        if (idx >= 0) {
            image = image.substring(idx);
        }

        image = image.replace("\\", "/");
        image = image.replace("/www/wwwroot", "");

        if (image.startsWith("file/")) {
            image = "/" + image;
        }

        return image;
    }
}
