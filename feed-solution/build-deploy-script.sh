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

echo "=== Installing Gateway API CRDs ==="
kubectl kustomize "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/standard?ref=v2.4.1" | kubectl apply -f -

echo "=== Installing/Updating NGINX Gateway Fabric controller ==="
helm upgrade --install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric \
  --create-namespace \
  -n nginx-gateway \
  --set nginx.service.type=NodePort \
  --set-json 'nginx.service.nodePorts=[{"port":30080,"listenerPort":80},{"port":30443,"listenerPort":443}]'
kubectl -n nginx-gateway wait --for=condition=available deployment/ngf-nginx-gateway-fabric --timeout=180s

echo "=== Applying K8s manifests ==="
kubectl apply -k k8s/

echo "=== Restarting deployments to pick up new images ==="
# Since the image tag stays the same, kubectl apply may not recreate pods.
# A rollout restart forces the pod to pick up the newly loaded local image.
kubectl -n feed rollout restart deployment/postservice
kubectl -n feed rollout restart deployment/gateway
kubectl -n feed rollout restart deployment/feeds-web-app

echo ""
echo "=== Deployment complete ==="
echo ""
echo "Wait for all pods to be ready:"
echo "  kubectl -n feed get pods -w"
echo ""
echo "URL access (no port-forward for frontend + API):"
echo "  http://feed.local"
echo "  # Requires kind cluster created with kind-config.yaml (host 80/443 mapped to Kind nodePorts)"
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
echo "Create a post (via gateway through Kubernetes Gateway API):"
echo "  curl -X POST http://feed.local/api/v1/posts \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -H 'Authorization: Bearer <TOKEN>' \\"
echo "    -d '{\"content\": \"Hello from authenticated user!\"}'"
