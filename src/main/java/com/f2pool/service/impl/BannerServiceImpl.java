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
        banner.setImage(request.getImage().trim());
        save(banner);
        return banner;
    }

    @Override
    public Banner update(Long id, BannerSaveRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        validateRequest(request);
        Banner banner = getById(id);
        if (banner == null) {
            throw new IllegalArgumentException("banner not found");
        }
        banner.setImage(request.getImage().trim());
        updateById(banner);
        return banner;
    }

    private void validateRequest(BannerSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("image is required");
        }
    }
}
