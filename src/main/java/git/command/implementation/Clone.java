package git.command.implementation;

import git.command.Command;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.DeflaterOutputStream;

public class Clone implements Command {

    @Override
    public void execute(String[] args) throws Exception {
        System.out.println("Received arguments count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("Arg[" + i + "]: " + args[i]);
        }

        if (args.length < 3) {
            System.err.println("Error: Insufficient arguments provided.");
            throw new IllegalArgumentException("Usage: clone <repository_url> <destination_directory>");
        }

        String repoUrl = args[1].replaceAll("/$", "");
        String destinationDir = args[2];

        System.out.println("Repository URL: " + repoUrl);
        System.out.println("Destination Directory: " + destinationDir);

        Path targetPath = Paths.get(destinationDir);
        if (Files.exists(targetPath)) {
            throw new IllegalStateException("Error: Directory '" + destinationDir + "' already exists.");
        }
        Files.createDirectories(targetPath);

        setupGitStructure(targetPath);
        fetchObjects(repoUrl, targetPath);
        System.out.println("Cloning completed successfully into " + destinationDir);
    }

    private void setupGitStructure(Path repoPath) throws IOException {
        Path gitPath = repoPath.resolve(".git");
        Files.createDirectories(gitPath);
        Files.createDirectories(gitPath.resolve("objects"));
        Files.createDirectories(gitPath.resolve("refs/heads"));

        Files.write(gitPath.resolve("HEAD"), "ref: refs/heads/main\n".getBytes());
    }

    private void fetchObjects(String repoUrl, Path destination) throws IOException {
        String refsUrl = repoUrl + "/info/refs?service=git-upload-pack";
        System.out.println("Fetching refs from: " + refsUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(refsUrl).openConnection();
        connection.setRequestProperty("User-Agent", "Git/2.30");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Pragma", "no-cache");

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch refs: HTTP " + connection.getResponseCode());
        }

        String commitHash = null;
        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("refs/heads/main") || line.contains("refs/heads/master")) {
                    commitHash = line.split(" ")[0];
                    System.out.println("Found HEAD commit: " + commitHash);
                    break;
                }
            }
        }

        if (commitHash == null) {
            throw new IOException("Could not find main or master branch in repository.");
        }

        Files.write(destination.resolve(".git/refs/heads/main"), commitHash.getBytes());

        fetchPackfile(repoUrl, destination, commitHash);
    }

    private void fetchPackfile(String repoUrl, Path destination, String commitHash) throws IOException {
        String packUrl = repoUrl + "/git-upload-pack";
        System.out.println("Fetching packfile from: " + packUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(packUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Git/2.30");
        connection.setRequestProperty("Content-Type", "application/x-git-upload-pack-request");
        connection.setDoOutput(true);

        try (OutputStream output = connection.getOutputStream()) {
            // Correctly formatted request for packfile negotiation
            output.write(("0024want " + commitHash + " multi_ack ofs-delta\n0000").getBytes());
        }

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch packfile: HTTP " + connection.getResponseCode());
        }

        // ✅ Read raw packfile instead of InflaterInputStream
        try (InputStream input = connection.getInputStream()) {
            Path objectsPath = destination.resolve(".git/objects");
            Files.createDirectories(objectsPath);

            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream packData = new ByteArrayOutputStream();
            while ((bytesRead = input.read(buffer)) != -1) {
                packData.write(buffer, 0, bytesRead);
            }

            if (packData.size() == 0) {
                throw new IOException("Received empty packfile. Possible server error or missing objects.");
            }

            processPackfile(destination, packData.toByteArray());
        }
    }

    private void processPackfile(Path destination, byte[] packData) throws IOException {
        Path objectsPath = destination.resolve(".git/objects");

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(packData);
             DataInputStream dataInputStream = new DataInputStream(inputStream)) {

            byte[] header = new byte[4];
            dataInputStream.readFully(header);
            String headerStr = new String(header);
            if (!"PACK".equals(headerStr)) {
                throw new IOException("Invalid packfile format: missing PACK header.");
            }

            // Read version and number of objects
            int version = dataInputStream.readInt();
            int objectCount = dataInputStream.readInt();
            System.out.println("Packfile Version: " + version + ", Objects: " + objectCount);

            if (objectCount <= 0) {
                throw new IOException("Packfile contains zero objects.");
            }

            for (int i = 0; i < objectCount; i++) {
                if (dataInputStream.available() < 2) {
                    throw new EOFException("Unexpected end of packfile while reading objects.");
                }

                byte typeAndSize = dataInputStream.readByte();
                int type = (typeAndSize >> 4) & 0b111;
                int size = typeAndSize & 0b1111;

                ByteArrayOutputStream objectData = new ByteArrayOutputStream();
                for (int j = 0; j < size; j++) {
                    if (dataInputStream.available() == 0) {
                        throw new EOFException("Unexpected end of file while reading object data.");
                    }
                    objectData.write(dataInputStream.readByte());
                }

                String objectHash = computeSHA1(objectData.toByteArray());
                storeObject(objectsPath, objectHash, objectData.toByteArray());

                System.out.println("Stored Git object: " + objectHash + " (Type: " + type + ")");
            }
        }
    }

    private void storeObject(Path objectsPath, String hash, byte[] data) throws IOException {
        Path objectDir = objectsPath.resolve(hash.substring(0, 2));
        Files.createDirectories(objectDir);
        Path objectFile = objectDir.resolve(hash.substring(2));

        try (OutputStream fileOut = Files.newOutputStream(objectFile);
             DeflaterOutputStream deflaterOut = new DeflaterOutputStream(fileOut)) {
            deflaterOut.write(data);
        }
    }

    private String computeSHA1(byte[] data) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("SHA-1 computation failed", e);
        }
    }
}
