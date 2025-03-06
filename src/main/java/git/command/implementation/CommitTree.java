package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

import static git.constant.Constant.*;

public class CommitTree implements Command {

    private static final Logger log = Logger.getLogger(CommitTree.class.getName());

    @Override
    public void execute(String[] args) {
        String treeSha = null;
        String parentSha = null;
        String message = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case COMMIT_TREE:
                    treeSha = args[i + 1];
                    i++;
                    break;
                case ARG_P:
                    parentSha = args[i + 1];
                    i++;
                    break;
                case ARG_M:
                    message = args[i + 1];
                    i++;
                    break;
            }
        }

        if (treeSha == null || message == null) {
            log.log(Level.SEVERE, "Usage: commit-tree <tree_sha> -p <commit_sha> -m <message>");
            return;
        }

        String commitContent = buildCommitContent(treeSha, parentSha, message);
        String commitSha = writeCommitObject(commitContent);

        if (commitSha != null)
            System.out.println(commitSha);
    }

    private String buildCommitContent(String treeSha, String parentSha, String message) {
        String author = "Test User <test@example.com>";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String timezone = "+0000";

        StringBuilder commitContent = new StringBuilder();
        commitContent.append(TREE).append(treeSha).append("\n");
        if (parentSha != null) {
            commitContent.append(PARENT).append(parentSha).append("\n");
        }
        commitContent.append("author ").append(author).append(" ").append(timestamp).append(" ").append(timezone).append("\n");
        commitContent.append("committer ").append(author).append(" ").append(timestamp).append(" ").append(timezone).append("\n\n");
        commitContent.append(message).append("\n");

        return commitContent.toString();
    }

    private String writeCommitObject(String content) {
        try {
            // 1. Prepare Git object header: "commit <size>\0"
            String header = COMMIT + content.length() + NULL_STRING;
            byte[] fullContent = (header + content).getBytes(StandardCharsets.UTF_8);

            // 2. Compute the SHA-1 hash of the full content
            String sha = computeSHA1(fullContent);

            // 3. Determine the object storage path
            File objectDir = new File(OBJECTS_BASE_PATH + sha.substring(0, 2));
            if (!objectDir.exists()) {
                objectDir.mkdirs();
            }
            File commitFile = new File(objectDir, sha.substring(2));

            // 4. Compress and write to file
            try (FileOutputStream fos = new FileOutputStream(commitFile);
                 DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
                dos.write(fullContent);
            }

            return sha;
        } catch (IOException | NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "Exception in writeCommitObject {0}", e.getMessage());
            return null;
        }
    }

    private String computeSHA1(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(SHA_1);
        byte[] hash = md.digest(data);
        return byteArrayToHex(hash);
    }

    private String byteArrayToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes)
                formatter.format(HEX_CHAR, b);
            return formatter.toString();
        }
    }
}