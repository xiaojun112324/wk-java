package com.f2pool.service;

import java.util.List;
import java.util.Map;

public interface IUserFavoriteCoinService {
    Map<String, Object> addFavorite(Long userId, String symbol);

    Map<String, Object> removeFavorite(Long userId, String symbol);

    Map<String, Object> checkFavorite(Long userId, String symbol);

    List<Map<String, Object>> listFavorites(Long userId);
}
