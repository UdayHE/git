package git.command;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;
import java.util.logging.Logger;

public class HashObject implements Command {

    private static final Logger log = Logger.getLogger(HashObject.class.getName());

    private static final String ARG = "-w";
    private static final String SHA_1 = "SHA-1";
    private static final String GIT_OBJECTS_PATH = ".git/objects/";
    private static final String FORWARD_SLASH = "/";
    private static final String BLOB = "blob ";
    private static final String NULL_CHAR = "\0";
    private static final String PADDING = "%02x";

    @Override
    public void execute(String[] args) {
        if (args.length < 2 || !ARG.equals(args[0])) {
            log.log(Level.SEVERE, "Usage: hash-object -w <file>");
            return;
        }

        String filePath = args[1];
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String hash = computeHashAndStore(fileContent);
            System.out.println(hash);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error reading file: {0}", e.getMessage());
        }
    }

    private String computeHashAndStore(byte[] content) throws IOException {
        // Create the Git blob header: "blob <size>\0"
        String header = BLOB + content.length + NULL_CHAR;
        byte[] headerBytes = header.getBytes();

        // Concatenate header and file content
        byte[] fullContent = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
        System.arraycopy(content, 0, fullContent, headerBytes.length, content.length);

        // Compute SHA-1 hash of the full content
        String sha1 = computeSHA1(fullContent);

        // Store the compressed object in .git/objects/<first 2 chars>/<remaining 38 chars>
        storeObject(sha1, fullContent);

        return sha1;
    }

    private String computeSHA1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_1);
            byte[] hashBytes = md.digest(data);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error computing SHA-1 hash", e);
        }
    }

    private void storeObject(String hash, byte[] data) throws IOException {
        String objectDir = GIT_OBJECTS_PATH + hash.substring(0, 2);
        String objectPath = objectDir + FORWARD_SLASH + hash.substring(2);

        // Ensure the .git/objects directory exists
        Files.createDirectories(Paths.get(objectDir));

        // Compress and store the object
        try (OutputStream out = new DeflaterOutputStream(new FileOutputStream(objectPath))) {
            out.write(data);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes)
            hexString.append(String.format(PADDING, b));
        return hexString.toString();
    }
}
