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

# Start Trivy container to download the vulnerability database
echo "Starting Trivy service to download the vulnerability database..."
docker-compose up -d trivy
check_command

echo "Waiting for the Trivy service to initialize..."
sleep 10  # Allow time for the database download (adjust if needed)

# Initialize a variable to store the combined report HTML content
COMBINED_REPORT_HTML="${ROOT_DIR}/trivy-report-combined.html"
echo "<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Trivy Combined Vulnerability Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; color: #333; }
        h1 { text-align: center; color: #0044cc; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        table, th, td { border: 1px solid #ddd; }
        th, td { padding: 10px; text-align: left; }
        th { background-color: #f4f4f4; color: #000; }
        td { background-color: #fafafa; }
        pre { background-color: #f5f5f5; padding: 10px; white-space: pre-wrap; word-wrap: break-word; }
    </style>
</head>
<body>
<h1>Trivy Combined Vulnerability Report</h1>" > "$COMBINED_REPORT_HTML"

# Loop through all provided Docker image names
for IMAGE_NAME in "$@"; do
    echo "Scanning Docker image: $IMAGE_NAME"

    # Run Trivy scan and save the report as a text file
    REPORT_TXT="${ROOT_DIR}/trivy-report-${IMAGE_NAME//\//_}.txt"
    docker run --rm \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v trivy-cache:/root/.cache/trivy \
        aquasec/trivy:latest image --format table "$IMAGE_NAME" > "$REPORT_TXT"

    check_command
    echo "Trivy scan completed successfully for $IMAGE_NAME. Report saved as $REPORT_TXT."

    # Add the content of the text report to the combined HTML
    echo "<h2>Vulnerabilities Found for $IMAGE_NAME</h2>" >> "$COMBINED_REPORT_HTML"
    echo "<pre>" >> "$COMBINED_REPORT_HTML"
    cat "$REPORT_TXT" >> "$COMBINED_REPORT_HTML"
    echo "</pre>" >> "$COMBINED_REPORT_HTML"

    # Delete the text report after adding to the combined HTML
    rm "$REPORT_TXT"

done

# Close the HTML tags for the combined report
echo "</body></html>" >> "$COMBINED_REPORT_HTML"

echo "Combined HTML report saved as $COMBINED_REPORT_HTML."
echo "All scans completed. Combined report generated."

