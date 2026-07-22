package com.gcoedu.core.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioService(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void init() {
        List<String> bucketsToCreate = Arrays.asList(
                bucket, // Bucket padrão das propriedades
                "answer-sheets",
                "physical-tests",
                "municipality-logos",
                "school-logos",
                "question-images",
                "user-uploads"
        );

        for (String b : bucketsToCreate) {
            try {
                boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(b).build());
                if (!isExist) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(b).build());
                    System.out.println("✅ Bucket '" + b + "' criado com sucesso no MinIO.");
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao verificar/criar o bucket '" + b + "': " + e.getMessage());
            }
        }
    }

    public void uploadFile(String objectName, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }
}
