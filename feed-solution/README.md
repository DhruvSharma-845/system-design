# Feed Solution

A feed-style app with login, signup, posts, and timeline. Runs on Kubernetes (Kind) with Keycloak, Spring Cloud Gateway, and microservices (user, post, timeline).

**Architecture:** See [architecture.md](./architecture.md) for diagrams, auth flow, technology choices, and project structure.

---

## Prerequisites

- [Kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker)
- Podman or Docker
- kubectl
- Helm
- jq (for parsing JSON)

## Quick Start

### 1. Create cluster

```bash
kind create cluster --config kind-config.yaml
```

### 2. Build and deploy

```bash
chmod +x ./build-deploy-script.sh
./build-deploy-script.sh
```

### 3. Wait for pods

```bash
kubectl -n feed get pods -w
```

### 4. Local URL access

Add to `/etc/hosts`:

```bash
echo "127.0.0.1 feed.local" | sudo tee -a /etc/hosts
```

- **App:** http://feed.local  
- **API:** http://feed.local/api  

Optional (Keycloak admin):

```bash
kubectl -n feed port-forward svc/keycloak 8080:8080
```

## Test Authentication

Get a token (use **feed-gateway** client for password grant):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/feed/protocol/openid-connect/token \
  -d 'client_id=feed-gateway' \
  -d 'client_secret=feed-gateway-secret' \
  -d 'username=testuser' \
  -d 'password=testpass' \
  -d 'grant_type=password' | jq -r '.access_token')
```

Create a post:

```bash
curl -X POST http://feed.local/api/v1/posts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content": "Hello from authenticated user!"}'
```

## Seed Users (dev)

| Username      | Password | Roles                     |
| ------------- | -------- | ------------------------- |
| testuser      | testpass | feed_user                 |
| testadmin     | testpass | feed_user, feed_admin     |
| testmoderator | testpass | feed_user, feed_moderator |

## Register a New User

1. Open the app at http://feed.local and click **Sign Up**, or go to Keycloak and click **Register**.
2. After signup, the app calls the user service to persist the user; you can then log in and create posts.

Keycloak admin (after port-forward): http://localhost:8080 — user `admin`, password `admin`.
