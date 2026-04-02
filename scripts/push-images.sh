#!/usr/bin/env bash
# Builds every service image and pushes it to ECR.
# Usage: scripts/push-images.sh <registry> [tag] [region]
#   registry: e.g. 123456789012.dkr.ecr.us-east-1.amazonaws.com (terraform output ecr_registry)
set -euo pipefail

REGISTRY=${1:?usage: push-images.sh <registry> [tag] [region]}
TAG=${2:-latest}
REGION=${3:-us-east-1}
PREFIX=${NAME_PREFIX:-tickettide}
SERVICES=(auth-service event-service ticket-service payment-service notification-service)

cd "$(dirname "$0")/.."

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

for svc in "${SERVICES[@]}"; do
  image="$REGISTRY/$PREFIX/$svc:$TAG"
  echo "==> $image"
  docker build --platform linux/amd64 --build-arg SERVICE="$svc" -t "$image" .
  docker push "$image"
done
