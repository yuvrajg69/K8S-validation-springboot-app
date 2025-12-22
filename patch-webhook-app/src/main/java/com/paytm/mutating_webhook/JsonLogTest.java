package com.paytm.mutating_webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JsonLogTest {

	public static void main(String[] args) {

		SpringApplication.run(com.paytm.mutating_webhook.JsonLogTest.class, args);
//		final String usage = """
//            Usage:    <repositoryName>
//
//            Where:
//               repositoryName - The name of the Amazon ECR repository.
//            """;
//
//		if (args.length != 1) {
//			System.out.println(usage);
//			System.exit(1);
//		}

//		String repoName = args[0];
//		ProfileCredentialsProvider provider = ProfileCredentialsProvider
//				.builder()
//				.profileName("recon-prod-yuv")
//				.build();
//
//		EcrClient ecrClient = EcrClient.builder()
//				.region(Region.AP_SOUTH_1)
//				.credentialsProvider(provider)
//				.build();
//		S3Client s3Client = S3Client.builder()
//						.region()
//		System.out.println(ecrClient.describeRepositories());


//		System.out.println(ecrClient.describeRepositories());


//		listImageTags(ecrClient,"refund-consumer");
	}

//	public static void listImageTags(EcrClient ecrClient, String repoName){
//		ListImagesRequest listImagesPaginator = ListImagesRequest.builder()
//				.repositoryName(repoName)
//				.build();
//
//		ListImagesIterable imagesIterable = ecrClient.listImagesPaginator(listImagesPaginator);
//		imagesIterable.stream()
//				.flatMap(r -> r.imageIds().stream())
//				.forEach(image -> System.out.println("The docker image tag is: " +image.imageTag()));
//	}

}
