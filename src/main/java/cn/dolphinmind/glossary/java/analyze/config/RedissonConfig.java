package cn.dolphinmind.glossary.java.analyze.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置
 * 
 * 参考自 S-PAY-MALL 架构，提供分布式锁、延迟队列、对象存储等高级功能。
 */
@Configuration
@ConfigurationProperties(prefix = "redisson")
public class RedissonConfig {

    private String address = "redis://localhost:16379";
    private String password;
    private int connectionPoolSize = 64;
    private int connectionMinimumIdleSize = 10;
    private int idleConnectionTimeout = 10000;
    private int connectTimeout = 10000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(JsonJacksonCodec.INSTANCE);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(password)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setConnectTimeout(connectTimeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setKeepAlive(true);

        return Redisson.create(config);
    }

    // Setters for ConfigurationProperties
    public void setAddress(String address) { this.address = address; }
    public void setPassword(String password) { this.password = password; }
    public void setConnectionPoolSize(int connectionPoolSize) { this.connectionPoolSize = connectionPoolSize; }
    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) { this.connectionMinimumIdleSize = connectionMinimumIdleSize; }
    public void setIdleConnectionTimeout(int idleConnectionTimeout) { this.idleConnectionTimeout = idleConnectionTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
    public void setRetryInterval(int retryInterval) { this.retryInterval = retryInterval; }
}
