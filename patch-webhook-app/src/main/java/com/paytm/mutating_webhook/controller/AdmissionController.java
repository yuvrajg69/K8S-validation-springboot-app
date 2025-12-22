package com.paytm.mutating_webhook.controller;

import com.paytm.mutating_webhook.model.AdmissionReviewRequest;
import com.paytm.mutating_webhook.model.AdmissionReviewResponse;
import com.paytm.mutating_webhook.service.EcrService;
import com.paytm.mutating_webhook.model.Container;

import com.paytm.mutating_webhook.service.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/validate")
public class AdmissionController {

    private static final Logger logger = LoggerFactory.getLogger(AdmissionController.class);
    private final EcrService ecrService;
    private final KubernetesService kubernetesService;

    public AdmissionController(EcrService ecrService, KubernetesService kubernetesService) {
        this.ecrService = ecrService;
        this.kubernetesService=kubernetesService;
    }

    @PostMapping
    public ResponseEntity<AdmissionReviewResponse> validate(@RequestBody AdmissionReviewRequest reviewRequest) {
        var req = reviewRequest.getRequest();
        var pod = req.getObject();

        logger.info("Validating pod: {}", pod.getMetadata().getName());

        // Collect all containers
        List<Container> allContainers = new ArrayList<>();
        if (pod.getSpec().getContainers() != null)
            allContainers.addAll(pod.getSpec().getContainers());
        if (pod.getSpec().getInitContainers() != null)
            allContainers.addAll(pod.getSpec().getInitContainers());
        if (pod.getSpec().getEphemeralContainers() != null)
            allContainers.addAll(pod.getSpec().getEphemeralContainers());

        // Check ECR existence
        boolean allExist = true;
        for (Container container : allContainers) {
            String image = container.getImage();
            logger.info("Checking image in ECR: {}", image);
            if (!ecrService.doesImageExist(image)) {
                allExist = false;
                break;
            }
        }
// Scale top-level controller to 0 only if all images exist
        if (!allExist) {
            try {
                // CHANGED: Use the new method with pod object instead of pod name
                kubernetesService.scaleRolloutToZero(
                        pod.getMetadata().getNamespace(),
                        pod  // Pass the entire pod object, not just the name
                );
                logger.info("Attempted to scale Argo Rollout to 0 replicas");
            } catch (Exception e) {
                logger.error("Failed to scale Argo Rollout to 0", e);
            }
        }

        // Build response
        AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
        AdmissionReviewResponse.AdmissionResponse response = new AdmissionReviewResponse.AdmissionResponse();
        response.setUid(req.getUid());
        response.setAllowed(allExist);

        AdmissionReviewResponse.AdmissionResponse.Status status = new AdmissionReviewResponse.AdmissionResponse.Status();
        status.setMessage(allExist ? "All images verified in ECR" : "One or more images not found in ECR");

        response.setStatus(status);
        reviewResponse.setResponse(response);

        return ResponseEntity.ok(reviewResponse);

    }
}