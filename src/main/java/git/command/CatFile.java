package git.command;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

public class CatFile implements Command {

    private static final Logger log = Logger.getLogger(CatFile.class.getName());

    @Override
    public void execute(String[] args) {
        if (args[1].equals("-p")) {
            catFile(args);
        }
    }

    private void catFile(String[] args) {
        String fileName = args[2];
        String path = String.format(".git/objects/%s/%s", fileName.substring(0, 2), fileName.substring(2));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(path))))) {
            String line = reader.readLine();
            System.out.print(line.substring(line.indexOf('\0') + 1));
            while ((line = reader.readLine()) != null)
                log.log(Level.INFO, line);
        } catch (IOException exception) {
            log.log(Level.SEVERE, exception.getMessage());
        }
    }
}
