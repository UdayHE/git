package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Init implements Command {

    private static final Logger log = Logger.getLogger(Init.class.getName());

    @Override
    public void execute(String[] args) {
        final File root = new File(".git");
        final File objectsDir = new File(root, "objects");
        final File refsDir = new File(root, "refs");
        final File head = new File(root, "HEAD");

        // Create .git directory and its subdirectories
        try {
            createObjectsDirectory(objectsDir);
            createRefsDirectory(refsDir);
            createHeadFile(head);
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
            log.log(Level.INFO, "Initialized git directory");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception in init command while handling file: {0}", e.getMessage());
            throw new RuntimeException("Error initializing git directory.", e);
        }
    }

    private void createHeadFile(File head) throws IOException {
        if (!head.createNewFile() && !head.exists())
            throwException("Failed to create HEAD file.");
    }

    private void createRefsDirectory(File refsDir) throws IOException {
        if (!refsDir.mkdirs() && !refsDir.exists())
            throwException("Failed to create refs directory.");
    }

    private void createObjectsDirectory(File objectsDir) throws IOException {
        if (!objectsDir.mkdirs() && !objectsDir.exists())
            throwException("Failed to create objects directory.");
    }

    private void throwException(String msg) throws IOException {
        log.log(Level.SEVERE, msg);
        throw new IOException(msg);
    }
}
