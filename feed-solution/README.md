Directory containing services and apps for feed solution

# Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         Kubernetes Cluster                       │
│                                                                  │
│  ┌─────────┐     ┌──────────────┐     ┌──────────────────────┐  │
│  │Keycloak │     │  API Gateway │     │    Post Service       │  │
│  │  (IdP)  │◄────│  (Spring     │────►│  (Spring Boot +      │  │
│  │         │     │   Cloud GW)  │     │   OAuth2 Resource    │  │
│  │ :8080   │     │   :9090      │     │   Server) :8080      │  │
│  └─────────┘     └──────┬───────┘     └──────────┬───────────┘  │
│       ▲                 │                        │               │
│       │                 │                  ┌─────▼─────┐         │
│       │                 │                  │ PostgreSQL │         │
│       │                 │                  │   :5432    │         │
│       │                 │                  └───────────-┘         │
└───────┼─────────────────┼────────────────────────────────────────┘
        │                 │
        │    ┌────────────▼────────────┐
        │    │   Future Frontend SPA   │
        └────│  (React/Vue/Angular)    │
             │  OIDC + PKCE Flow       │
             └─────────────────────────┘
```

## Auth Flow

### User Registration (self-service signup)

1. User clicks "Sign Up" in the frontend (or visits Keycloak directly)
2. Frontend redirects to Keycloak's login page, which has a **Register** link
3. User fills in the registration form (username, email, first/last name, password)
4. Keycloak creates the account and auto-assigns the `feed_user` role
5. User is authenticated and redirected back to the frontend with JWT tokens
6. No backend code needed -- Keycloak handles the entire registration flow

### User Login & API Access

1. **User authenticates** with Keycloak via OIDC (Authorization Code + PKCE for SPAs)
2. **Keycloak issues JWT** tokens (access token + refresh token)
3. **Frontend sends requests** to the API Gateway with `Authorization: Bearer <JWT>`
4. **Gateway validates JWT** (first layer of defense) and routes to the microservice
5. **Microservice validates JWT** independently (defense in depth) and applies authorization
6. **User identity** is extracted from JWT `sub` claim (Keycloak user UUID)
7. **Roles** are extracted from JWT `realm_access.roles` claim

## Technology Choices

| Component | Technology | Why |
|-----------|-----------|-----|
| Identity Provider | **Keycloak** | Open-source, cloud-agnostic, OIDC/OAuth2/SAML, self-hosted |
| API Gateway | **Spring Cloud Gateway** | Native Spring integration, reactive, JWT validation |
| Service Auth | **Spring Security OAuth2 Resource Server** | Industry standard, per-service JWT validation |
| Auth Protocol | **OpenID Connect (OIDC)** | Industry standard on top of OAuth2 |
| Token Format | **JWT** | Stateless, self-contained, verifiable |
| Frontend Auth (future) | **OIDC Authorization Code + PKCE** | Secure flow for SPAs, no client secret needed |

## Roles & Permissions

| Role | Description | Permissions |
|------|-------------|-------------|
| `feed_user` | Standard user | Create posts, read posts |
| `feed_moderator` | Content moderator (includes feed_user) | All user permissions + delete any post |
| `feed_admin` | Administrator (includes all roles) | Full access to all resources |

## Project Structure

```
feed-solution/
├── gateway/                    # API Gateway (Spring Cloud Gateway)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../gateway/
│       │   ├── GatewayApplication.java
│       │   └── config/SecurityConfig.java
│       └── resources/application.yml
├── postservice/                # Post Service (Spring Boot)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../postservice/
│       │   ├── config/SecurityConfig.java     # JWT validation + RBAC
│       │   ├── controller/PostSubmissionController.java
│       │   └── ...
│       └── resources/
│           ├── application.properties
│           └── db/migration/
│               ├── V1__create_posts_table.sql
│               └── V2__add_author_to_posts.sql
├── keycloak/                   # Keycloak realm configuration
│   └── feed-realm.json         # Pre-configured realm, clients, roles, test users
├── k8s/                        # Kubernetes manifests
│   ├── kustomization.yaml
│   ├── keycloak-*.yaml         # Keycloak deployment
│   ├── gateway-*.yaml          # Gateway deployment
│   ├── postservice-*.yaml      # Post service deployment
│   └── postgres-*.yaml         # PostgreSQL deployment
├── build-deploy-script.sh      # Build and deploy all services
└── README.md
```

# Local Development Setup

## Prerequisites

- Kind (Kubernetes in Docker)
- Podman or Docker
- kubectl
- jq (for parsing JSON responses)

## 1. Create local Kubernetes cluster

```bash
kind create cluster --name feed-cluster
```

## 2. Build and deploy all services

```bash
chmod +x ./build-deploy-script.sh
./build-deploy-script.sh
```

## 3. Wait for pods to be ready

```bash
kubectl -n feed get pods -w
```

## 4. Port-forward services

In separate terminals:

```bash
# Keycloak (Admin Console + token endpoint)
kubectl -n feed port-forward svc/keycloak 8080:8080

# API Gateway (all API traffic goes through here)
kubectl -n feed port-forward svc/gateway 9090:9090
```

## 5. Test authentication

### Get a JWT token

```bash
# Using the direct access grant (Resource Owner Password) for testing
TOKEN=$(curl -s -X POST http://localhost:8080/realms/feed/protocol/openid-connect/token \
  -d 'client_id=feed-frontend' \
  -d 'username=testuser' \
  -d 'password=testpass' \
  -d 'grant_type=password' | jq -r '.access_token')

echo $TOKEN
```

### Create a post (via gateway)

```bash
curl -X POST http://localhost:9090/api/v1/posts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content": "Hello from authenticated user!"}'
```

### Verify auth is enforced (should return 401)

```bash
curl -v http://localhost:9090/api/v1/posts
```

## Seed Test Users (dev only)

These users are pre-loaded via `feed-realm.json` for local development testing.
In production, all users register dynamically through Keycloak's self-service signup.

| Username | Password | Roles |
|----------|----------|-------|
| testuser | testpass | feed_user |
| testadmin | testpass | feed_user, feed_admin |
| testmoderator | testpass | feed_user, feed_moderator |

## Register a New User (dynamic signup)

After port-forwarding Keycloak, visit the registration page directly:

```
http://localhost:8080/realms/feed/protocol/openid-connect/registrations?client_id=feed-frontend&response_type=code&scope=openid&redirect_uri=http://localhost:3000/
```

Or go to the Keycloak login page and click **Register**:

```
http://localhost:8080/realms/feed/account
```

New users are automatically assigned the `feed_user` role upon registration.
Admins can promote users to `feed_moderator` or `feed_admin` via the Keycloak Admin Console.

## Keycloak Admin Console

Access at http://localhost:8080 after port-forwarding.
- Username: `admin`
- Password: `admin`

# Future Frontend Integration

The `feed-frontend` client in Keycloak is pre-configured for SPA integration:

- **Protocol**: OpenID Connect
- **Flow**: Authorization Code + PKCE (most secure for SPAs)
- **Public Client**: Yes (no client secret required)
- **Redirect URIs**: localhost:3000, localhost:5173, localhost:4200

### Recommended frontend OIDC libraries

| Framework | Library |
|-----------|---------|
| React | `oidc-client-ts` + `react-oidc-context` |
| Vue | `oidc-client-ts` + custom composable |
| Angular | `angular-auth-oidc-client` |
| Any | `keycloak-js` (Keycloak's official JS adapter) |

### Frontend signup + login flow (PKCE)

**Sign Up (new user):**
1. User clicks "Sign Up" in the frontend
2. Frontend redirects to Keycloak (same OIDC endpoint -- Keycloak shows a "Register" link)
3. User fills in the registration form (Keycloak's built-in UI or custom theme)
4. Keycloak creates the account, assigns `feed_user` role automatically
5. Keycloak redirects back with authorization code
6. Frontend exchanges code for tokens (access + refresh + id token)

**Login (existing user):**
1. User clicks "Login" -> redirect to Keycloak login page
2. User enters credentials
3. Keycloak redirects back with authorization code
4. Frontend exchanges code for tokens

**After authentication (both flows):**
1. Frontend stores tokens in memory (not localStorage -- XSS protection)
2. Frontend includes `Authorization: Bearer <access_token>` in API requests to the gateway
3. Frontend uses token claims to show/hide UI based on roles
4. Frontend uses the refresh token to silently renew the access token before expiry

### Role-based UI visibility

```javascript
// Example: Check roles from decoded JWT access token
const roles = decodedToken.realm_access?.roles || [];

const canCreatePost = roles.includes('feed_user');
const canModerate = roles.includes('feed_moderator');
const canAdmin = roles.includes('feed_admin');

// Show/hide UI elements based on roles
// - All authenticated users see the feed and can create posts
// - Moderators see a "Delete" button on any post
// - Admins see a "User Management" section in navigation
```

# Cloud Deployment Notes

This architecture is **cloud-agnostic** and runs on any Kubernetes cluster:

| Cloud | Managed K8s | Notes |
|-------|-------------|-------|
| AWS | EKS | Use ALB Ingress Controller for gateway exposure |
| GCP | GKE | Use GKE Ingress or Istio |
| Azure | AKS | Use Azure Application Gateway Ingress |
| Any | k3s, k0s | Lightweight K8s for self-hosted |

### Production considerations

1. **Keycloak**: Switch from `start-dev` to `start` mode with external PostgreSQL
2. **Email verification**: Set `verifyEmail: true` in realm config and configure SMTP in Keycloak (Realm Settings -> Email). This ensures only real email addresses can register.
3. **SMTP**: Configure an email provider (SendGrid, AWS SES, Mailgun) in Keycloak for verification emails, password resets, and notifications
4. **TLS**: Use cert-manager + Let's Encrypt for HTTPS
5. **Ingress**: Add Ingress resource to expose the gateway externally
6. **Secrets**: Use external secret management (Vault, AWS Secrets Manager, etc.)
7. **Keycloak HA**: Run multiple Keycloak replicas with shared database
8. **Token security**: Configure appropriate token lifespans in Keycloak
9. **Custom Keycloak theme**: Brand the login/registration pages to match your frontend
10. **Remove seed users**: Delete the test users from `feed-realm.json` before production deployment
