package com.riskcontrol.config;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 接口鉴权：校验 X-Api-Key 请求头，密钥走环境变量 RISK_API_KEY。
 * 未配置密钥时 fail-closed，拒绝所有请求。
 */
@Slf4j
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-Api-Key";

    @Value("${risk.api-key:}")
    private String apiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("未配置 RISK_API_KEY，拒绝访问（fail-closed）");
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return true;
    }
}
