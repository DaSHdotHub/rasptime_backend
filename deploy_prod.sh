#!/bin/bash
set -e  # Exit on any error

APP_DIR=~/projects/rasptime_backend
SERVICE_NAME=rasptime
PROFILE=prod
PORT=8081

cd "$APP_DIR"

echo "=========================================="
echo "Deploying Rasptime Backend (${PROFILE})"
echo "=========================================="

echo ""
echo "[1/5] Pulling latest changes..."
git pull

echo ""
echo "[2/5] Building..."
./mvnw clean package -DskipTests -Dspring.profiles.active=${PROFILE}

echo ""
echo "[3/5] Restarting service..."
sudo systemctl restart ${SERVICE_NAME}

echo ""
echo "[4/5] Waiting for startup..."
sleep 30

echo ""
echo "[5/5] Health check..."
if curl -sf http://localhost:${PORT}/api/health > /dev/null; then
    echo "✓ Application is healthy"
    curl -s http://localhost:${PORT}/api/health | jq . 2>/dev/null || curl -s http://localhost:8080/api/health
else
    echo "✗ Health check failed!"
    echo ""
    echo "Recent logs:"
    sudo journalctl -u ${SERVICE_NAME} -n 20 --no-pager
    exit 1
fi

echo ""
echo "=========================================="
echo "Deployment complete"
echo "=========================================="

