package git.command.implementation;

import git.command.Command;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

public class Clone implements Command {

    @Override
    public void execute(String[] args) throws Exception {
//        if (args.length != 3) {
//            System.err.println("Usage: clone <repository_url> <destination_directory>");
//            return;
//        }

        String repoUrl = args[1];
        String destinationDir = args[2];

        System.out.println("Cloning repository: " + repoUrl);
        System.out.println("Destination: " + destinationDir);

        File repoDirectory = new File(destinationDir);

        if (repoDirectory.exists()) {
            System.err.println("Error: Directory already exists.");
            return;
        }

        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDirectory)
                    .call();
            System.out.println("Repository successfully cloned into: " + destinationDir);
        } catch (GitAPIException e) {
            System.err.println("Error: Cloning failed - " + e.getMessage());
            e.printStackTrace();
        }
    }
}

