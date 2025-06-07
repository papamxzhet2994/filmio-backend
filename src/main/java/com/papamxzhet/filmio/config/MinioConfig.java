package com.papamxzhet.filmio.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${yandex.storage.endpoint}")
    private String endpoint;

    @Value("${yandex.storage.accessKey}")
    private String accessKey;

    @Value("${yandex.storage.secretKey}")
    private String secretKey;

    @Value("${yandex.storage.region}")
    private String region;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}