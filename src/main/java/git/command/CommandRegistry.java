package git.command;

import git.command.implementation.CatFile;
import git.command.implementation.HashObject;
import git.command.implementation.Init;
import git.command.implementation.LsTree;

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
        commands.put(git.enums.Command.HASH_OBJECT.getValue(), new HashObject());
        commands.put(git.enums.Command.LS_TREE.getValue(), new LsTree());
    }

    public void execute(String[] args) throws Exception {
        final String command = args[0];
        log.log(Level.INFO, "Args: {0}", args);
        log.log(Level.INFO, "Executing {0} command", command);
        Command cmd = commands.get(command);
        if (cmd != null)
            cmd.execute(args);
        else
            log.log(Level.SEVERE, "Unknown command: {0}", command);
    }
}
