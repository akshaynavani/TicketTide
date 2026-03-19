package com.tickettide.event.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

@Component
public class S3ImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorage.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    public S3ImageStorage(S3Client s3, S3Presigner presigner,
                          @Value("${tickettide.s3.bucket}") String bucket,
                          @Value("${tickettide.s3.presign-ttl:PT1H}") Duration presignTtl) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.presignTtl = presignTtl;
    }

    @Override
    public String put(String key, InputStream content, long length, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl("public, max-age=86400")
                        .build(),
                RequestBody.fromInputStream(content, length));
        return key;
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            // Orphaned objects are harmless; a bucket lifecycle rule can sweep them.
            log.warn("Failed to delete s3://{}/{}: {}", bucket, key, e.getMessage());
        }
    }

    @Override
    public Optional<String> publicUrl(String key) {
        if (key == null) {
            return Optional.empty();
        }
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return Optional.of(presigner.presignGetObject(request).url().toString());
    }
}
