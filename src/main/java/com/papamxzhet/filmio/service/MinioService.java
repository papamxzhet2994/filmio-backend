package com.papamxzhet.filmio.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${yandex.storage.bucket}")
    private String bucketName;

    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

                System.out.println("Бакет '" + bucketName + "' создан успешно");
            } else {
                System.out.println("Бакет '" + bucketName + "' уже существует");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации бакета: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String uploadFile(MultipartFile file) throws IOException,
            MinioException,
            NoSuchAlgorithmException,
            InvalidKeyException {
        String fileKey = generateFileKey(Objects.requireNonNull(file.getOriginalFilename()));

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return fileKey;
    }

    public String getPresignedUrl(String fileKey) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .method(Method.GET)
                        .build()
        );
    }

    public void deleteFile(String fileKey) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .build()
        );
    }

    public List<Map<String, String>> listImages() throws Exception {
        List<Map<String, String>> imagesList = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix("images/")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String fileKey = item.objectName();
            String fileUrl = getPresignedUrl(fileKey);

            imagesList.add(Map.of(
                    "fileKey", fileKey,
                    "fileUrl", fileUrl
            ));
        }

        return imagesList;
    }

    private String generateFileKey(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return "images/" + uuid + extension;
    }
}