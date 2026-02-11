#!/bin/bash

podman build -t localhost/rideservice:dev ./rideservice
podman save -o /tmp/rideservice-dev.tar localhost/rideservice:dev

kind load image-archive /tmp/rideservice-dev.tar --name ride-hailing-cluster

kubectl apply -k k8s/

# Since the image tag stays the same, kubectl apply may not recreate pods. A rollout restart forces the pod to pick up the newly loaded local image.
kubectl -n ride-hailing rollout restart deployment/rideservice