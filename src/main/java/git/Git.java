package git;

import git.command.CommandRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Git {

    private static final Logger log = Logger.getLogger(Git.class.getName());

    private static final Git INSTANCE = new Git();

    private Git() {
    }

    public static Git getInstance() {
        return INSTANCE;
    }

    public void process(String[] args) {
        log.log(Level.INFO, "Working with Git");
        CommandRegistry commandRegistry = new CommandRegistry();
        final String command = args[0];
        log.log(Level.INFO, "Executing {0} command", command);
        commandRegistry.execute(command);
    }
}
