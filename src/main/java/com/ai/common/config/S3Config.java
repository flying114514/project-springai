package com.ai.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * 这是S3客户端的配置类
 * 我们现在用的S3客户端时RustFS,作用类似于阿里云OSS,用来上传和下载数据
 * RustFS中也是有bucket的,用来存储数据
 *
 * @RequiredArgsConstructor注解用来自动生成构造函数 我们需要注意@NoArgsConstructor生成无参构造函数
 * @RequiredArgsConstructor注解会生成一个包含所有final字段的构造函数
 * @AllArgsConstructor生成包含所有字段的构造函数
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {
    private final StorageConfigProperties storageConfig;

    @Bean
    public S3Client s3Client() {
        // 创建访问凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageConfig.getAccessKey(),
                storageConfig.getSecretKey() //这里从刚才配置的StorageConfigProperties中获取密钥
        );
        // 构建并返回 S3 客户端
        return S3Client.builder()
                .endpointOverride(URI.create(storageConfig.getEndpoint()))
                .region(Region.of(storageConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                /**
                 * 因为我们没有使用到虚拟主机,所以要想访问bucket,需要设置forcePathStyle为true,就会以下面这样访问
                 * http://192.168.88.140:9000/interview-guide/files/test.pdf
                 * 否则就是下面这样:导致DNS解析错误
                 * http://interview-guide.192.168.88.140:9000/files/test.pdf
                 */
                .forcePathStyle(true) // 使用路径风格访问bucket
                .build();
    }
}
