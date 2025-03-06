package git.command.implementation;

import git.command.Command;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import static git.constant.Constant.FORWARD_SLASH;
import static git.constant.Constant.OBJECTS_PATH;
import static java.lang.System.out;

public class LsTree implements Command {

    private static final Logger log = Logger.getLogger(LsTree.class.getName());

    @Override
    public void execute(String[] args) throws Exception {
        String hash = args[2];
        File file = new File(OBJECTS_PATH + hash.substring(0, 2) + FORWARD_SLASH + hash.substring(2));
        if (!file.exists()) {
            log.log(Level.SEVERE, "Error: Object not found.");
            return;
        }
        try (InflaterInputStream inFlaterStream = new InflaterInputStream(new FileInputStream(file))) {
            byte[] rawData = inFlaterStream.readAllBytes(); // Read decompressed tree object
            parseAndPrintTree(rawData);
        }
    }

    private void parseAndPrintTree(byte[] data) {
        int index = 0;
        // **Skip tree object header** ("tree <size>\0")
        while (data[index] != 0) index++;
        index++; // Move past the null terminator

        List<String> fileNames = new ArrayList<>();

        while (index < data.length) {
            // Extract file mode (ends at the first space)
            int modeEnd = index;
            while (data[modeEnd] != ' ') modeEnd++;
            new String(data, index, modeEnd - index);
            index = modeEnd + 1;

            // Extract filename (ends at null byte)
            int nameEnd = index;
            while (data[nameEnd] != 0) nameEnd++;
            String fileName = new String(data, index, nameEnd - index);
            index = nameEnd + 1; // Move past null byte

            // **Skip the 20-byte SHA-1 binary hash**
            index += 20;

            // Store the filename for sorting
            fileNames.add(fileName);
        }
        // **Sort and print the filenames**
        fileNames.sort(null);
        for (String name : fileNames)
            out.println(name);
    }
}

