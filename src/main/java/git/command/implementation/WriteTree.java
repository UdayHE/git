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
    public void execute(String[] args) throws Exception {
        File currentDir = new File(".");
        String treeHash = writeTree(currentDir);
        System.out.println(treeHash);
    }

    private String writeTree(File directory) throws IOException {
        if (!directory.isDirectory()) return null;

        List<byte[]> entries = new ArrayList<>();

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.getName().equals(".git")) continue; // Ignore .git directory

            if (file.isFile()) {
                String fileHash = hashAndStoreBlob(file);
                entries.add(serializeEntry("100644", file.getName(), fileHash));
            } else if (file.isDirectory()) {
                String treeHash = writeTree(file);
                entries.add(serializeEntry("40000", file.getName(), treeHash));
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
        byte[] header = ("tree " + totalSize + "\0").getBytes();

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
            output.write((mode + " " + name + "\0").getBytes());
            output.write(hexToBinary(hash)); // Convert hash to binary
        } catch (IOException e) {
            throw new RuntimeException("Error serializing entry", e);
        }
        return output.toByteArray();
    }

    private String hashAndStoreBlob(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] header = ("blob " + content.length + "\0").getBytes();

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
}
