package git.command;

public interface Command {

    void execute(String[] args) throws Exception;
}
