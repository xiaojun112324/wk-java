package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.dto.banner.BannerSaveRequest;
import com.f2pool.entity.Banner;
import com.f2pool.service.IBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "管理端轮播图接口")
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private IBannerService bannerService;

    @Value("${app.image-domain:https://api.kuaiyi.info}")
    private String imageDomain;

    @ApiOperation("新增轮播图")
    @PostMapping
    public R<Banner> create(@RequestBody BannerSaveRequest request) {
        return R.ok(withImageUrl(bannerService.create(request)));
    }

    @ApiOperation("修改轮播图")
    @PutMapping("/{id}")
    public R<Banner> update(@PathVariable Long id, @RequestBody BannerSaveRequest request) {
        return R.ok(withImageUrl(bannerService.update(id, request)));
    }

    @ApiOperation("删除轮播图")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(bannerService.removeById(id));
    }

    @ApiOperation("轮播图详情")
    @GetMapping("/{id}")
    public R<Banner> detail(@PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            throw ApiException.notFound("轮播图不存在");
        }
        return R.ok(withImageUrl(banner));
    }

    @ApiOperation("轮播图列表")
    @GetMapping("/list")
    public R<List<Banner>> list() {
        List<Banner> list = bannerService.list(new QueryWrapper<Banner>().orderByDesc("id"));
        return R.ok(withImageUrl(list));
    }

    private List<Banner> withImageUrl(List<Banner> source) {
        List<Banner> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (Banner banner : source) {
            result.add(withImageUrl(banner));
        }
        return result;
    }

    private Banner withImageUrl(Banner source) {
        if (source == null) {
            return null;
        }
        Banner target = new Banner();
        target.setId(source.getId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setImage(toFullUrl(source.getImage()));
        return target;
    }

    private String toFullUrl(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String domain = imageDomain == null ? "" : imageDomain.trim();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + (path.startsWith("/") ? path : "/" + path);
    }
}
