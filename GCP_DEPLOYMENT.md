# Deploying Java Monitoring Project to Google Cloud Platform

This guide provides step-by-step instructions for deploying the Java Demo Application to Google Cloud Platform using different deployment options.

## Prerequisites

1. **Google Cloud Account**: Sign up at [https://cloud.google.com](https://cloud.google.com)
2. **Google Cloud SDK**: Install from [https://cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install)
3. **Docker** (for local testing): Install from [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)
4. **Java 17** and **Maven** (for local builds)

## Initial Setup

### 1. Install and Initialize Google Cloud SDK

```powershell
# Initialize gcloud
gcloud init

# Login to your Google account
gcloud auth login

# Set your project ID (replace with your actual project ID)
gcloud config set project YOUR_PROJECT_ID

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
gcloud services enable appengine.googleapis.com
```

### 2. Set Environment Variables

```powershell
# Set your GCP project ID
$env:PROJECT_ID = "your-project-id"
gcloud config set project $env:PROJECT_ID
```

## Deployment Options

### Option 1: Cloud Run (Recommended for Containers)

Cloud Run is a fully managed serverless platform for containerized applications. It automatically scales based on traffic.

#### Deploy to Cloud Run

```powershell
# Build and deploy in one command
gcloud run deploy java-demoapp `
  --source . `
  --region us-central1 `
  --platform managed `
  --allow-unauthenticated `
  --port 8080

# Or build container first and then deploy
docker build -t gcr.io/$env:PROJECT_ID/java-demoapp:latest .
docker push gcr.io/$env:PROJECT_ID/java-demoapp:latest

gcloud run deploy java-demoapp `
  --image gcr.io/$env:PROJECT_ID/java-demoapp:latest `
  --region us-central1 `
  --platform managed `
  --allow-unauthenticated `
  --port 8080
```

#### Configure Cloud Run Settings

```powershell
# Update with specific memory and CPU
gcloud run deploy java-demoapp `
  --image gcr.io/$env:PROJECT_ID/java-demoapp:latest `
  --region us-central1 `
  --platform managed `
  --allow-unauthenticated `
  --memory 512Mi `
  --cpu 1 `
  --min-instances 0 `
  --max-instances 5 `
  --port 8080
```

#### Set Environment Variables (Optional)

```powershell
gcloud run deploy java-demoapp `
  --image gcr.io/$env:PROJECT_ID/java-demoapp:latest `
  --region us-central1 `
  --set-env-vars "azure_activedirectory_clientid=YOUR_CLIENT_ID" `
  --set-env-vars "azure_activedirectory_clientsecret=YOUR_SECRET" `
  --set-env-vars "azure_activedirectory_tenantid=YOUR_TENANT"
```

#### Get Service URL

```powershell
gcloud run services describe java-demoapp --region us-central1 --format 'value(status.url)'
```

### Option 2: App Engine (Standard Environment)

App Engine is a fully managed platform for building scalable web applications.

#### Deploy to App Engine

```powershell
# Initialize App Engine (first time only)
gcloud app create --region=us-central

# Deploy the application
gcloud app deploy app.yaml

# Open in browser
gcloud app browse
```

#### View Logs

```powershell
gcloud app logs tail -s default
```

### Option 3: Google Kubernetes Engine (GKE)

For more control and Kubernetes orchestration.

#### Create GKE Cluster

```powershell
# Create a small cluster
gcloud container clusters create java-demoapp-cluster `
  --num-nodes=2 `
  --machine-type=e2-medium `
  --region=us-central1

# Get credentials
gcloud container clusters get-credentials java-demoapp-cluster --region=us-central1
```

#### Build and Push Container

```powershell
# Build the Docker image
docker build -t gcr.io/$env:PROJECT_ID/java-demoapp:latest .

# Configure Docker for GCR
gcloud auth configure-docker

# Push to Google Container Registry
docker push gcr.io/$env:PROJECT_ID/java-demoapp:latest
```

#### Deploy to GKE

Create `kubernetes-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-demoapp
spec:
  replicas: 2
  selector:
    matchLabels:
      app: java-demoapp
  template:
    metadata:
      labels:
        app: java-demoapp
    spec:
      containers:
      - name: java-demoapp
        image: gcr.io/YOUR_PROJECT_ID/java-demoapp:latest
        ports:
        - containerPort: 8080
        env:
        - name: PORT
          value: "8080"
---
apiVersion: v1
kind: Service
metadata:
  name: java-demoapp-service
spec:
  type: LoadBalancer
  selector:
    app: java-demoapp
  ports:
  - port: 80
    targetPort: 8080
```

Deploy:

```powershell
kubectl apply -f kubernetes-deployment.yaml
kubectl get service java-demoapp-service
```

### Option 4: Using Cloud Build (CI/CD)

Automated builds and deployments using `cloudbuild.yaml`.

#### Trigger Manual Build

```powershell
gcloud builds submit --config cloudbuild.yaml .
```

#### Set Up Automated Builds from GitHub

```powershell
# Connect your GitHub repository
gcloud builds triggers create github `
  --repo-name=Java-Monitoring-Project `
  --repo-owner=YOUR_GITHUB_USERNAME `
  --branch-pattern="^main$" `
  --build-config=cloudbuild.yaml
```

## Testing Your Deployment

After deployment, test your application:

```powershell
# For Cloud Run or App Engine - get the URL and open in browser
# Test the endpoints
curl https://your-service-url.run.app/
curl https://your-service-url.run.app/info
curl https://your-service-url.run.app/tools
```

## Monitoring and Logs

### View Logs in Cloud Console

```powershell
# Cloud Run logs
gcloud run services logs read java-demoapp --region us-central1

# App Engine logs
gcloud app logs tail -s default

# Cloud Build logs
gcloud builds log --stream
```

### Access Cloud Console

- **Cloud Run**: https://console.cloud.google.com/run
- **App Engine**: https://console.cloud.google.com/appengine
- **GKE**: https://console.cloud.google.com/kubernetes
- **Cloud Build**: https://console.cloud.google.com/cloud-build

## Cost Management

### Estimate Costs

- **Cloud Run**: Free tier includes 2 million requests/month, pay-per-use after that
- **App Engine**: Free tier available, standard instances are billed per hour
- **GKE**: Charged for cluster and node usage (no free tier for production)

### Clean Up Resources

```powershell
# Delete Cloud Run service
gcloud run services delete java-demoapp --region us-central1

# Delete App Engine version (cannot delete default)
gcloud app versions list
gcloud app versions delete VERSION_ID

# Delete GKE cluster
gcloud container clusters delete java-demoapp-cluster --region=us-central1

# Delete container images
gcloud container images list
gcloud container images delete gcr.io/$env:PROJECT_ID/java-demoapp:latest
```

## Troubleshooting

### Common Issues

1. **Port Configuration**: Ensure the application listens on the port specified by the `PORT` environment variable (defaults to 8080)

2. **Authentication Issues**: For Azure AD integration, update the redirect URLs in Azure AD to include your GCP deployment URL

3. **Memory Issues**: If the app crashes with OOM errors, increase memory allocation:
   ```powershell
   gcloud run deploy java-demoapp --memory 1Gi --region us-central1
   ```

4. **Build Failures**: Check Cloud Build logs:
   ```powershell
   gcloud builds list
   gcloud builds log BUILD_ID
   ```

5. **Container Registry Access**: Ensure Cloud Build service account has permissions:
   ```powershell
   gcloud projects add-iam-policy-binding $env:PROJECT_ID `
     --member="serviceAccount:PROJECT_NUMBER@cloudbuild.gserviceaccount.com" `
     --role="roles/run.admin"
   ```

## Additional Configuration

### Custom Domain

```powershell
# Map a custom domain to Cloud Run
gcloud run domain-mappings create --service java-demoapp --domain yourdomain.com --region us-central1
```

### HTTPS/SSL

Cloud Run and App Engine automatically provision SSL certificates for your service.

### Scaling Configuration

```powershell
# Cloud Run autoscaling
gcloud run services update java-demoapp `
  --min-instances 1 `
  --max-instances 10 `
  --region us-central1
```

## Best Practices

1. **Use Cloud Build** for automated CI/CD pipelines
2. **Enable Cloud Monitoring** for application observability
3. **Set up alerts** for errors and high resource usage
4. **Use Secret Manager** for sensitive configuration (API keys, credentials)
5. **Implement health checks** for better reliability
6. **Use Cloud CDN** for static content delivery
7. **Enable Cloud Armor** for DDoS protection

## Support and Resources

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [App Engine Documentation](https://cloud.google.com/appengine/docs)
- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Cloud Build Documentation](https://cloud.google.com/build/docs)
- [GCP Free Tier](https://cloud.google.com/free)

## Quick Start Commands

For the fastest deployment to Cloud Run:

```powershell
# One command deployment
gcloud run deploy java-demoapp --source . --region us-central1 --allow-unauthenticated
```

That's it! Your Java application should now be running on Google Cloud Platform. 🚀
