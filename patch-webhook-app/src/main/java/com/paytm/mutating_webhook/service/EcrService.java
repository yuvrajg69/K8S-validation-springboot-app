package com.paytm.mutating_webhook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

@Service
public class EcrService {

    private static final Logger logger = LoggerFactory.getLogger(EcrService.class);

    private final EcrClient ecrClient;

    public EcrService() {
        // Create credentials provider using your named profile  ** uncomment if using profile credentials **
//        ProfileCredentialsProvider provider = ProfileCredentialsProvider
//                .builder()
//                .profileName("recon-prod-yuv")
//                .build();

        // Initialize ECR client with region and credentials
        this.ecrClient = EcrClient.builder()
                .region(Region.AP_SOUTH_1)
//                .credentialsProvider(provider)
                .build();
    }

    public boolean doesImageExist(String imageFullName) {
        try {
            String[] parts = imageFullName.split("/");
            String repoNameWithTag = parts[parts.length - 1];
            String repoName = repoNameWithTag.split(":")[0];
            String tag = repoNameWithTag.contains(":") ? repoNameWithTag.split(":")[1] : "latest";

            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .repositoryName(repoName)
                    .imageIds(ImageIdentifier.builder().imageTag(tag).build())
                    .build();

            ecrClient.describeImages(request); // Throws exception if image not found
            logger.info("Found image in ECR: {}", imageFullName);
            return true;

        } catch (EcrException e) {
            logger.warn("Image not found in ECR: {}", imageFullName);
            return false;
        }
    }
}