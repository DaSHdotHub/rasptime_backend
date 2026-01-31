#!/bin/bash
cd ~/projects/rasptime_backend

echo "Pulling latest changes..."
git pull

echo "Building..."
mvn clean package -DskipTests

echo "Restarting service..."
sudo systemctl restart rasptime

echo "Status:"
sudo systemctl status rasptime --no-pager