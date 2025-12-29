package server.poptato.infra.s3.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final int IMAGE_URL_PREFIX_LENGTH = 41;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 30;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile multipartFile, String groupName) {
        String fileName = createFileNameWithGroup(multipartFile.getOriginalFilename(), groupName);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata));
            return amazonS3.getUrl(bucket, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getEmojiImageUrl(String groupName, String fileName) {
        String fullPath = groupName + "-" + fileName;
        return amazonS3.getUrl(bucket, fullPath).toString();
    }
    public List<String> getUrlsByGroupName(String groupName) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(groupName + "-");
        ListObjectsV2Result result = amazonS3.listObjectsV2(request);

        List<String> urls = new ArrayList<>();
        for (S3ObjectSummary summary : result.getObjectSummaries()) {
            String url = amazonS3.getUrl(bucket, summary.getKey()).toString();
            urls.add(url);
        }
        return urls;
    }

    public void deleteS3Image(final String imageUrl) {
        final String imageKey = getImageUrlToKey(imageUrl);
        amazonS3.deleteObject(bucket, imageKey);
    }

    public String extractGroupNameFromUrl(String imageUrl) {
        String key = getImageUrlToKey(imageUrl);
        int firstDash = key.indexOf('-');
        if (firstDash != -1) {
            return key.substring(0, firstDash);
        }
        return null;
    }

    private String createFileNameWithGroup(String fileName, String groupName) {
        return groupName + "-" + UUID.randomUUID() + getFileExtension(fileName);
    }

    private String getImageUrlToKey(final String imageUrl) {
        return imageUrl.substring(IMAGE_URL_PREFIX_LENGTH + bucket.length());
    }

    private String getFileExtension(String fileName) {
        if (fileName.isEmpty()) throw new IllegalArgumentException("File name cannot be empty");

        List<String> validExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG");
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        if (!validExtensions.contains(fileExtension)) throw new IllegalArgumentException("Invalid file extension");

        return fileExtension;
    }

    public String generatePresignedUrl(String filePath) {
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + PRESIGNED_URL_EXPIRATION_MINUTES * 60 * 1000);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, filePath)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = amazonS3.generatePresignedUrl(request);
        return url.toString();
    }
}
