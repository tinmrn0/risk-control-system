package com.riskcontrol.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskcontrol.entity.RiskNameList;
import com.riskcontrol.mapper.RiskNameListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 名单缓存：value=1 在名单（TTL 5min），value=0 不在（负缓存 TTL 1min），
 * 未命中缓存回源 DB 后回填。名单变更时由 NameListService 主动失效。
 */
@Component
@RequiredArgsConstructor
public class NameListCache {

    private static final String KEY_TEMPLATE = "risk:nl:%s:%s:%s";
    private static final Duration POSITIVE_TTL = Duration.ofMinutes(5);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final RiskNameListMapper nameListMapper;

    /** 是否命中名单（仅统计 ENABLED 状态） */
    public boolean hit(String listType, String bizType, String bizValue) {
        String key = String.format(KEY_TEMPLATE, listType, bizType, bizValue);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return "1".equals(cached);
        }
        Long count = nameListMapper.selectCount(new LambdaQueryWrapper<RiskNameList>()
                .eq(RiskNameList::getListType, listType)
                .eq(RiskNameList::getBizType, bizType)
                .eq(RiskNameList::getBizValue, bizValue)
                .eq(RiskNameList::getStatus, "ENABLED"));
        boolean inList = count != null && count > 0;
        redisTemplate.opsForValue().set(key, inList ? "1" : "0", inList ? POSITIVE_TTL : NEGATIVE_TTL);
        return inList;
    }

    /** 名单增删改后失效对应缓存 */
    public void evict(String listType, String bizType, String bizValue) {
        redisTemplate.delete(String.format(KEY_TEMPLATE, listType, bizType, bizValue));
    }
}
