#!/bin/bash
set -e

echo "=== Building Post Service ==="
podman build -t localhost/postservice:dev ./postservice
podman save -o /tmp/postservice-dev.tar localhost/postservice:dev

echo "=== Building API Gateway ==="
podman build -t localhost/gateway:dev ./gateway
podman save -o /tmp/gateway-dev.tar localhost/gateway:dev

echo "=== Building Feeds Web App ==="
podman build -t localhost/feeds-web-app:dev ./feeds-web-app
podman save -o /tmp/feeds-web-app-dev.tar localhost/feeds-web-app:dev

echo "=== Loading images into Kind cluster ==="
kind load image-archive /tmp/postservice-dev.tar --name feed-cluster
kind load image-archive /tmp/gateway-dev.tar --name feed-cluster
kind load image-archive /tmp/feeds-web-app-dev.tar --name feed-cluster

echo "=== Applying K8s manifests ==="
kubectl apply -k k8s/

echo "=== Restarting deployments to pick up new images ==="
# Since the image tag stays the same, kubectl apply may not recreate pods.
# A rollout restart forces the pod to pick up the newly loaded local image.
kubectl -n feed rollout restart deployment/postservice
kubectl -n feed rollout restart deployment/gateway
kubectl -n feed rollout restart deployment/feeds-web-app

kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx wait --for=condition=ready pod -l app.kubernetes.io/component=controller --timeout=120s

echo ""
echo "=== Deployment complete ==="
echo ""
echo "Wait for all pods to be ready:"
echo "  kubectl -n feed get pods -w"
echo ""
echo "URL access (no port-forward for frontend + API):"
echo "  http://feed.local"
echo "  # Requires kind cluster created with kind-config.yaml (ports 80/443 mapped)"
echo ""
echo "Optional port-forward for Keycloak Admin Console:"
echo "  kubectl -n feed port-forward svc/keycloak 8080:8080    # Keycloak Admin Console"
echo ""
echo "Get a JWT token (testuser):"
echo "  curl -s -X POST http://localhost:8080/realms/feed/protocol/openid-connect/token \\"
echo "    -d 'client_id=feed-frontend' \\"
echo "    -d 'username=testuser' \\"
echo "    -d 'password=testpass' \\"
echo "    -d 'grant_type=password' | jq -r '.access_token'"
echo ""
echo "Create a post (via gateway through ingress):"
echo "  curl -X POST http://feed.local/api/v1/posts \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -H 'Authorization: Bearer <TOKEN>' \\"
echo "    -d '{\"content\": \"Hello from authenticated user!\"}'"
