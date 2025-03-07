package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.IOException;
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

        clone(repoUrl, repoDirectory);
    }

    private void clone(String repoUrl, File repoDirectory) {
        try {
            // Construct the Git clone command
            String[] command = {"git", "clone", repoUrl, repoDirectory.getAbsolutePath()};

            // Execute the command
            Process process = Runtime.getRuntime().exec(command);

            // Wait for the command to finish
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.log(Level.INFO, "Repository successfully cloned into: {0}", repoDirectory.getAbsolutePath());
            } else {
                log.log(Level.SEVERE, "Error: Cloning failed. Exit code: {0}", exitCode);
                // Optionally, you can read the error output from the process
                // to log more detailed error messages.
                readErrorOutput(process);
            }
        } catch (IOException | InterruptedException e) {
            log.log(Level.SEVERE, "Error: Cloning failed - {0}", e.getMessage());
        }
    }

    private void readErrorOutput(Process process) throws IOException {
        java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
        String errorOutput = s.hasNext() ? s.next() : "";
        log.log(Level.SEVERE, "Error output: {0}", errorOutput);
    }
}
