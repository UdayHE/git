package git.command;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandRegistry {

    private static final Logger log = Logger.getLogger(CommandRegistry.class.getName());

    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        commands.put(git.enums.Command.INIT.getValue(), new InitCommand());
    }

    public void execute(String command) {
        Command cmd = commands.get(command);
        if (cmd != null)
            cmd.execute();
        else
            log.log(Level.SEVERE, "Unknown command: {0}", command);
    }
}
