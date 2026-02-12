Directory containing services and apps for feed solution

# Local Development Setup

1. Create local kubernetes cluster  
   i. kind

   ```
   kind create cluster --name feed-cluster
   ```

2. Execute build and deploy script
   ```
   chmod +x ./build-deploy-script.sh
   ./build-deploy-script.sh
   ```
