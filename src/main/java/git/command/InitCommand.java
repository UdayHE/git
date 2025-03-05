package git.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class InitCommand implements Command {
        @Override
        public void execute() {
            final File root = new File(".git");
            new File(root, "objects").mkdirs();
            new File(root, "refs").mkdirs();
            final File head = new File(root, "HEAD");

            try {
                head.createNewFile();
                Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
                System.out.println("Initialized git directory");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }