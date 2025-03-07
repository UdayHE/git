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
            System.out.println("Repository successfully cloned into: " + destinationDir);
        } catch (GitAPIException e) {
            log.log(Level.SEVERE,"Error: Cloning failed - {0}" , e.getMessage());
        }
    }
}

