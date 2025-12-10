#!/bin/sh
# Abort the script if a command fails
set -e

# Start the MinIO server in the background using the original entrypoint
/usr/bin/docker-entrypoint.sh "$@" &
minio_pid=$!
echo "Started MinIO server with PID: $minio_pid"

echo "Waiting for MinIO server to start..."
sleep 30
echo "Wait for MinIO server should be over..."
# Wait for the MinIO server to be ready

# We use a loop to retry the connection until the server is available
until mc alias set local http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"; do
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

# Wait for the MinIO process to finish to keep the container running
wait $minio_pid

