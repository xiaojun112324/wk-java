package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.UserFavoriteCoin;
import com.f2pool.mapper.UserFavoriteCoinMapper;
import com.f2pool.service.IMiningCoinService;
import com.f2pool.service.IUserFavoriteCoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserFavoriteCoinServiceImpl implements IUserFavoriteCoinService {

    @Autowired
    private UserFavoriteCoinMapper userFavoriteCoinMapper;
    @Autowired
    private IMiningCoinService miningCoinService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addFavorite(Long userId, String symbol) {
        String safeSymbol = normalizeSymbol(symbol);
        assertCoinExists(safeSymbol);

        UserFavoriteCoin existed = userFavoriteCoinMapper.selectOne(new QueryWrapper<UserFavoriteCoin>()
                .eq("user_id", userId)
                .eq("coin_symbol", safeSymbol)
                .last("limit 1"));
        if (existed == null) {
            UserFavoriteCoin favorite = new UserFavoriteCoin();
            favorite.setUserId(userId);
            favorite.setCoinSymbol(safeSymbol);
            userFavoriteCoinMapper.insert(favorite);
        }
        return checkFavorite(userId, safeSymbol);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeFavorite(Long userId, String symbol) {
        String safeSymbol = normalizeSymbol(symbol);
        userFavoriteCoinMapper.delete(new QueryWrapper<UserFavoriteCoin>()
                .eq("user_id", userId)
                .eq("coin_symbol", safeSymbol));
        return checkFavorite(userId, safeSymbol);
    }

    @Override
    public Map<String, Object> checkFavorite(Long userId, String symbol) {
        String safeSymbol = normalizeSymbol(symbol);
        long count = userFavoriteCoinMapper.selectCount(new QueryWrapper<UserFavoriteCoin>()
                .eq("user_id", userId)
                .eq("coin_symbol", safeSymbol));
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", safeSymbol);
        map.put("favorite", count > 0);
        return map;
    }

    @Override
    public List<Map<String, Object>> listFavorites(Long userId) {
        List<UserFavoriteCoin> favorites = userFavoriteCoinMapper.selectList(new QueryWrapper<UserFavoriteCoin>()
                .eq("user_id", userId)
                .orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserFavoriteCoin favorite : favorites) {
            MiningCoin coin = miningCoinService.getCoinDetail(null, favorite.getCoinSymbol());
            if (coin == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", coin.getId());
            row.put("symbol", coin.getSymbol());
            row.put("name", coin.getName());
            row.put("logo", coin.getLogo());
            row.put("algorithm", coin.getAlgorithm());
            row.put("poolHashrate", coin.getPoolHashrate());
            row.put("networkHashrate", coin.getNetworkHashrate());
            row.put("priceCny", coin.getPriceCny());
            row.put("dailyRevenuePerP", coin.getDailyRevenuePerP());
            row.put("status", coin.getStatus());
            row.put("priceChange24h", coin.getPriceChange24h());
            row.put("high24h", coin.getHigh24h());
            row.put("low24h", coin.getLow24h());
            row.put("favorite", true);
            row.put("favoriteTime", favorite.getCreateTime());
            result.add(row);
        }
        return result;
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw ApiException.badRequest("币种标识不能为空");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private void assertCoinExists(String symbol) {
        MiningCoin coin = miningCoinService.getCoinDetail(null, symbol);
        if (coin == null) {
            throw ApiException.notFound("币种不存在: " + symbol);
        }
    }
}
