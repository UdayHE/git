package git.command.implementation;

import git.command.Command;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import static git.constant.Constant.*;
import static java.lang.System.out;

public class CatFile implements Command {

    private static final Logger log = Logger.getLogger(CatFile.class.getName());

    @Override
    public void execute(String[] args) {
        if (args[1].equals(ARG_P))
            catFile(args);
    }

    private void catFile(String[] args) {
        String fileName = args[2];
        String path = String.format(OBJECTS_BASE_PATH, fileName.substring(0, 2), fileName.substring(2));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(path))))) {
            String line = reader.readLine();
            out.print(line.substring(line.indexOf(NULL_CHAR) + 1));
            while ((line = reader.readLine()) != null)
                log.log(Level.INFO, line);
        } catch (IOException exception) {
            log.log(Level.SEVERE, exception.getMessage());
        }
    }
}
