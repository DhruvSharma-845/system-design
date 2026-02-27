# Feed Solution — Architecture

## High-Level Diagram

```
                                    ┌──────────┐
                                    │ Browser  │
                                    │ (User)   │
                                    └────┬─────┘
                                         │
                                  http://feed.local
                                         │
┌────────────────────────────────────────┼────────────────────────────────────┐
│                    Kubernetes Cluster (feed namespace)                       │
│                                        │                                    │
│         ┌──────────────────────────────▼────────────────────────┐         │
│         │         NGINX Gateway Fabric (Gateway API)              │         │
│         │                  host: feed.local                       │         │
│         │                                                         │         │
│         │  /           → feeds-web-app :80                       │         │
│         │  /api        → gateway :9090                            │         │
│         │  /realms     → keycloak :8080                           │         │
│         │  /resources  → keycloak :8080                           │         │
│         └──────┬────────────────┬────────────────┬──────────────┘         │
│                │                │                │                         │
│                ▼                ▼                ▼                         │
│       ┌──────────────┐  ┌────────────┐  ┌──────────────────┐              │
│       │ feeds-web-app│  │  Keycloak  │  │   API Gateway    │              │
│       │ React + Vite │  │   (IdP)    │  │ (Spring Cloud GW)│              │
│       │ OIDC + PKCE  │  │ OIDC/OAuth2│  │ JWT validation   │              │
│       │ nginx :80    │  │ :8080      │  │ :9090            │              │
│       └──────────────┘  └────────────┘  └────────┬─────────┘              │
│                                                   │                        │
│                    ┌──────────────────────────────┼──────────────────┐   │
│                    ▼                              ▼                  ▼   │
│           ┌────────────────┐            ┌────────────────┐  ┌────────────┐│
│           │  User Service   │            │ Post Service   │  │  Timeline  ││
│           │  (usersdb)      │            │ (postsdb)      │  │  Service  ││
│           │  :8082          │◄───────────│ :8080          │  │ (postsdb) ││
│           │  signup, /me    │  resolve   │ create post    │  │ :8081     ││
│           └────────┬────────┘  user id   └────────┬───────┘  └─────┬──────┘│
│                    │                              │                │      │
│                    ▼                              └────────┬───────┘      │
│           ┌────────────────┐                              │             │
│           │  PostgreSQL     │  usersdb    postsdb           │             │
│           │  :5432          │  (users)    (posts)            │             │
│           └─────────────────┘                              │             │
│                                                             │             │
└─────────────────────────────────────────────────────────────┼─────────────┘
                                                               │
```

## Request Flow (OIDC)

1. Browser loads React SPA from **feeds-web-app** (`/`).
2. User clicks Login/Sign Up → redirect to **Keycloak** (`/realms/feed/...`).
3. Keycloak authenticates → Authorization Code + PKCE → JWT tokens.
4. Browser sends API requests with `Authorization: Bearer <JWT>` to `/api/...`.
5. **Gateway** validates JWT (Keycloak JWKS) and routes by path.
6. Backend service validates JWT again (defense in depth) and processes the request.

## Auth Flow

### User registration (self-service signup)

1. User clicks "Sign Up" → redirect to Keycloak (with `kc_action=register`).
2. User registers in Keycloak; gets `feed_user` role.
3. Keycloak redirects back with authorization code.
4. Frontend exchanges code for tokens, then calls **User Service** `POST /api/v1/users/signup` to persist the user in **usersdb** (idempotent).

### User login and API access

1. User authenticates with Keycloak (OIDC, PKCE).
2. Keycloak issues JWT (access + refresh).
3. Frontend sends requests with `Authorization: Bearer <access_token>`.
4. Gateway and each service validate JWT and use `sub` / roles as needed.
5. **Pattern B**: Internal user id is the central concept. Post service calls User Service `GET /api/v1/users/me` (forwarding JWT) to resolve author → internal user id, then stores that id in **postsdb**.

## Technology Choices

| Component         | Technology                                  | Why                                      |
| ----------------- | ------------------------------------------- | ---------------------------------------- |
| Frontend          | React 19 + Vite + TypeScript                 | Modern SPA, fast tooling                  |
| K8s ingress       | NGINX Gateway Fabric (Gateway API)           | K8s-native routing                        |
| Identity provider | Keycloak                                    | OIDC/OAuth2, self-hosted, cloud-agnostic |
| API gateway       | Spring Cloud Gateway                        | JWT validation, reactive, route to svcs  |
| Service auth      | Spring Security OAuth2 Resource Server       | Per-service JWT validation               |
| Auth protocol     | OpenID Connect (OIDC)                       | Industry standard                         |
| Token format      | JWT                                         | Stateless, verifiable                     |
| Frontend auth     | Authorization Code + PKCE                   | Secure for SPAs, no client secret        |

## Roles and Permissions

| Role              | Description                    | Permissions                          |
| ----------------- | ------------------------------ | ------------------------------------ |
| `feed_user`       | Standard user                  | Create posts, read posts/timeline    |
| `feed_moderator`  | Content moderator              | feed_user + delete any post          |
| `feed_admin`      | Administrator                  | Full access                          |

## Data Stores

- **PostgreSQL** (single deployment, two databases):
  - **usersdb** — User Service only. Table: `users` (id, keycloak_sub_id, username, email, created_at). Internal user id is used across services (Pattern B).
  - **postsdb** — Post Service and Timeline Service. Table: `posts` (id, content, author_id). `author_id` is the internal user id (FK logically; no cross-DB FK). Flyway runs per service with no version collision.

## API Routes (Gateway)

| Path                | Service        | Description                    |
| ------------------- | -------------- | ------------------------------ |
| `/`                 | feeds-web-app  | SPA                            |
| `/realms`, `/resources` | Keycloak  | OIDC / account                 |
| `/api/v1/posts`     | postservice    | Create post (JWT → user id)    |
| `/api/v1/timelines` | timelineservice| Get timeline (posts)           |
| `/api/v1/users/*`   | userservice    | Signup, /me, by-sub, by id     |

## Project Structure

```
feed-solution/
├── feeds-web-app/           # Frontend SPA (React, Vite, OIDC PKCE)
├── gateway/                 # Spring Cloud Gateway (JWT, routing)
├── postservice/             # Create posts; uses userservice for author id
├── timelineservice/         # Read timeline (posts from postsdb)
├── userservice/             # User signup, /me, by-sub, by id (usersdb)
├── keycloak/                # Realm config (feed-realm.json)
├── k8s/                     # Kustomize: postgres (usersdb + postsdb), all services
├── kind-config.yaml         # Kind cluster (host 80/443)
├── build-deploy-script.sh   # Build images, load into Kind, deploy
├── architecture.md          # This file
└── README.md                # Getting started
```

## Frontend Integration

- **feed-frontend** client: public, Authorization Code + PKCE. Redirect URIs include feed.local, localhost:3000, 5173, 4200.
- After login/signup, frontend calls `POST /api/v1/users/signup` to sync user to usersdb.
- Use token claims (`realm_access.roles`) for role-based UI.

## Cloud and Production Notes

- Runs on any Kubernetes (EKS, GKE, AKS, k3s). Use cloud ingress/ALB for gateway.
- Production: Keycloak `start` (not dev) with external DB; TLS (e.g. cert-manager); external secrets; optional Keycloak HA and custom theme. Remove seed users from realm config.
