package git.command;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

public class HashObject implements Command {

    private static final Logger log = Logger.getLogger(HashObject.class.getName());

    private static final String ARG = "-w";
    private static final String SHA_1 = "SHA-1";
    private static final String BLOB = "blob ";
    private static final String NULL_CHAR = "\0";
    private static final String HEX_FORMAT = "%02x";
    private static final String OBJECTS_PATH = ".git/objects/";

    @Override
    public void execute(String[] args) {
        if (args.length != 2 || !ARG.equals(args[0])) {
            log.log(Level.SEVERE, "Usage: hash-object -w <file>");
            return;
        }
        String fileName = args[2];
        try {
            byte[] fileContent = Files.readAllBytes(Path.of(fileName));
            // Create Git-style blob header
            String header = BLOB + fileContent.length + NULL_CHAR;
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            // Combine header and file content
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(headerBytes);
            outputStream.write(fileContent);

            byte[] gitBlob = outputStream.toByteArray();
            // Compute SHA-1 hash
            MessageDigest md = MessageDigest.getInstance(SHA_1);
            byte[] sha1Hash = md.digest(gitBlob);
            // Convert hash to hex string
            StringBuilder hashHex = new StringBuilder();
            for (byte b : sha1Hash)
                hashHex.append(String.format(HEX_FORMAT, b));

            String objectHash = hashHex.toString();
            System.out.print(objectHash);
            // Write to .git/objects directory
            File gitObjectsDir = new File(OBJECTS_PATH + objectHash.substring(0, 2));

            if (!gitObjectsDir.exists())
                gitObjectsDir.mkdirs();

            File objectFile = new File(gitObjectsDir, objectHash.substring(2));
            try (FileOutputStream fos = new FileOutputStream(objectFile);
                 DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
                dos.write(gitBlob); // Compress and write
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}