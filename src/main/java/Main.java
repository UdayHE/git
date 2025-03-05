import git.command.CommandRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.log(Level.INFO, "Logs from your program will appear here!");
        CommandRegistry commandRegistry = new CommandRegistry();
        final String command = args[0];
        commandRegistry.execute(command);
    }
}
