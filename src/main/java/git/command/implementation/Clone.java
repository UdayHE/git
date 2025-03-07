package git.command.implementation;

import git.command.Command;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Clone implements Command {

    private static final Logger log = Logger.getLogger(Clone.class.getName());

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length != 3) {
            log.log(Level.SEVERE, "Usage: clone <repository_url> <destination_directory>");
            return;
        }

        String repoUrl = args[1];
        String destinationDir = args[2];

        log.log(Level.INFO, "Cloning repository: {0}", repoUrl);
        log.log(Level.INFO, "Destination: {0}", destinationDir);

        File repoDirectory = new File(destinationDir);

        if (repoDirectory.exists()) {
            log.log(Level.SEVERE, "Error: Directory already exists.");
            return;
        }

        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDirectory)
                    .call();
            log.log(Level.INFO, "Repository successfully cloned into: {0}", destinationDir);
        } catch (GitAPIException e) {
            log.log(Level.SEVERE, "Error: Cloning failed - {0}", e.getMessage());
        }
    }
}

