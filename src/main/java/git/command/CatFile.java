package git.command;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

public class CatFile implements Command {

    private static final Logger log = Logger.getLogger(CatFile.class.getName());

    @Override
    public void execute(String[] args) {
        String hash = args[2];
        String dirHash = hash.substring(0, 2);
        String fileHash = hash.substring(2);
        File blobFile = new File("./.git/objects/" + dirHash + "/" + fileHash);
        try {
            String blob = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile)))).readLine();
            String content = blob.substring(blob.indexOf("\0") + 1);
            log.log(Level.INFO, content);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception {0}", e);
            throw new RuntimeException(e);
        }
    }
}
