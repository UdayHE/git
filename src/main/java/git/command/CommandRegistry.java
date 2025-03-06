package git.command;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandRegistry {

    private static final Logger log = Logger.getLogger(CommandRegistry.class.getName());

    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        commands.put(git.enums.Command.INIT.getValue(), new Init());
        commands.put(git.enums.Command.CAT_FILE.getValue(), new CatFile());
    }

    public void execute(String[] args) {
        final String command = args[0];
        log.log(Level.INFO, "Executing {0} command", command);
        Command cmd = commands.get(command);
        if (cmd != null)
            cmd.execute(args);
        else
            log.log(Level.SEVERE, "Unknown command: {0}", command);
    }
}
