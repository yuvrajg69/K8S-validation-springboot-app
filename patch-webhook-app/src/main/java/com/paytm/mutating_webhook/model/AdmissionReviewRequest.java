package com.paytm.mutating_webhook.model;

import lombok.Data;

@Data
public class AdmissionReviewRequest {
    private AdmissionRequest request;

    @Data
    public static class AdmissionRequest {
        private String uid;
        private String operation;
        private ResourceKind kind;
        private ResourceKind resource;
        private Pod object;
    }

    @Data
    public static class ResourceKind {
        private String group;
        private String version;
        private String kind;
    }
}
