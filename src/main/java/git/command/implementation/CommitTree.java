package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Formatter;

public class CommitTree implements Command {


    @Override
    public void execute(String[] args) throws Exception {
        String treeSha = args[1];
        String parentSha = null;
        String message = null;

        for (int i = 2; i < args.length; i++) {
            if ("-p".equals(args[i]) && i + 1 < args.length) {
                parentSha = args[i + 1];
                i++;
            } else if ("-m".equals(args[i]) && i + 1 < args.length) {
                message = args[i + 1];
                i++;
            }
        }

        if (treeSha == null || message == null) {
            throw new IllegalArgumentException("Invalid arguments: tree SHA and message are required");
        }

        // Hardcoded author information
        String author = "John Doe <johndoe@example.com>";
        String timestamp = Instant.now().getEpochSecond() + " +0000";

        StringBuilder commitContent = new StringBuilder();
        commitContent.append("tree ").append(treeSha).append("\n");
        if (parentSha != null) {
            commitContent.append("parent ").append(parentSha).append("\n");
        }
        commitContent.append("author ").append(author).append(" ").append(timestamp).append("\n");
        commitContent.append("committer ").append(author).append(" ").append(timestamp).append("\n");
        commitContent.append("\n");
        commitContent.append(message).append("\n");

        // Generate commit SHA
        byte[] commitBytes = commitContent.toString().getBytes(StandardCharsets.UTF_8);
        String commitSha = generateSHA1("commit " + commitBytes.length + "\0" + commitContent);

        // Write commit to .git/objects
        String objectPath = ".git/objects/" + commitSha.substring(0, 2) + "/" + commitSha.substring(2);
        File objectFile = new File(objectPath);
        objectFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(objectFile)) {
            writer.write(commitContent.toString());
        }

        // Print the generated commit SHA
        System.out.println(commitSha);
    }

    private String generateSHA1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        try (Formatter formatter = new Formatter()) {
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
