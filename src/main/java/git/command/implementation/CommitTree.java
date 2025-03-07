package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

import static git.constant.Constant.*;

/**
 * Represents a command to create a commit tree object in a Git repository.
 * This class encapsulates the logic for building commit content from given parameters
 * and writing the commit object to the repository's object store.
 */
public class CommitTree implements Command {

    private static final Logger log = Logger.getLogger(CommitTree.class.getName());

    @Override
    public void execute(String[] args) {
        Map<String, String> argumentMap = new HashMap<>();
        // Parse command-line arguments and populate the map
        for (int i = 0; i < args.length - 1; i++)
            argumentMap.put(args[i], args[i + 1]);

        String treeSha = argumentMap.get(COMMIT_TREE);
        String parentSha = argumentMap.get(ARG_P);
        String message = argumentMap.get(ARG_M);

        // Validates required arguments and logs an error if they are missing
        if (treeSha == null || message == null) {
            log.log(Level.SEVERE, "Usage: commit-tree <tree_sha> -p <commit_sha> -m <message>");
            return;
        }

        // Builds the commit content using the provided SHA and message
        String commitContent = buildCommitContent(treeSha, parentSha, message);
        // Writes the commit object to the repository and prints the commit SHA
        String commitSha = writeCommitObject(commitContent);

        if (commitSha != null)
            System.out.println(commitSha);
    }

    /**
     * Constructs the content of a commit object based on the provided SHA values and message.
     * Includes the tree SHA, parent SHA (if present), author, committer, and commit message.
     *
     * @param treeSha   SHA of the tree object to be committed
     * @param parentSha SHA of the parent commit (optional)
     * @param message   Commit message describing the changes
     * @return String containing the formatted commit content
     */
    private String buildCommitContent(String treeSha, String parentSha, String message) {
        // Static author and timestamp information
        String author = AUTHOR_DETAILS;
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String timezone = TIME_ZONE;

        // Constructs the commit content using a StringBuilder for efficiency
        StringBuilder commitContent = new StringBuilder();
        commitContent.append(TREE).append(treeSha).append(NEW_LINE);
        if (parentSha != null) {
            commitContent.append(PARENT).append(parentSha).append(NEW_LINE);
        }
        commitContent.append(AUTHOR).append(author).append(SPACE).append(timestamp).append(SPACE).append(timezone).append(NEW_LINE);
        commitContent.append(COMMITTER).append(author).append(SPACE).append(timestamp).append(SPACE).append(timezone).append(NEW_LINE + NEW_LINE);
        commitContent.append(message).append(NEW_LINE);

        return commitContent.toString();
    }

    /**
     * Writes the commit object to the repository's object store.
     * The object is compressed using zlib and stored with a name derived from its SHA-1 hash.
     *
     * @param content The content of the commit object as a String
     * @return The SHA-1 hash of the commit object if written successfully, otherwise null
     */
    private String writeCommitObject(String content) {
        try {
            // Prepares the header for the Git object including the type and size
            String header = COMMIT + content.length() + NULL_STRING;
            byte[] fullContent = (header + content).getBytes(StandardCharsets.UTF_8);

            // Computes the SHA-1 hash of the full content to use as the object name
            String sha = computeSHA1(fullContent);

            // Determines the directory path based on the first two characters of the SHA
            File objectDir = new File(OBJECTS_PATH + sha.substring(0, 2));
            if (!objectDir.exists()) {
                objectDir.mkdirs();
            }
            // Constructs the full file path for storing the commit object
            File commitFile = new File(objectDir, sha.substring(2));

            // Compresses the content and writes it to the file
            try (FileOutputStream fos = new FileOutputStream(commitFile);
                 DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
                dos.write(fullContent);
            }

            return sha;
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Computes the SHA-1 hash of the provided byte array.
     *
     * @param data The byte array to hash
     * @return The SHA-1 hash as a hexadecimal string
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
     */
    private String computeSHA1(byte[] data) throws NoSuchAlgorithmException {
        // Uses the MessageDigest class to compute the SHA-1 hash
        MessageDigest md = MessageDigest.getInstance(SHA_1);
        byte[] hash = md.digest(data);
        // Converts the byte array hash to a hexadecimal string representation
        return byteArrayToHex(hash);
    }

    /**
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param bytes The byte array to convert
     * @return The hexadecimal string representation of the byte array
     */
    private String byteArrayToHex(byte[] bytes) {
        // Utilizes a Formatter to efficiently construct the hexadecimal string
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes)
                formatter.format(HEX_CHAR, b);
            return formatter.toString();
        }
    }
}
