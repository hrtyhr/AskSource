package com.itheima.aiupweb.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiAvailability {

    public static final String UNCONFIGURED_KEY = "not-configured";

    private final AiProperties properties;
    private final String apiKey;

    public AiAvailability(AiProperties properties,
                          @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.properties = properties;
        this.apiKey = apiKey;
    }

    public boolean isAvailable() {
        return properties.isEnabled()
                && StringUtils.hasText(apiKey)
                && !UNCONFIGURED_KEY.equals(apiKey.trim());
    }

    public String statusMessage() {
        if (!properties.isEnabled()) {
            return "AI 功能已由配置关闭";
        }
        if (!StringUtils.hasText(apiKey) || UNCONFIGURED_KEY.equals(apiKey.trim())) {
            return "未配置 DASHSCOPE_API_KEY，AI 功能暂不可用";
        }
        return "AI 服务已配置";
    }
}
