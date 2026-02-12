#!/bin/bash

podman build -t localhost/postservice:dev ./postservice
podman save -o /tmp/postservice-dev.tar localhost/postservice:dev

kind load image-archive /tmp/postservice-dev.tar --name feed-cluster

kubectl apply -k k8s/

# Since the image tag stays the same, kubectl apply may not recreate pods. A rollout restart forces the pod to pick up the newly loaded local image.
kubectl -n feed rollout restart deployment/postservice