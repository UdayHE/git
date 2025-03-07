package git.command.implementation;

import git.command.Command;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Clone implements Command {

    private static final Logger log = Logger.getLogger(Clone.class.getName());

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length != 3) {
            log.log(Level.SEVERE, "Usage: clone <repository_url> <destination_directory>");
            return;
        }

        String repoUrl = args[1];
        String destinationDir = args[2];

        log.log(Level.INFO, "Cloning repository: {0}", repoUrl);
        log.log(Level.INFO, "Destination: {0}", destinationDir);

        File repoDirectory = new File(destinationDir);

        if (repoDirectory.exists()) {
            log.log(Level.SEVERE, "Error: Directory already exists.");
            return;
        }

        clone(repoUrl, repoDirectory);
    }

    private void clone(String repoUrl, File repoDirectory) {
        try {
            String zipUrl = getGitHubZipUrl(repoUrl);
            File tempZip = File.createTempFile("repo", ".zip");
            tempZip.deleteOnExit();

            log.log(Level.INFO, "Downloading repository zip from: {0}", zipUrl);
            downloadFile(zipUrl, tempZip);

            log.log(Level.INFO, "Extracting repository...");
            extractZip(tempZip, repoDirectory);

            log.log(Level.INFO, "Repository successfully cloned into: {0}", repoDirectory.getAbsolutePath());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error: Cloning failed - {0}", e.getMessage());
        }
    }

    private String getGitHubZipUrl(String repoUrl) throws IllegalArgumentException {
        if (!repoUrl.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Unsupported repository URL: " + repoUrl);
        }
        String[] parts = repoUrl.replace(".git", "").split("/");
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
        }
        String owner = parts[3];
        String repo = parts[4];
        return "https://codeload.github.com/" + owner + "/" + repo + "/zip/main";
    }

    private void downloadFile(String fileURL, File destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void extractZip(File zipFile, File destinationDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                File newFile = new File(destinationDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(newFile)) {
                        Files.copy(zipIn, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                zipIn.closeEntry();
            }
        }
    }
}