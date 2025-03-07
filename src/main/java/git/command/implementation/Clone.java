package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!repoDirectory.mkdirs()) {
                throw new IOException("Failed to create directory: " + repoDirectory.getAbsolutePath());
            }

            log.log(Level.INFO, "Fetching repository objects...");
            downloadGitObjects(repoUrl, repoDirectory);
            log.log(Level.INFO, "Checking out files...");
            checkoutFiles(repoDirectory);
            log.log(Level.INFO, "Repository successfully cloned into: {0}", repoDirectory.getAbsolutePath());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error: Cloning failed - {0}", e.getMessage());
        }
    }

    private void downloadGitObjects(String repoUrl, File repoDirectory) throws IOException {
        String gitDir = repoDirectory.getAbsolutePath() + "/.git";
        new File(gitDir).mkdirs();

        downloadFile(repoUrl + "/info/refs?service=git-upload-pack", new File(gitDir, "refs"));
        downloadFile(repoUrl + "/objects/info/packs", new File(gitDir, "objects/info/packs"));
        downloadFile(repoUrl + "/HEAD", new File(gitDir, "HEAD"));
    }

    private void checkoutFiles(File repoDirectory) throws IOException {
        File headFile = new File(repoDirectory, ".git/HEAD");
        if (!headFile.exists()) {
            throw new IOException("Missing HEAD file, repository might be incomplete");
        }

        String headRef = new String(Files.readAllBytes(headFile.toPath())).trim();
        Pattern pattern = Pattern.compile("ref: (.+)");
        Matcher matcher = pattern.matcher(headRef);
        if (!matcher.find()) {
            throw new IOException("Invalid HEAD reference");
        }
        String branchRef = matcher.group(1);
        File refFile = new File(repoDirectory, ".git/" + branchRef);
        if (!refFile.exists()) {
            throw new IOException("Missing reference file: " + branchRef);
        }
        String commitHash = new String(Files.readAllBytes(refFile.toPath())).trim();
        log.log(Level.INFO, "Checked out commit: {0}", commitHash);
    }

    private void downloadFile(String fileURL, File destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}