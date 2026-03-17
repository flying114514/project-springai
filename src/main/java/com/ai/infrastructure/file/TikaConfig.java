package com.ai.infrastructure.file;

import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Tika 配置类
 */
@Configuration
public class TikaConfig {

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
