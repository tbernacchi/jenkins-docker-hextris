# Jenkins with Kubernetes Support

> This project configures a Jenkins instance with complete Kubernetes support using Docker Compose.

## Prerequisites

### Traefik Application Proxy
This pipeline requires **Traefik** to be installed and configured in your Kubernetes cluster as an application proxy.

- **Purpose**: Handles ingress routing and SSL termination for the deployed applications
- **Installation**: Deploy Traefik using Helm, manifests, or your preferred method
- **Configuration**: Ensure Traefik is configured to handle ingress resources in the `hextris` namespace

### Kubernetes Cluster
- Running Kubernetes cluster with kubectl access
- Cluster-admin permissions for Jenkins service account
- Sufficient resources for Jenkins pods and application deployments

## Features

- Jenkins LTS with Java 17
- Pre-installed Kubernetes plugin
- Web interface accessible on port 8080
- Docker-in-Docker support
- kubectl installed
- Automatic configuration via JCasC (Jenkins Configuration as Code)

## How to use

### 1. Start Jenkins

```bash
cd /Users/tadeu/arrc/jenkins
docker compose up -d
```

### 2. Access web interface

- URL: http://localhost:8082
- Username: admin
- Password: admin123

### 3. Install required plugins

- **Manage Jenkins** → **Manage Plugins** → **Available**
- Install: **Pipeline**, **Kubernetes**, **Pipeline: Stage View**

### 4. Create Pipeline Job

- **New Item** → **Pipeline**
- **Repository URL**: `https://github.com/tbernacchi/arrc.git`
- **Script Path**: `jenkins/Jenkinsfile`

### 5. Check status

```bash
docker compose ps
docker compose logs jenkins
```

### 6. Stop services

```bash
docker compose down
```

## File structure

```
├── jenkins/
│   ├── Jenkinsfile             # Pipeline
│   └── docker-compose.yaml     # Jenkins with UI
├── hextris/
│   ├── Dockerfile              # Application build
│   └── k8s/                    # Kubernetes manifests
│       ├── 000-deployment.yaml
│       └── 001-ingress.yaml
└── README.md                   # This file
```

## Pipeline

1. **Test Credentials** - Verify Jenkins credentials
2. **Code Checkout** - Git clone
3. **Read Version** - Read version from hextris/.version file
4. **Static Analysis** - Basic file structure validation
5. **Build and Push** - Build Docker image and push to registry
6. **Deploy to Kubernetes** - Deploy to K8s cluster via ArgoCD

## Required Credentials

The pipeline requires the following credentials to be configured in Jenkins:

### 1. **Docker Hub Credentials** (Required for Build & Push)
- **ID**: `docker-hub-credentials` 
- **Type**: Username with password
- **Purpose**: Login to Docker Hub to push built images
- **Status**: ✅ Already configured

### 2. **GitHub Token** (Required for Code Checkout)
- **ID**: `jenkins pat token` (or similar)
- **Type**: Username with password
- **Purpose**: Access to private GitHub repository
- **Your ID**: `f9b98027-b745-4cc5-9346-fa0e6fec2d28`
- **Status**: ✅ Already configured

### Current Pipeline Authentication:
- **Docker Hub**: Uses `docker-hub-credentials` for build and push
- **GitHub**: Uses job-configured credentials via `checkout scm`
- **Kubernetes**: Uses automatic service account token mounted at `/var/run/secrets/kubernetes.io/serviceaccount/token`

### Configure GitHub Credentials in ArgoCD:
For ArgoCD to access private repositories, configure GitHub credentials via UI:

**How to get GitHub PAT Token:**
1. **GitHub** → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token** → **Generate new token (classic)**
3. **Note**: `ArgoCD Git Access`
4. **Expiration**: Choose appropriate duration
5. **Scopes**: Select `repo`
6. **Generate token** and copy the token value
7. Use this token as `YOUR_GITHUB_PAT_TOKEN` in the command above

**ArgoCD UI:**
1. **Access ArgoCD** → **Settings** → **Repositories**
2. **Connect Repo** → **Connect Repo**
3. **Connection Method**: HTTPS
4. **Repository URL**: `https://github.com/tbernacchi/arrc.git`
5. **Username**: `tbernacchi`
6. **Password**: `YOUR_GITHUB_PAT_TOKEN`
7. **Repository Type**: Git
8. **Click** → **Connect**

## Required plugins

### Essential plugins for the project:

#### **Pipeline** (required)
- Main plugin to execute Jenkinsfile
- Syntax `pipeline { ... }`
- Stages and steps

#### **Kubernetes** (required)
- To use `agent { kubernetes { ... } }`
- Executes each stage in separate pods
- Connects Jenkins with K8s cluster

#### **Pipeline: Stage View** (recommended)
- Pipeline stages visualization
- Improved graphical interface

#### **Git** (included)
- Git repositories integration
- Automatic code checkout

#### **Docker Workflow** (included)
- Docker commands support
- Image build and push

### How to install:

1. **Manage Jenkins** → **Manage Plugins**
2. **"Available" tab**
3. **Search and mark:**
   - ✅ Pipeline
   - ✅ Kubernetes
   - ✅ Pipeline: Stage View
   - ✅ SonarQube Scanner
   - ✅ SonarQube Scanner for Jenkins
4. **Install without restart**

### Configure Kubernetes Plugin:

**Manage Jenkins** → **Configure System** → **Cloud**

## Kubernetes Configuration

### 1. Create ServiceAccount in Kubernetes

```bash
# Create service account
kubectl create serviceaccount jenkins -n default

# Grant cluster-admin permissions
kubectl create clusterrolebinding jenkins-admin --clusterrole=cluster-admin --serviceaccount=default:jenkins
```

### 2. Generate access token

```bash
# IMPORTANT: Grant admin permissions to the default ServiceAccount
kubectl create rolebinding default-admin-binding \
  --clusterrole=admin \
  --serviceaccount=hextris:default \
  -n hextris

# Generate token for the service account
kubectl create token jenkins --duration=8760h
```

### 3. Get cluster CA certificate

```bash
# Extract CA certificate
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > ca.crt

# Verify the certificate
cat ca.crt
```

### 4. Configure credentials in Jenkins

**Manage Jenkins** → **Credentials** → **System** → **Global credentials** → **Add Credentials**

- **Kind**: `Secret text`
- **Secret**: `[paste the token generated in step 2]`
- **ID**: `k8s-token`
- **Description**: `Kubernetes service account token`

### 5. Configure Kubernetes Cloud

**Manage Jenkins** → **Configure Clouds** → **Add a new cloud** → **Kubernetes**

- **Name**: `kubernetes`
- **Kubernetes URL**: `https://YOUR_IP:6443` (e.g: `https://192.168.1.106:6443`) # Your kubernetes cluster. You can get with kubectl cluster-info.
- **Kubernetes server certificate key**: `[paste the content of ca.crt from step 3]`
- **Disable HTTPS certificate check**: ❌ (unchecked - using certificate)
- **Credentials**: Select `k8s-token`
- **Jenkins URL**: `http://192.168.1.100:8082` # Your local IP address (It's a docker.)
- **Test Connection**: Should show "Connection test successful"
