package git.command.implementation;

import git.command.Command;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

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
            if (!repoDirectory.mkdirs()) {
                throw new IOException("Failed to create directory: " + repoDirectory.getAbsolutePath());
            }

            log.log(Level.INFO, "Fetching repository objects...");
            downloadGitObjects(repoUrl, repoDirectory);
            log.log(Level.INFO, "Checking out files...");
            checkoutFiles(repoDirectory);
            log.log(Level.INFO, "Repository successfully cloned into: {0}", repoDirectory.getAbsolutePath());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error: Cloning failed - {0}", e.getMessage());
        }
    }

    private void downloadGitObjects(String repoUrl, File repoDirectory) throws IOException {
        String gitDir = repoDirectory.getAbsolutePath() + "/.git";
        new File(gitDir).mkdirs();

        // 1. Get initial refs information
        String uploadPackUrl = repoUrl + "/info/refs?service=git-upload-pack";
        String infoRefs = downloadAsString(uploadPackUrl);

        // 2. Parse refs to find HEAD commit
        String headCommit = parseHeadCommit(infoRefs);

        // 3. Request packfile containing needed objects
        String packfileUrl = repoUrl + "/git-upload-pack";
        String requestBody = "0032want " + headCommit + "\n00000009done\n";
        byte[] packfile = postRequest(packfileUrl, requestBody.getBytes());

        // 4. Process packfile and write objects
        processPackfile(packfile, new File(gitDir, "objects"));
    }

    private byte[] postRequest(String urlString, byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-git-upload-pack-request");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body);
        }

        return connection.getInputStream().readAllBytes();
    }

    private String parseHeadCommit(String infoRefs) {
        // Parse the info/refs response to find HEAD commit
        Pattern pattern = Pattern.compile("^(.*)\\srefs/heads/main$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(infoRefs);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Could not find HEAD commit");
    }


    private void checkoutFiles(File repoDirectory) throws IOException {
        File headFile = new File(repoDirectory, ".git/HEAD");
        if (!headFile.exists()) {
            throw new IOException("Missing HEAD file, repository might be incomplete");
        }

        String headRef = new String(Files.readAllBytes(headFile.toPath())).trim();
        Pattern pattern = Pattern.compile("ref: (.+)");
        Matcher matcher = pattern.matcher(headRef);
        if (!matcher.find()) {
            throw new IOException("Invalid HEAD reference");
        }
        String branchRef = matcher.group(1);
        File refFile = new File(repoDirectory, ".git/" + branchRef);
        if (!refFile.exists()) {
            throw new IOException("Missing reference file: " + branchRef);
        }
        String commitHash = new String(Files.readAllBytes(refFile.toPath())).trim();
        log.log(Level.INFO, "Checked out commit: {0}", commitHash);
    }

    private String downloadAsString(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        if (connection.getResponseCode() != 200) {
            throw new IOException("HTTP error: " + connection.getResponseCode());
        }

        try (InputStream in = connection.getInputStream();
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
    }

    private void processPackfile(byte[] packfileData, File repoDirectory) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packfileData))) {
            // Verify packfile signature "PACK"
            byte[] signature = new byte[4];
            dis.readFully(signature);
            if (!Arrays.equals(signature, "PACK".getBytes())) {
                throw new IOException("Invalid packfile signature");
            }

            // Read version and number of objects
            int version = Integer.reverseBytes(dis.readInt());
            int numObjects = Integer.reverseBytes(dis.readInt());

            for (int i = 0; i < numObjects; i++) {
                // Read variable-length header
                int header = readPackObjectHeader(dis);
                int type = (header >> 4) & 0x07;
                long size = header & 0x0F;
                int shift = 4;
                while ((header & 0x80) != 0) {
                    header = dis.readUnsignedByte();
                    size |= (header & 0x7F) << shift;
                    shift += 7;
                }

                // Decompress object data
                try (InflaterInputStream inflater = new InflaterInputStream(dis)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] temp = new byte[1024];
                    int len;
                    while ((len = inflater.read(temp)) > 0) {
                        buffer.write(temp, 0, len);
                    }
                    byte[] objectData = buffer.toByteArray();

                    // Create object hash
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    String headerStr = type + " " + size + "\0";
                    sha1.update(headerStr.getBytes());
                    sha1.update(objectData);
                    String hash = bytesToHex(sha1.digest());

                    // Write to objects directory
                    File objectDir = new File(repoDirectory, ".git/objects/" + hash.substring(0, 2));
                    objectDir.mkdirs();
                    Files.write(new File(objectDir, hash.substring(2)).toPath(), objectData);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Missing SHA-1 algorithm", e);
        }
    }

    private int readPackObjectHeader(DataInputStream dis) throws IOException {
        int header = 0;
        int shift = 0;
        int b;
        do {
            b = dis.readUnsignedByte();
            header |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return header;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

}