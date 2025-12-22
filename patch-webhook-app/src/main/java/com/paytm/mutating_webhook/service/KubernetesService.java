package com.paytm.mutating_webhook.service;

import com.paytm.mutating_webhook.model.Pod;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

@Service
public class KubernetesService {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesService.class);
    private final ApiClient client;
    private final CustomObjectsApi customApi;

    public KubernetesService() throws IOException {
        this.client = Config.defaultClient();
        this.client.setConnectTimeout(30_000);
        this.client.setReadTimeout(30_000);
        this.customApi = new CustomObjectsApi(client);
    }

    /**
     * Scales an Argo Rollout's replicas to 0 based on Pod ownership.
     */
    public void scaleRolloutToZero(String namespace, Pod pod) {
        if (pod == null || pod.getMetadata() == null) {
            logger.info("Pod or metadata is null");
            return;
        }

        String podName = pod.getMetadata().getName();
        Map<String, String> labels = pod.getMetadata().getLabels();

        logger.info("Checking pod {}/{} for Argo Rollout", namespace, podName);

        if (isRolloutPod(labels)) {
            String rolloutName = getRolloutNameFromLabels(labels);
            if (rolloutName != null) {
                scaleRollout(namespace, rolloutName);
            } else {
                logger.warn("Could not determine Rollout name from labels for pod {}", podName);
            }
        } else {
            logger.debug("Pod {} is not part of a Rollout", podName);
        }
    }

    /**
     * Determines if this Pod belongs to a Rollout by checking rollout-specific labels.
     */
    private boolean isRolloutPod(Map<String, String> labels) {
        return labels != null && labels.containsKey("rollouts-pod-template-hash");
    }

    /**
     * Extracts the Rollout name from labels (prefers "app", falls back to "app.kubernetes.io/name").
     */
    private String getRolloutNameFromLabels(Map<String, String> labels) {
        if (labels == null) return null;
        if (labels.containsKey("app")) return labels.get("app");
        return labels.getOrDefault("app.kubernetes.io/name", null);
    }


    private void scaleRollout(String namespace, String rolloutName) {
        try {
            String patchJson = "{\"spec\":{\"replicas\":0}}";
            V1Patch patch = new V1Patch(patchJson);

            logger.info("Scaling Rollout {}/{} to 0 replicas", namespace, rolloutName);

            // Use PatchUtils for proper patch handling as shown in official examples
            PatchUtils.patch(
                    V1Scale.class,
                    () -> customApi.patchNamespacedCustomObjectScale(
                            "argoproj.io",
                            "v1alpha1",
                            namespace,
                            "rollouts",
                            rolloutName,
                            new V1Patch("[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":0}]")
                    ).buildCall(null),
                    V1Patch.PATCH_FORMAT_JSON_PATCH,
                    client
            );

            logger.info("Successfully scaled Rollout {}/{} to 0 replicas", namespace, rolloutName);

        } catch (ApiException e) {
            logger.error("Failed to scale Rollout {}/{}: {}", namespace, rolloutName, e.getResponseBody());
        } catch (Exception e) {
            logger.error("Error scaling Rollout {}/{}: {}", namespace, rolloutName, e.getMessage());
        }
    }
}