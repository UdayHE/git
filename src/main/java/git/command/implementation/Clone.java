package git.command.implementation;

import git.command.Command;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;

public class Clone implements Command {

    @Override
    public void execute(String[] args) throws Exception {
        // Debug log: print received arguments
        System.out.println("Received arguments count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("Arg[" + i + "]: " + args[i]);
        }

        // Validate arguments
        if (args.length < 3) {
            System.err.println("Error: Insufficient arguments provided.");
            throw new IllegalArgumentException("Usage: clone <repository_url> <destination_directory>");
        }

        String repoUrl = args[1].replaceAll("/$", ""); // Fix: Assign the correct URL
        String destinationDir = args[2];

        // Debugging to ensure URL is correct
        System.out.println("Repository URL: " + repoUrl);
        System.out.println("Destination Directory: " + destinationDir);

        // Create the target directory
        Path targetPath = Paths.get(destinationDir);
        if (Files.exists(targetPath)) {
            throw new IllegalStateException("Error: Directory '" + destinationDir + "' already exists.");
        }
        Files.createDirectories(targetPath);

        // Clone repository by fetching objects
        fetchObjects(repoUrl, targetPath);

        System.out.println("Cloning completed successfully into " + destinationDir);
    }

    private void fetchObjects(String repoUrl, Path destination) throws IOException {
        // Ensure the URL is correctly formed
        String refsUrl = repoUrl + "/info/refs?service=git-upload-pack";
        System.out.println("Fetching refs from: " + refsUrl); // Debugging URL output

        HttpURLConnection connection = (HttpURLConnection) new URL(refsUrl).openConnection();
        connection.setRequestProperty("User-Agent", "Git/2.30");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Pragma", "no-cache");

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch refs: HTTP " + connection.getResponseCode());
        }

        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("refs/heads/main") || line.contains("refs/heads/master")) {
                    String commitHash = line.split(" ")[0];
                    System.out.println("Found HEAD commit: " + commitHash);
                    fetchPackfile(repoUrl, destination, commitHash);
                    return;
                }
            }
        }
        throw new IOException("Could not find main or master branch in repository.");
    }

    private void fetchPackfile(String repoUrl, Path destination, String commitHash) throws IOException {
        // Send a request to download the packfile from the remote repository
        String packUrl = repoUrl + "/git-upload-pack";
        System.out.println("Fetching packfile from: " + packUrl); // Debugging URL output

        HttpURLConnection connection = (HttpURLConnection) new URL(packUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Git/2.30");
        connection.setRequestProperty("Content-Type", "application/x-git-upload-pack-request");
        connection.setDoOutput(true);

        // Request refs from the server
        try (OutputStream output = connection.getOutputStream()) {
            output.write(("0032want " + commitHash + "\n0000").getBytes());
        }

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch packfile: HTTP " + connection.getResponseCode());
        }

        // Process and store packfile
        try (InputStream input = connection.getInputStream()) {
            Path objectsPath = destination.resolve(".git/objects");
            Files.createDirectories(objectsPath);

            byte[] buffer = new byte[8192];
            int bytesRead;
            try (OutputStream fileOut = Files.newOutputStream(objectsPath.resolve(commitHash))) {
                while ((bytesRead = input.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Packfile downloaded and stored.");
        }
    }
}
