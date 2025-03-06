package git.command.implementation;

import git.command.Command;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

public class WriteTree implements Command {

    private static final Logger log = Logger.getLogger(WriteTree.class.getName());


    @Override
    public void execute(String[] args) throws Exception {
        File file = new File(".");
        String treeHash = writeTree(file);
        System.out.println(treeHash);
    }

    private String writeTree(File directory) throws IOException, NoSuchAlgorithmException {
        if (!directory.isDirectory()) return null;
        List<String> entries = new ArrayList<>();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.getName().equals(".git")) continue;  // Ignore .git directory

            if (file.isFile()) {
                String hash = getShaAndFileblobFromFile(file)[0];
                entries.add("100644 " + file.getName() + "\0" + hexToBinary(hash));
            } else if (file.isDirectory()) {
                String treeHash = writeTree(file);
                entries.add("40000 " + file.getName() + "\0" + hexToBinary(treeHash));
            }
        }
        String treeBlob = "tree " + entries.size() + "\0" + String.join("", entries);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(treeBlob.getBytes());
        String hex = bytesToHex(hash);

        File parentDir = new File(".git/objects/" + hex.substring(0, 2));
        if (!parentDir.exists()) parentDir.mkdirs();

        File treeFile = new File(parentDir, hex.substring(2));
        if (!treeFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(new FileOutputStream(treeFile))))) {
                writer.write(treeBlob);
            }
        }
        return hex;
    }

    private byte[] hexToBinary(String hex) {
        byte[] binary = new byte[20];
        for (int i = 0; i < 20; i++) {
            binary[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return binary;
    }


    private String[] getShaAndFileblobFromFile(File file) throws IOException, NoSuchAlgorithmException {
        byte[] content = Files.readAllBytes(file.toPath());

        String fileBlob = "blob "
                + content.length
                + "\0"
                + content;
        MessageDigest md = MessageDigest.getInstance("SHA-1"); //need to convert this to HEX
        byte[] hash = md.digest(fileBlob.getBytes());
        String hex = bytesToHex(hash);
        return new String[]{hex, fileBlob};
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            String temp = Integer.toHexString(0xff & hash[i]);
            if (temp.length() == 1) {
                hex.append('0');
            }
            hex.append(temp);
        }
        return hex.toString();
    }

}
