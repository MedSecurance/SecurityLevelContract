#!/bin/bash
# Abort the script if a command fails
set -e

echo "Starting MinIO configuration script..."
minio server /data --console-address ":9001" &
MINIO_PID=$!
sleep 30
echo "MinIO should have been started..."

# We use a loop to retry the connection until the server is available
until mc alias set local http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"; do
    echo "Retrying in 2 seconds..."
    sleep 2
done
echo "MinIO is ready."

# Create the bucket if it doesn't exist
echo "Creating bucket: $MINIO_BUCKET_NAME"
mc mb "local/$MINIO_BUCKET_NAME" --ignore-existing
echo "Created bucket: $MINIO_BUCKET_NAME"

# Create the user if it doesn't exist
# We use '|| true' to prevent the script from failing if the user already exists
echo "Creating user: $MINIO_ACCESS_NAME"
mc admin user add local "$MINIO_ACCESS_NAME" "$MINIO_ACCESS_SECRET"
echo "Created user: $MINIO_ACCESS_NAME"

# Assign the read/write policy to the user
echo "Assigning policy to $MINIO_ACCESS_NAME"
mc admin policy attach local readwrite --user "$MINIO_ACCESS_NAME"
echo "Policy assigned to $MINIO_ACCESS_NAME"

echo "MinIO configuration completed."

