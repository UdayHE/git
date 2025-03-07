package git.command;

import git.command.implementation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static git.enums.Command.*;

public class CommandRegistry {

    private static final Logger log = Logger.getLogger(CommandRegistry.class.getName());

    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        commands.put(INIT.getValue(), new Init());
        commands.put(CAT_FILE.getValue(), new CatFile());
        commands.put(HASH_OBJECT.getValue(), new HashObject());
        commands.put(LS_TREE.getValue(), new LsTree());
        commands.put(WRITE_TREE.getValue(), new WriteTree());
        commands.put(COMMIT_TREE.getValue(), new CommitTree());
        commands.put(CLONE.getValue(), new Clone());
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
