package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.banner.BannerSaveRequest;
import com.f2pool.entity.Banner;

public interface IBannerService extends IService<Banner> {
    Banner create(BannerSaveRequest request);

    Banner update(Long id, BannerSaveRequest request);
}
