package git.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Init implements Command {

    private static final Logger log = Logger.getLogger(Init.class.getName());

    @Override
    public void execute() {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
            head.createNewFile();
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
            log.log(Level.INFO, "Initialized git directory");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception in init command: {0}", e);
            throw new RuntimeException(e);
        }
    }
}