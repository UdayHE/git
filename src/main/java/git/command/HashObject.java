package git.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.DeflaterOutputStream;

import static java.lang.System.out;

public class HashObject implements Command {

    private static final String SHA_1 = "SHA-1";
    private static final String OBJECTS_PATH = ".git/objects/";

    private static final byte[] OBJECT_TYPE_BLOB = "blob".getBytes();
    private static final byte[] SPACE = " ".getBytes();
    private static final byte[] NULL = {0};

    @Override
    public void execute(String[] args) throws Exception {
        String fileName = args[2];
        String hash = hashFile(new File(fileName));
        out.println(hash);
    }

    public String hashFile(File File) throws IOException, NoSuchAlgorithmException {
        try (var inputStream = new FileInputStream(File)) {
            return hashFile(inputStream.readAllBytes());
        }
    }

    private String hashFile(byte[] bytes) throws IOException, NoSuchAlgorithmException {
        var lengthBytes = String.valueOf(bytes.length).getBytes();
        var message = MessageDigest.getInstance(SHA_1);
        message.update(OBJECT_TYPE_BLOB);
        message.update(SPACE);
        message.update(lengthBytes);
        message.update(NULL);
        message.update(bytes);

        var hashBytes = message.digest();
        var hash = HexFormat.of().formatHex(hashBytes);

        var firstTwo = hash.substring(0, 2);
        var rest = hash.substring(2);
        var firstTwoPath = Paths.get(OBJECTS_PATH, firstTwo).toFile();
        firstTwoPath.mkdirs();
        var restPath = Paths.get(firstTwoPath.getPath(), rest).toFile();
        try (
                var outputStream = Files.newOutputStream(restPath.toPath());
                var deflaterOutputStream = new DeflaterOutputStream(outputStream);
        ) {
            deflaterOutputStream.write(OBJECT_TYPE_BLOB);
            deflaterOutputStream.write(SPACE);
            deflaterOutputStream.write(lengthBytes);
            deflaterOutputStream.write(NULL);
            deflaterOutputStream.write(bytes);
        }
        return hash;
    }
}