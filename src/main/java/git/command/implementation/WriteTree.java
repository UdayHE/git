package git.command.implementation;

import git.command.Command;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

public class WriteTree implements Command {

    private static final Logger log = Logger.getLogger(WriteTree.class.getName());

    @Override
    public void execute(String[] args) {
        try {
            File currentDir = new File(".");
            String treeHash = writeTree(currentDir);
            System.out.println(treeHash);
        } catch (Exception e) {
            log.severe("Error executing WriteTree: " + e.getMessage());
        }
    }

    private String writeTree(File directory) throws IOException {
        if (!directory.isDirectory()) return null;

        List<byte[]> entries = new ArrayList<>();

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.getName().equals(".git")) continue; // Ignore .git directory

            String hash = file.isFile() ? hashAndStoreBlob(file) : (file.isDirectory() ? writeTree(file) : null);
            if (hash != null) {
                String mode = file.isFile() ? "100644" : "40000";
                entries.add(serializeEntry(mode, file.getName(), hash));
            }
        }

        // Sort entries using raw bytes
        entries.sort(Comparator.comparingInt(this::getNameStart));

        // Compute the tree object byte size
        int totalSize = entries.stream().mapToInt(e -> e.length).sum();
        byte[] header = ("tree " + totalSize + "\0").getBytes();

        // Merge header and entries
        try (ByteArrayOutputStream treeStream = new ByteArrayOutputStream()) {
            treeStream.write(header);
            for (byte[] entry : entries) {
                treeStream.write(entry);
            }
            byte[] treeData = treeStream.toByteArray();

            // Compute hash and store tree object
            String treeHash = computeSHA1(treeData);
            storeObject(treeHash, treeData);
            return treeHash;
        }
    }

    private int getNameStart(byte[] entry) {
        int spaceIndex = indexOf(entry, ' ');
        return indexOf(entry, 0, spaceIndex + 1); // Index of the null byte
    }

    private byte[] serializeEntry(String mode, String name, String hash) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write((mode + " " + name + "\0").getBytes());
            output.write(hexToBinary(hash)); // Convert hash to binary
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing entry", e);
        }
    }

    private String hashAndStoreBlob(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] header = ("blob " + content.length + "\0").getBytes();

        try (ByteArrayOutputStream blobStream = new ByteArrayOutputStream()) {
            blobStream.write(header);
            blobStream.write(content);
            byte[] blobData = blobStream.toByteArray();

            String blobHash = computeSHA1(blobData);
            storeObject(blobHash, blobData);
            return blobHash;
        }
    }

    private String computeSHA1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(data);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error computing SHA-1 hash", e);
        }
    }

    private void storeObject(String hash, byte[] data) throws IOException {
        String objectDir = ".git/objects/" + hash.substring(0, 2);
        String objectPath = objectDir + "/" + hash.substring(2);

        Files.createDirectories(Paths.get(objectDir));

        try (OutputStream out = new DeflaterOutputStream(new FileOutputStream(objectPath))) {
            out.write(data);
        }
    }

    private byte[] hexToBinary(String hex) {
        byte[] binary = new byte[20];
        for (int i = 0; i < 20; i++) {
            binary[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return binary;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private int indexOf(byte[] array, char value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(byte[] array, int fromIndex, int endIndex) {
        for (int i = fromIndex; i < endIndex; i++) {
            if (array[i] == 0) {
                return i;
            }
        }
        return endIndex; // Return endIndex if not found
    }
}
