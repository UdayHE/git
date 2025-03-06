import git.command.CommandRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.log(Level.INFO, "Logs from your program will appear here!");
       // CommandRegistry commandRegistry = new CommandRegistry();
        final String command = args[0];
       // commandRegistry.execute(command);


        switch (command) {

            case "init" -> {

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

            default -> System.out.println("Unknown command: " + command);

        }
    }
}
