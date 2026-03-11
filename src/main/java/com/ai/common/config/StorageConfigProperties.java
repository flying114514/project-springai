package com.ai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置RustFS存储配置类,实现类型安全的配置绑定
 * @ConfigurationProperties(prefix = "app.storage")
 * 上面这个注解表示读取配置文件中所有以app.storage开头的属性,并赋给这个类
 */
@Data
@Component // 将这个类注册为Spring Bean
@ConfigurationProperties(prefix = "app.storage")
public class StorageConfigProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "us-east-1";
}
