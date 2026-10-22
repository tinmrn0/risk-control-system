package com.riskcontrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Redis Lua 脚本 Bean：classpath 加载，避免代码内拼接脚本字符串 */
@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> frequencyWindowScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/frequency_window.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
