package com.paytm.mutating_webhook.model;

import lombok.Data;

@Data
public class AdmissionReviewResponse {
    private String apiVersion = "admission.k8s.io/v1";
    private String kind = "AdmissionReview";
    private AdmissionResponse response;

    @Data
    public static class AdmissionResponse {
        private String uid;
        private boolean allowed;
        private Status status;

        @Data
        public static class Status {
            private String message;
        }
    }
}