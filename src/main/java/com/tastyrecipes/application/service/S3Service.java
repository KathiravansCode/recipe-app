package com.tastyrecipes.application.service;



import com.tastyrecipes.application.exception.ImageUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    private final S3Client s3Client;

    public S3Service(@Value("${aws.region}") String awsRegion) {
        this.s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    void normalizeConfig() {
        if (bucketName != null) bucketName = bucketName.trim();
        if (region != null) region = region.trim();

        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("Missing required config: aws.s3.bucket-name (or AWS_S3_BUCKET)");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("Missing required config: aws.region (or AWS_REGION)");
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        try {
            String safeName = sanitizeFilename(file.getOriginalFilename());
            String key = "images/" + UUID.randomUUID() + "_" + safeName;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Return full public S3 URL — this is what gets stored in the database
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        } catch (SdkException e) {
            throw new ImageUploadException("S3 upload failed. Check AWS credentials, bucket name, and bucket permissions.", e);
        } catch (RuntimeException e) {
            throw new ImageUploadException("Image upload failed.", e);
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            int idx = imageUrl.indexOf("images/");
            if (idx < 0) return;
            String key = imageUrl.substring(idx);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            System.err.println("Failed to delete S3 image: " + e.getMessage());
        }
    }

    private static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }
        String baseName = Paths.get(originalFilename).getFileName().toString().trim();
        if (baseName.isEmpty()) {
            return "image";
        }
        String cleaned = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "image" : cleaned;
    }
}
