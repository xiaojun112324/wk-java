package com.f2pool.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashrateUnitUtil {

    private static final Map<String, BigDecimal> UNIT_TO_TH_FACTOR;

    static {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("H", new BigDecimal("0.000000000001"));
        map.put("KH", new BigDecimal("0.000000001"));
        map.put("MH", new BigDecimal("0.000001"));
        map.put("GH", new BigDecimal("0.001"));
        map.put("TH", new BigDecimal("1"));
        map.put("PH", new BigDecimal("1000"));
        map.put("EH", new BigDecimal("1000000"));
        UNIT_TO_TH_FACTOR = Collections.unmodifiableMap(map);
    }

    private HashrateUnitUtil() {
    }

    public static boolean isSupportedUnit(String unit) {
        return unit != null && UNIT_TO_TH_FACTOR.containsKey(unit.toUpperCase());
    }

    public static BigDecimal toTH(BigDecimal value, String unit) {
        if (value == null) {
            throw new IllegalArgumentException("算力值不能为空");
        }
        if (!isSupportedUnit(unit)) {
            throw new IllegalArgumentException("不支持的算力单位：" + unit);
        }
        return value.multiply(UNIT_TO_TH_FACTOR.get(unit.toUpperCase()));
    }

    public static List<String> supportedUnits() {
        return new ArrayList<>(UNIT_TO_TH_FACTOR.keySet());
    }
}
