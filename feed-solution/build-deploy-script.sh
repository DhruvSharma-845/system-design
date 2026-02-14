#!/bin/bash
set -e

echo "=== Building Post Service ==="
podman build -t localhost/postservice:dev ./postservice
podman save -o /tmp/postservice-dev.tar localhost/postservice:dev

echo "=== Building API Gateway ==="
podman build -t localhost/gateway:dev ./gateway
podman save -o /tmp/gateway-dev.tar localhost/gateway:dev

echo "=== Loading images into Kind cluster ==="
kind load image-archive /tmp/postservice-dev.tar --name feed-cluster
kind load image-archive /tmp/gateway-dev.tar --name feed-cluster

echo "=== Applying K8s manifests ==="
kubectl apply -k k8s/

echo "=== Restarting deployments to pick up new images ==="
# Since the image tag stays the same, kubectl apply may not recreate pods.
# A rollout restart forces the pod to pick up the newly loaded local image.
kubectl -n feed rollout restart deployment/postservice
kubectl -n feed rollout restart deployment/gateway

echo ""
echo "=== Deployment complete ==="
echo ""
echo "Wait for all pods to be ready:"
echo "  kubectl -n feed get pods -w"
echo ""
echo "Port-forward services for local access:"
echo "  kubectl -n feed port-forward svc/keycloak 8080:8080    # Keycloak Admin Console"
echo "  kubectl -n feed port-forward svc/gateway 9090:9090     # API Gateway"
echo ""
echo "Get a JWT token (testuser):"
echo "  curl -s -X POST http://localhost:8080/realms/feed/protocol/openid-connect/token \\"
echo "    -d 'client_id=feed-frontend' \\"
echo "    -d 'username=testuser' \\"
echo "    -d 'password=testpass' \\"
echo "    -d 'grant_type=password' | jq -r '.access_token'"
echo ""
echo "Create a post (via gateway):"
echo "  curl -X POST http://localhost:9090/api/v1/posts \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -H 'Authorization: Bearer <TOKEN>' \\"
echo "    -d '{\"content\": \"Hello from authenticated user!\"}'"
