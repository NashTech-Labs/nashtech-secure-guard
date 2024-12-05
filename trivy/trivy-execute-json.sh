#!/usr/bin/env bash

# Function to check if a command executed successfully
check_command() {
    if [ $? -ne 0 ]; then
        echo "Error executing the previous command!"
        exit 1
    fi
}

# Ensure Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if at least one image name was provided as an argument
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <docker-image-name-1> [docker-image-name-2] [...]"
    exit 1
fi

# Set the root directory of the project (you can adjust this if needed)
ROOT_DIR="$(pwd)"  # or specify a path like /path/to/your/project

# Check if a container named 'trivy' already exists and remove it
EXISTING_CONTAINER=$(docker ps -aq -f name=trivy)
if [ -n "$EXISTING_CONTAINER" ]; then
    echo "Removing existing 'trivy' container..."
    docker rm -f "$EXISTING_CONTAINER"
    check_command
fi

# Start Trivy container to download the vulnerability database
echo "Starting Trivy service to download the vulnerability database..."
docker-compose up -d trivy
check_command

echo "Waiting for the Trivy service to initialize..."
sleep 10  # Allow time for the database download (adjust if needed)

# Initialize a variable to store the combined report in JSON format
COMBINED_REPORT_JSON="${ROOT_DIR}/trivy-report-combined.json"

# Start the JSON array for the report
echo "[" > "$COMBINED_REPORT_JSON"

# Loop through all provided Docker image names
for IMAGE_NAME in "$@"; do
    echo "Scanning Docker image: $IMAGE_NAME"

    # Run Trivy scan and save the report as a JSON file
    REPORT_JSON="${ROOT_DIR}/trivy-report-${IMAGE_NAME//\//_}.json"
    docker run --rm \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v trivy-cache:/root/.cache/trivy \
        aquasec/trivy:latest image --format json "$IMAGE_NAME" > "$REPORT_JSON"

    check_command
    echo "Trivy scan completed successfully for $IMAGE_NAME. Report saved as $REPORT_JSON."

    # Add the content of the JSON report to the combined JSON
    # Read the JSON report and append it to the combined report
    cat "$REPORT_JSON" >> "$COMBINED_REPORT_JSON"

    # If it's not the last image, add a comma to separate JSON objects
    if [ "$IMAGE_NAME" != "${!#}" ]; then
        echo "," >> "$COMBINED_REPORT_JSON"
    fi

    # Delete the individual JSON report after appending
    rm "$REPORT_JSON"

done

# Close the JSON array
echo "]" >> "$COMBINED_REPORT_JSON"

echo "Combined JSON report saved as $COMBINED_REPORT_JSON."
echo "All scans completed. Combined report generated."
