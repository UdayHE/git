package git.command.implementation;

import git.command.Command;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;

public class WriteTree implements Command {


    private static final String OBJECTS_PATH = ".git/objects/";
    private static final String FORWARD_SLASH = "/";
    private static final String SHA_1 = "SHA-1";
    private static final String HEX_CHAR = "%02x";
    private static final String BLOB = "blob ";
    private static final String SPACE = " ";
    private static final String NULL_CHAR = "\0";
    private static final String TREE = "tree ";
    private static final String FILE_MODE_BLOB = "100644";  // Regular file (non-executable)
    private static final String TREE_MODE_DIRECTORY = "40000"; // Directory (tree object)
    private static final String GIT_DIRECTORY = ".git";
    private static final String CURRENT_DIR = ".";


    @Override
    public void execute(String[] args) throws Exception {
        File currentDir = new File(CURRENT_DIR);
        String treeHash = writeTree(currentDir);
        System.out.println(treeHash);
    }

    private String writeTree(File directory) throws IOException {
        if (!directory.isDirectory()) return null;

        List<byte[]> entries = new ArrayList<>();

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.getName().equals(GIT_DIRECTORY)) continue; // Ignore .git directory

            if (file.isFile()) {
                String fileHash = hashAndStoreBlob(file);
                entries.add(serializeEntry(FILE_MODE_BLOB, file.getName(), fileHash));
            } else if (file.isDirectory()) {
                String treeHash = writeTree(file);
                entries.add(serializeEntry(TREE_MODE_DIRECTORY, file.getName(), treeHash));
            }
        }

        // Sort entries using raw bytes (Git sorts lexicographically)
        entries.sort((a, b) -> {
            // Find the space byte (0x20) after the mode
            int aSpace = -1;
            for (int i = 0; i < a.length; i++) {
                if (a[i] == ' ') {
                    aSpace = i;
                    break;
                }
            }

            // Find the null byte (0x00) after the name
            int aNull = aSpace + 1;
            while (aNull < a.length && a[aNull] != 0) {
                aNull++;
            }

            // Do the same for b
            int bSpace = -1;
            for (int i = 0; i < b.length; i++) {
                if (b[i] == ' ') {
                    bSpace = i;
                    break;
                }
            }

            int bNull = bSpace + 1;
            while (bNull < b.length && b[bNull] != 0) {
                bNull++;
            }

            // Extract name bytes from both entries
            byte[] aName = Arrays.copyOfRange(a, aSpace + 1, aNull);
            byte[] bName = Arrays.copyOfRange(b, bSpace + 1, bNull);

            return Arrays.compare(aName, bName);
        });

        // Compute the tree object byte size
        int totalSize = entries.stream().mapToInt(e -> e.length).sum();
        byte[] header = (TREE + totalSize + NULL_CHAR).getBytes();

        // Merge header and entries
        ByteArrayOutputStream treeStream = new ByteArrayOutputStream();
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

    private byte[] serializeEntry(String mode, String name, String hash) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write((mode + SPACE + name + NULL_CHAR).getBytes());
            output.write(hexToBinary(hash)); // Convert hash to binary
        } catch (IOException e) {
            throw new RuntimeException("Error serializing entry", e);
        }
        return output.toByteArray();
    }

    private String hashAndStoreBlob(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] header = (BLOB + content.length + NULL_CHAR).getBytes();

        ByteArrayOutputStream blobStream = new ByteArrayOutputStream();
        blobStream.write(header);
        blobStream.write(content);
        byte[] blobData = blobStream.toByteArray();

        String blobHash = computeSHA1(blobData);
        storeObject(blobHash, blobData);
        return blobHash;
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
        String objectDir = OBJECTS_PATH + hash.substring(0, 2);
        String objectPath = objectDir + FORWARD_SLASH + hash.substring(2);

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
            hex.append(String.format(HEX_CHAR, b));
        }
        return hex.toString();
    }
}
