package com.lucky.quota.user.kb.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class DocumentStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public DocumentStorageService(
            MinioClient minioClient,
            @Value("${kb.minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String upload(Long documentId, MultipartFile file) {
        String originalName = safeFileName(file.getOriginalFilename());
        String objectName = "documents/" + documentId + "/" + UUID.randomUUID() + "-" + originalName;
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception exception) {
            throw new IllegalStateException("文件上传到 MinIO 失败", exception);
        }
    }

    public void delete(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("删除 MinIO 文件失败", exception);
        }
    }

    public String bucket() {
        return bucket;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        String normalized = fileName.replace("\\", "/");
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
