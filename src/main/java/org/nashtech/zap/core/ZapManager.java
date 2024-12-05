package org.nashtech.zap.core;


import org.nashtech.zap.config.ZapConfig;
import org.nashtech.zap.exceptions.ZapException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ZapManager {
    private Process zapProcess;
    private ZapConfig config;

    public ZapManager() {
        config = new ZapConfig();
    }

    public void startZap(int zapPort) {
        String zapDockerImage = config.getProperty("zap.docker.image");
        String zapApiKey = config.getProperty("zap.apikey");
        String containerName = config.getProperty("zap.container.name");
        try {
            // Check if the container already exists
            String checkContainerCommand = String.format("docker ps -a -q -f name=%s", containerName);
            String containerId = runCommandAndCaptureOutput(checkContainerCommand).trim();

            // If the container exists, stop and remove it
            if (!containerId.isEmpty()) {
                runCommand("docker stop " + containerName);
                runCommand("docker rm " + containerName);
            }

            // Pull the Docker image if not already available
            runCommand("docker pull " + zapDockerImage);

            // Start ZAP container
            String startCommand = String.format("docker run --name %s -u zap -p %d:8080 -i %s zap.sh -daemon -port 8080 -host 0.0.0.0 " +
                    "-config api.addrs.addr.name=.* -config api.addrs.addr.regex=true -config api.key=%s", containerName, zapPort, zapDockerImage, zapApiKey);
            zapProcess = runCommandAsync(startCommand);

            System.out.println("OWASP ZAP starting on port " + zapPort);
            // Wait for ZAP to be fully initialized (Optional: Implement a check to ensure ZAP is ready)
            Thread.sleep(15000); // Adjust sleep time as needed
        } catch (IOException | InterruptedException e) {
            throw new ZapException("Failed to start OWASP ZAP", e);
        }
    }

    public void stopZap() {
        if (zapProcess != null && zapProcess.isAlive()) {
            try {
                // Step 1: Attempt to stop the ZAP container
                ProcessBuilder stopBuilder = new ProcessBuilder("/bin/sh", "-c",
                        "docker stop $(docker ps -q --filter ancestor=" + config.getProperty("zap.docker.image") + ")");
                Process stopProcess = stopBuilder.start();
                stopProcess.waitFor(); // Wait for the stop command to complete
                // Step 2: Verify if the container is actually stopped
                ProcessBuilder checkBuilder = new ProcessBuilder("/bin/sh", "-c",
                        "docker ps -q --filter ancestor=" + config.getProperty("zap.docker.image"));
                Process checkProcess = checkBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
                String line;
                boolean zapRunning = false;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        zapRunning = true;
                    }
                }
                // Step 3: Print status based on whether ZAP is stopped
                if (!zapRunning) {
                    System.out.println("OWASP ZAP stopped");
                } else {
                    System.out.println("Failed to stop OWASP ZAP: ZAP is still running.");
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to stop OWASP ZAP: " + e);
            }
        }
    }


    public Process runCommand(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        Process process = processBuilder.start();
        String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        System.out.println("Running command : " + command);
        System.out.println(output);
        return process;
    }

    private String runCommandAndCaptureOutput(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        process.waitFor();
        return output.toString();
    }

    public Process runCommandAsync(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        processBuilder.redirectErrorStream(true); // Redirect error stream to output stream

        Process process = processBuilder.start();
        // Read output and error streams asynchronously (if needed)
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Print output lines
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Optionally, read error stream asynchronously (if needed)
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line); // Print error lines
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return process;
    }

    public Process runTrivyCommand(String imageNames, String reportType) throws IOException {
        // Hardcode the command with the shell script and arguments
        String command;
        if(reportType.equalsIgnoreCase("html")){
            command =  "./trivy-execute-html.sh " + imageNames;
        }
        else{
            command =  "./trivy-execute-json.sh " + imageNames;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.directory(new File("trivy")); // Set the working directory
        processBuilder.redirectErrorStream(true);  // Merge error and output streams

        // Start the process
        Process process = processBuilder.start();

        // Read the output in real-time (as the process is running)
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);  // Print output as it's produced
        }

        // Optionally, wait for the process to finish before returning
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return process;
    }


}
