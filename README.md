# Kubernetes ECR Image Validation Webhook

A Kubernetes Validating Admission Webhook built with Spring Boot that validates container images exist in Amazon ECR before allowing pod deployments.

## Overview

This webhook intercepts pod creation requests and validates that all container images (including init containers and ephemeral containers) exist in your ECR registry. If any image is not found, the webhook:

1. **Rejects the pod creation** - Prevents deployment of pods with missing images
2. **Scales down the Argo Rollout** - Automatically scales the parent rollout to 0 replicas

This prevents failed deployments and provides immediate feedback when images are missing from ECR.

## Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────┐
│  Pod Creation   │────▶│  Validating Webhook  │────▶│  AWS ECR    │
│  Request        │     │  (Spring Boot)       │     │  Registry   │
└─────────────────┘     └──────────────────────┘     └─────────────┘
                                  │
                                  │ If image not found
                                  ▼
                        ┌──────────────────────┐
                        │  Scale Argo Rollout  │
                        │  to 0 replicas       │
                        └──────────────────────┘
```

## Features

- ✅ Validates all container images (containers, initContainers, ephemeralContainers)
- ✅ Integrates with Amazon ECR for image verification
- ✅ Automatic Argo Rollout scaling on validation failure
- ✅ Kubernetes native integration via Admission Webhook
- ✅ Helm chart for easy deployment
- ✅ Spring Boot 3.5 with Java 21

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5.3 |
| Language | Java 21 |
| Kubernetes Client | kubernetes-client-java 24.0.0 |
| AWS SDK | AWS SDK v2 (ECR, S3) |
| Build Tool | Maven |
| Deployment | Helm |

## Project Structure

```
K8S-validation-springboot-app/
├── patch-webhook-app/          # Spring Boot Application
│   ├── src/
│   │   └── main/java/com/paytm/mutating_webhook/
│   │       ├── controller/
│   │       │   └── AdmissionController.java    # Webhook endpoint
│   │       ├── service/
│   │       │   ├── EcrService.java             # ECR image validation
│   │       │   └── KubernetesService.java      # K8s operations
│   │       └── model/
│   │           ├── AdmissionReviewRequest.java
│   │           ├── AdmissionReviewResponse.java
│   │           ├── Pod.java
│   │           └── Container.java
│   └── pom.xml
└── helm-templates/             # Helm Chart
    ├── Chart.yaml
    └── templates/
        ├── deployment.yaml
        ├── service.yaml
        └── serviceaccount.yaml
```

## How It Works

### 1. Webhook Registration
The webhook is registered with Kubernetes as a ValidatingAdmissionWebhook that intercepts `CREATE` operations on pods.

### 2. Image Validation
When a pod creation request comes in:
```java
@PostMapping
public ResponseEntity<AdmissionReviewResponse> validate(@RequestBody AdmissionReviewRequest reviewRequest) {
    // Extract all containers
    List<Container> allContainers = new ArrayList<>();
    allContainers.addAll(pod.getSpec().getContainers());
    allContainers.addAll(pod.getSpec().getInitContainers());
    allContainers.addAll(pod.getSpec().getEphemeralContainers());
    
    // Check each image exists in ECR
    for (Container container : allContainers) {
        if (!ecrService.doesImageExist(container.getImage())) {
            // Image not found - reject and scale down
        }
    }
}
```

### 3. Response
- **Allowed**: All images exist in ECR → Pod creation proceeds
- **Denied**: Image missing → Pod rejected + Argo Rollout scaled to 0

## Installation

### Prerequisites

- Kubernetes cluster (1.19+)
- Helm 3.x
- AWS credentials configured (for ECR access)
- cert-manager (for TLS certificates)

### Deploy with Helm

```bash
# Clone the repository
git clone https://github.com/yuvrajg69/K8S-validation-springboot-app.git
cd K8S-validation-springboot-app

# Install the Helm chart
helm install ecr-validator ./helm-templates \
  --namespace validation-system \
  --create-namespace \
  --set aws.region=ap-south-1 \
  --set aws.accountId=YOUR_ACCOUNT_ID
```

### Build from Source

```bash
cd patch-webhook-app

# Build the application
./mvnw clean package -DskipTests

# Build Docker image
docker build -t ecr-validator:latest .

# Push to your registry
docker tag ecr-validator:latest YOUR_REGISTRY/ecr-validator:latest
docker push YOUR_REGISTRY/ecr-validator:latest
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_REGION` | AWS region for ECR | `ap-south-1` |
| `AWS_ACCESS_KEY_ID` | AWS access key (or use IAM role) | - |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key (or use IAM role) | - |

### Webhook Configuration

Create a `ValidatingWebhookConfiguration`:

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: ecr-image-validator
webhooks:
  - name: ecr-validator.validation.svc
    clientConfig:
      service:
        name: ecr-validator
        namespace: validation-system
        path: /validate
    rules:
      - operations: ["CREATE"]
        apiGroups: [""]
        apiVersions: ["v1"]
        resources: ["pods"]
    failurePolicy: Fail
    sideEffects: None
    admissionReviewVersions: ["v1"]
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/validate` | POST | Admission webhook endpoint |
| `/actuator/health` | GET | Health check |

## Example Request/Response

### Request (from Kubernetes)
```json
{
  "apiVersion": "admission.k8s.io/v1",
  "kind": "AdmissionReview",
  "request": {
    "uid": "abc-123",
    "object": {
      "metadata": { "name": "my-pod" },
      "spec": {
        "containers": [
          { "image": "123456789.dkr.ecr.ap-south-1.amazonaws.com/my-app:v1" }
        ]
      }
    }
  }
}
```

### Response (Image Found)
```json
{
  "apiVersion": "admission.k8s.io/v1",
  "kind": "AdmissionReview",
  "response": {
    "uid": "abc-123",
    "allowed": true,
    "status": {
      "message": "All images verified in ECR"
    }
  }
}
```

### Response (Image Not Found)
```json
{
  "response": {
    "uid": "abc-123",
    "allowed": false,
    "status": {
      "message": "One or more images not found in ECR"
    }
  }
}
```

## Use Cases

1. **Prevent Deployment Failures** - Catch missing images before pods fail to start
2. **CI/CD Validation** - Ensure images are pushed to ECR before deployment
3. **Security** - Only allow images from your ECR registry
4. **Argo Rollouts Integration** - Automatic rollback on image issues

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License

## Author

Yuvraj Gupta - [@yuvrajg69](https://github.com/yuvrajg69)

