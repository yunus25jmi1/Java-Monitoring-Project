# Java - Demo Web Application

This is a simple Java web app built using Spring Boot and Java 21 LTS.

The app has been designed with cloud native demos & containers in mind, in order to provide a real working application for deployment, something more than "hello-world" but with the minimum of pre-reqs. It is not intended as a complete example of a fully functioning architecture or complex software design.

Typical uses would be deployment to Kubernetes, demos of Docker, CI/CD (build pipelines are provided), deployment to cloud (Azure) monitoring, auto-scaling

The app has several basic pages accessed from the top navigation menu, some of which are only lit up when certain configuration variables are set (see 'Optional Features' below):

Features:

- The **'Info'** page displays some system basic information (OS, platform, CPUs, IP address etc) and should detect if the app is running as a container or not.
- The **'Tools'** page is useful in demos, and has options such a forcing CPU load (for autoscale demos), and error pages for use with App Insights
- The **'mBeans'** page is a basic Java mBeans explorer, letting you inspect mBeans registered with the JVM and the properties they are exposing
- Azure AD integration for user auth and sign-in (optional, see config below)
- Azure App Insights for monitoring (optional, see config below)

![](https://user-images.githubusercontent.com/14982936/71443390-87cd0680-2702-11ea-857c-63d34a6e1306.png)

# Status

![](https://img.shields.io/github/last-commit/benc-uk/java-demoapp) ![](https://img.shields.io/github/release-date/benc-uk/java-demoapp) ![](https://img.shields.io/github/v/release/benc-uk/java-demoapp) ![](https://img.shields.io/github/commit-activity/y/benc-uk/java-demoapp)

Live instances:

[![](https://img.shields.io/website?label=Hosted%3A%20Azure%20App%20Service&up_message=online&url=https%3A%2F%2Fjava-demoapp.azurewebsites.net%2F)](https://java-demoapp.azurewebsites.net/)  
[![](https://img.shields.io/website?label=Hosted%3A%20Kubernetes&up_message=online&url=https%3A%2F%2Fjava-demoapp.kube.benco.io%2F)](https://java-demoapp.kube.benco.io/)

# Building & Running Locally

### Pre-reqs

- Be using Linux, WSL, MacOS, or Windows with PowerShell
- [Java 21+](https://adoptium.net/) - for running locally, linting, running tests etc
- [Docker Desktop](https://docs.docker.com/get-docker/) - for running as a container, or image build and push
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) - for deployment to Azure
- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) - for deployment to Google Cloud Platform

Clone the project to any directory where you do development work

```
git clone https://github.com/benc-uk/java-demoapp.git
```

### Makefile

A standard GNU Make file is provided to help with running and building locally.

```text
help                 💬 This help message
lint                 🔎 Lint & format, will not fix but sets exit code on error
lint-fix             📜 Lint & format, will try to fix errors and modify code
image                🔨 Build container image from Dockerfile
push                 📤 Push container image to registry
run                  🏃 Run the server locally using Python & Flask
deploy               🚀 Deploy to Azure Web App
undeploy             💀 Remove from Azure
test                 🎯 Unit tests for server and frontend
test-report          🎯 Unit tests for server and frontend (with report output)
test-api             🚦 Run integration API tests, server must be running
clean                🧹 Clean up project
```

Make file variables and default values, pass these in when calling `make`, e.g. `make image IMAGE_REPO=blah/foo`

| Makefile Variable | Default              |
| ----------------- | -------------------- |
| IMAGE_REG         | ghcr<span>.</span>io |
| IMAGE_REPO        | benc-uk/java-demoapp |
| IMAGE_TAG         | latest               |
| AZURE_RES_GROUP   | temp-demoapps        |
| AZURE_REGION      | uksouth              |
| AZURE_SITE_NAME   | java-{git-sha}       |

The application listens on port 8080 by default, but this can be set with the `PORT` environmental variable.

# Containers

Public container image is [available on GitHub Container Registry](https://github.com/users/benc-uk/packages/container/package/java-demoapp)

Run in a container with:

```bash
docker run --rm -it -p 8080:8080 ghcr.io/benc-uk/java-demoapp:latest
```

Should you want to build your own container, use `make image` and the above variables to customise the name & tag.

# Kubernetes

The app can easily be deployed to Kubernetes using Helm, see [deploy/kubernetes/readme.md](deploy/kubernetes/readme.md) for details

# Cloud Deployment

## Google Cloud Platform (GCP) Deployment

### Prerequisites
- Google Cloud SDK installed and configured
- Docker Desktop running
- GCP project with billing enabled
- Required APIs enabled: Cloud Run, Artifact Registry, Cloud Build

### Quick Start - Deploy to Cloud Run

**Step 1: Set up GCP environment**

```powershell
# Login to Google Cloud
gcloud auth login

# Set your project ID
gcloud config set project YOUR_PROJECT_ID

# Enable required APIs
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable cloudbuild.googleapis.com
```

**Step 2: Create Artifact Registry repository**

```powershell
# Create a Docker repository in Artifact Registry
gcloud artifacts repositories create java-demoapp `
  --repository-format=docker `
  --location=us-central1 `
  --description="Java demoapp container repo"

# Configure Docker to authenticate to Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev -q
```

**Step 3: Build and push Docker image**

```powershell
# Get your project ID
$env:PROJECT_ID = (gcloud config get-value project)

# Build the Docker image with Java 21
docker build -t "us-central1-docker.pkg.dev/$env:PROJECT_ID/java-demoapp/java-demoapp:latest" .

# Push to Artifact Registry
docker push "us-central1-docker.pkg.dev/$env:PROJECT_ID/java-demoapp/java-demoapp:latest"
```

**Step 4: Deploy to Cloud Run**

```powershell
# Deploy the container to Cloud Run
gcloud run deploy java-demoapp `
  --image "us-central1-docker.pkg.dev/$env:PROJECT_ID/java-demoapp/java-demoapp:latest" `
  --region us-central1 `
  --platform managed `
  --allow-unauthenticated `
  --port 8080 `
  --memory 1Gi `
  --cpu 1 `
  --min-instances 0 `
  --max-instances 5

# Get the service URL
gcloud run services describe java-demoapp --region us-central1 --format 'value(status.url)'
```

### GCP Deployment Options

#### Option A: Direct Source Deployment
Cloud Run can build from source automatically:

```powershell
gcloud run deploy java-demoapp `
  --source . `
  --region us-central1 `
  --allow-unauthenticated
```

#### Option B: Using Cloud Build
For CI/CD pipeline deployment:

```powershell
# Submit build to Cloud Build
gcloud builds submit --config cloudbuild.yaml .
```

#### Option C: Deploy to Google Kubernetes Engine (GKE)

```powershell
# Create a GKE cluster
gcloud container clusters create java-demoapp-cluster `
  --num-nodes=2 `
  --machine-type=e2-medium `
  --region=us-central1

# Get cluster credentials
gcloud container clusters get-credentials java-demoapp-cluster --region=us-central1

# Deploy using kubectl (requires kubernetes deployment manifests)
kubectl apply -f kubernetes-deployment.yaml
```

### Environment Variables for GCP

Set environment variables for Azure AD integration:

```powershell
gcloud run services update java-demoapp `
  --region us-central1 `
  --set-env-vars "azure_activedirectory_clientid=YOUR_CLIENT_ID" `
  --set-env-vars "azure_activedirectory_clientsecret=YOUR_SECRET" `
  --set-env-vars "azure_activedirectory_tenantid=YOUR_TENANT"
```

### Monitoring and Logs (GCP)

```powershell
# View Cloud Run logs
gcloud run services logs read java-demoapp --region us-central1 --limit 100

# Stream logs in real-time
gcloud run services logs tail java-demoapp --region us-central1

# View service details
gcloud run services describe java-demoapp --region us-central1
```

### Cost Management (GCP)

```powershell
# Update scaling configuration
gcloud run services update java-demoapp `
  --region us-central1 `
  --min-instances 0 `
  --max-instances 10 `
  --cpu-throttling `
  --concurrency 80

# Delete Cloud Run service
gcloud run services delete java-demoapp --region us-central1

# Delete Artifact Registry repository
gcloud artifacts repositories delete java-demoapp --location=us-central1
```

## Azure Deployment

### Prerequisites
- Azure CLI installed and configured
- Docker Desktop running
- Azure subscription with contributor access
- Resource group created

### Quick Start - Deploy to Azure App Service

**Step 1: Login to Azure**

```powershell
# Login to Azure
az login

# Set your subscription
az account set --subscription YOUR_SUBSCRIPTION_ID

# Create a resource group (if needed)
az group create --name java-demoapp-rg --location eastus
```

**Step 2: Create Azure Container Registry (ACR)**

```powershell
# Create ACR
az acr create --resource-group java-demoapp-rg `
  --name javademoappacr `
  --sku Basic `
  --admin-enabled true

# Login to ACR
az acr login --name javademoappacr

# Get ACR login server
$ACR_LOGIN_SERVER = (az acr show --name javademoappacr --query loginServer --output tsv)
```

**Step 3: Build and push to ACR**

```powershell
# Build the Docker image
docker build -t "$ACR_LOGIN_SERVER/java-demoapp:latest" .

# Push to ACR
docker push "$ACR_LOGIN_SERVER/java-demoapp:latest"
```

**Step 4: Deploy to Azure App Service**

```powershell
# Create App Service Plan (Linux)
az appservice plan create --name java-demoapp-plan `
  --resource-group java-demoapp-rg `
  --is-linux `
  --sku B1

# Create Web App from container
az webapp create --resource-group java-demoapp-rg `
  --plan java-demoapp-plan `
  --name java-demoapp-unique-name `
  --deployment-container-image-name "$ACR_LOGIN_SERVER/java-demoapp:latest"

# Configure container registry credentials
$ACR_USERNAME = (az acr credential show --name javademoappacr --query username --output tsv)
$ACR_PASSWORD = (az acr credential show --name javademoappacr --query passwords[0].value --output tsv)

az webapp config container set --name java-demoapp-unique-name `
  --resource-group java-demoapp-rg `
  --docker-custom-image-name "$ACR_LOGIN_SERVER/java-demoapp:latest" `
  --docker-registry-server-url "https://$ACR_LOGIN_SERVER" `
  --docker-registry-server-user $ACR_USERNAME `
  --docker-registry-server-password $ACR_PASSWORD

# Set port configuration
az webapp config appsettings set --name java-demoapp-unique-name `
  --resource-group java-demoapp-rg `
  --settings WEBSITES_PORT=8080

# Browse to the app
az webapp browse --name java-demoapp-unique-name --resource-group java-demoapp-rg
```

### Azure Deployment Options

#### Option A: Deploy to Azure Container Instances (ACI)

```powershell
# Deploy to ACI
az container create --resource-group java-demoapp-rg `
  --name java-demoapp-aci `
  --image "$ACR_LOGIN_SERVER/java-demoapp:latest" `
  --registry-login-server $ACR_LOGIN_SERVER `
  --registry-username $ACR_USERNAME `
  --registry-password $ACR_PASSWORD `
  --dns-name-label java-demoapp-aci-unique `
  --ports 8080 `
  --cpu 1 `
  --memory 2

# Get the FQDN
az container show --resource-group java-demoapp-rg `
  --name java-demoapp-aci `
  --query ipAddress.fqdn --output tsv
```

#### Option B: Deploy to Azure Kubernetes Service (AKS)

```powershell
# Create AKS cluster
az aks create --resource-group java-demoapp-rg `
  --name java-demoapp-aks `
  --node-count 2 `
  --attach-acr javademoappacr `
  --generate-ssh-keys

# Get AKS credentials
az aks get-credentials --resource-group java-demoapp-rg --name java-demoapp-aks

# Deploy to AKS using kubectl (requires kubernetes manifests)
kubectl apply -f kubernetes-deployment.yaml
```

#### Option C: Deploy using Azure Container Apps

```powershell
# Install Azure Container Apps extension
az extension add --name containerapp --upgrade

# Create Container Apps environment
az containerapp env create --name java-demoapp-env `
  --resource-group java-demoapp-rg `
  --location eastus

# Deploy container app
az containerapp create --name java-demoapp `
  --resource-group java-demoapp-rg `
  --environment java-demoapp-env `
  --image "$ACR_LOGIN_SERVER/java-demoapp:latest" `
  --target-port 8080 `
  --ingress external `
  --registry-server $ACR_LOGIN_SERVER `
  --registry-username $ACR_USERNAME `
  --registry-password $ACR_PASSWORD `
  --cpu 1.0 `
  --memory 2.0Gi `
  --min-replicas 0 `
  --max-replicas 5

# Get the app URL
az containerapp show --name java-demoapp `
  --resource-group java-demoapp-rg `
  --query properties.configuration.ingress.fqdn --output tsv
```

### Environment Variables for Azure

Configure Azure AD and Application Insights:

```powershell
# For App Service
az webapp config appsettings set --name java-demoapp-unique-name `
  --resource-group java-demoapp-rg `
  --settings `
    azure_activedirectory_clientid="YOUR_CLIENT_ID" `
    azure_activedirectory_clientsecret="YOUR_SECRET" `
    azure_activedirectory_tenantid="YOUR_TENANT" `
    azure_applicationinsights_instrumentationkey="YOUR_INSIGHTS_KEY"

# For Container Apps
az containerapp update --name java-demoapp `
  --resource-group java-demoapp-rg `
  --set-env-vars `
    azure_activedirectory_clientid="YOUR_CLIENT_ID" `
    azure_activedirectory_clientsecret=secretref:azure-ad-secret `
    azure_activedirectory_tenantid="YOUR_TENANT"
```

### Monitoring and Logs (Azure)

```powershell
# View App Service logs
az webapp log tail --name java-demoapp-unique-name --resource-group java-demoapp-rg

# Enable Application Insights
az monitor app-insights component create `
  --app java-demoapp-insights `
  --location eastus `
  --resource-group java-demoapp-rg

# Get instrumentation key
az monitor app-insights component show `
  --app java-demoapp-insights `
  --resource-group java-demoapp-rg `
  --query instrumentationKey --output tsv

# View Container Apps logs
az containerapp logs show --name java-demoapp `
  --resource-group java-demoapp-rg `
  --follow
```

### Scaling Configuration

```powershell
# Scale App Service
az appservice plan update --name java-demoapp-plan `
  --resource-group java-demoapp-rg `
  --sku P1V2

az webapp config set --name java-demoapp-unique-name `
  --resource-group java-demoapp-rg `
  --always-on true

# Scale Container Apps
az containerapp update --name java-demoapp `
  --resource-group java-demoapp-rg `
  --min-replicas 1 `
  --max-replicas 10 `
  --scale-rule-name http-rule `
  --scale-rule-type http `
  --scale-rule-http-concurrency 50
```

### Cost Management (Azure)

```powershell
# Delete App Service
az webapp delete --name java-demoapp-unique-name --resource-group java-demoapp-rg

# Delete Container App
az containerapp delete --name java-demoapp --resource-group java-demoapp-rg

# Delete entire resource group (WARNING: deletes all resources)
az group delete --name java-demoapp-rg --yes --no-wait
```

## Comparison: GCP vs Azure

| Feature | GCP Cloud Run | Azure Container Apps | Azure App Service |
|---------|---------------|---------------------|-------------------|
| **Pricing Model** | Pay-per-request | Pay-per-use | Fixed pricing tiers |
| **Scale to Zero** | Yes | Yes | No (requires Always On) |
| **Cold Start** | ~1-2 seconds | ~1-2 seconds | N/A |
| **Max Instances** | 1000 | 30 (can increase) | Manual scaling |
| **Custom Domain** | Yes (free SSL) | Yes (free SSL) | Yes (paid SSL on Basic+) |
| **Container Size** | Up to 32 GiB RAM | Up to 4 GiB RAM | Up to 14 GB RAM |
| **Build Integration** | Cloud Build | ACR Tasks | ACR or Docker Hub |
| **Free Tier** | 2M requests/month | Limited compute time | F1 tier available |
| **Best For** | Serverless containers | Microservices | Traditional web apps |

## Troubleshooting

### Common Issues - GCP

1. **Port Configuration**: Ensure `PORT` environment variable is set to 8080
2. **Authentication**: Run `gcloud auth login` if commands fail
3. **API Not Enabled**: Enable required APIs with `gcloud services enable`
4. **Memory Issues**: Increase memory with `--memory 2Gi` flag
5. **Cold Starts**: Set `--min-instances 1` to avoid cold starts

### Common Issues - Azure

1. **Port Binding**: Set `WEBSITES_PORT=8080` for App Service
2. **Registry Authentication**: Verify ACR credentials are correct
3. **Resource Quotas**: Check subscription limits in Azure Portal
4. **Container Startup**: Check logs with `az webapp log tail`
5. **Always On**: Enable for production (paid tiers only)

## DevOps Best Practices

### CI/CD Pipeline Recommendations

1. **Build Stage**
   - Use multi-stage Dockerfiles for smaller images
   - Cache Maven dependencies in Docker layers
   - Run tests in parallel with linting

2. **Security**
   - Scan images for vulnerabilities (Trivy, Snyk)
   - Use managed identities (avoid storing credentials)
   - Rotate secrets regularly
   - Use private container registries

3. **Deployment**
   - Use blue-green or canary deployments
   - Implement health checks and readiness probes
   - Set appropriate resource limits
   - Configure auto-scaling based on metrics

4. **Monitoring**
   - Enable application performance monitoring (APM)
   - Set up alerts for errors and high latency
   - Use structured logging (JSON format)
   - Track key metrics (CPU, memory, request rate)

5. **Cost Optimization**
   - Use scale-to-zero for dev/test environments
   - Right-size compute resources
   - Use spot instances for non-critical workloads
   - Implement request-based scaling

### Infrastructure as Code

For production deployments, consider using:
- **Terraform**: Multi-cloud infrastructure provisioning
- **Pulumi**: Infrastructure as code using programming languages
- **Azure Bicep**: Native Azure infrastructure as code
- **Google Cloud Deployment Manager**: GCP infrastructure automation

Example Terraform snippet for Cloud Run:

```hcl
resource "google_cloud_run_service" "java_demoapp" {
  name     = "java-demoapp"
  location = "us-central1"

  template {
    spec {
      containers {
        image = "us-central1-docker.pkg.dev/PROJECT_ID/java-demoapp/java-demoapp:latest"
        ports {
          container_port = 8080
        }
        resources {
          limits = {
            memory = "1Gi"
            cpu    = "1"
          }
        }
      }
    }
  }
}
```

# Kubernetes

# Optional Features

## Application Insights

If you wish to enable Azure App Insights integration set the `azure_applicationinsights_instrumentationkey` environmental variable to the relevant workspace key

## User Sign-In - Azure Active Directory

The _'Spring Boot Starter for Azure Active Directory'_ is included in the application and the main application nav bar has a 'User' button which when configured will sign users in via Azure AD. This is optional and not required for the app to start and run.

**NOTE.** The Azure AD application must be registered with a reply/redirect URL which ends with `/login/oauth2/code/`, and have implicit grant enabled. The application must also be **granted admin consent** to several Graph APIs, this can limit which tenants the application can be registered in.

See the Azure docs for more details
https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-active-directory

If running locally you can enabled Azure AD authentication by setting the following three environmental variables, before running `make run`

```bash
export azure_activedirectory_clientid='my-client-id'
export azure_activedirectory_clientsecret='my-secret'
export azure_activedirectory_tenantid='my-tenant'
```

# GitHub Actions CI/CD

A working set of CI and CD release GitHub Actions workflows are provided `.github/workflows/`, automated builds are run in GitHub hosted runners

### [GitHub Actions](https://github.com/benc-uk/python-demoapp/actions)

[![](https://img.shields.io/github/workflow/status/benc-uk/java-demoapp/CI%20Build%20App)](https://github.com/benc-uk/java-demoapp/actions?query=workflow%3A%22CI+Build+App%22)

[![](https://img.shields.io/github/workflow/status/benc-uk/java-demoapp/CD%20Release%20-%20AKS?label=release-kubernetes)](https://github.com/benc-uk/java-demoapp/actions?query=workflow%3A%22CD+Release+-+AKS%22)

[![](https://img.shields.io/github/workflow/status/benc-uk/java-demoapp/CD%20Release%20-%20Webapp?label=release-azure)](https://github.com/benc-uk/java-demoapp/actions?query=workflow%3A%22CD+Release+-+Webapp%22)

[![](https://img.shields.io/github/last-commit/benc-uk/java-demoapp)](https://github.com/benc-uk/java-demoapp/commits/master)

# Updates

- Nov 2025 - Upgraded to Java 21 LTS, Spring Boot 3.2.0, added comprehensive GCP and Azure deployment guides
- Mar 2021 - Version bumps, unit tests
- Dec 2019 - First version
