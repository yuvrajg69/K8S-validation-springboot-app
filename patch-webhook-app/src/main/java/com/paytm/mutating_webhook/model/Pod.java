// File: com/paytm/list_ecr_images/model/Pod.java
package com.paytm.mutating_webhook.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Pod {
    private Metadata metadata;
    private PodSpec spec;

    @Data
    public static class Metadata {
        private String name;
        private String namespace;
        private Map<String, String> labels;
        private List<OwnerReference> ownerReferences;
    }

    @Data
    public static class PodSpec {
        private List<Container> containers;
        private List<Container> initContainers;
        private List<Container> ephemeralContainers;
    }

    @Data
    public static class OwnerReference {
        private String apiVersion;
        private String kind;     // e.g., ReplicaSet, StatefulSet
        private String name;     // name of the owning resource
        private String uid;
        private Boolean controller;
    }
}