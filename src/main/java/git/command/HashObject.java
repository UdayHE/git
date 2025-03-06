package git.command;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

public class HashObject implements Command {

    private static final Logger log = Logger.getLogger(HashObject.class.getName());

    private static final String ARG = "-w";
    private static final String SHA_1 = "SHA-1";
    private static final String BLOB = "blob ";
    private static final String NULL_CHAR = "\0";
    private static final String HEX_FORMAT = "%02x";
    private static final String OBJECTS_PATH = ".git/objects/";

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length != 2 || !ARG.equals(args[0])) {
            log.log(Level.SEVERE, "Usage: hash-object -w <file>");
            return;
        }
        String fileName = args[2];

        try {

            long fileSize = Files.size(Paths.get(fileName));

            byte[] fileContentInByte = Files.readAllBytes(Paths.get(fileName));

            String fileContentCovertedIntoString = new String(fileContentInByte);

            String header = "blob " + fileSize + "\0";

            String combinedData = header + fileContentCovertedIntoString;

//           Now we have the combined data and need to convert it into hash

            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] messageDigest = md.digest(combinedData.getBytes());

//           Convert byte array into hex format

            StringBuilder sb = new StringBuilder();

            for(byte b: messageDigest){

                sb.append(String.format("%02x", b));

            }

            String hashedString = sb.toString();

            String blobPath =

                    String.format(".git/objects/%s/%s", hashedString.substring(0, 2),

                            hashedString.substring(2));

            File blobFile = new File(blobPath);

            blobFile.getParentFile().mkdirs();

            DeflaterOutputStream out =

                    new DeflaterOutputStream(new FileOutputStream(blobFile));

            out.write("blob".getBytes());

            out.write(" ".getBytes());

            out.write(String.valueOf(fileSize).getBytes());

            out.write(0);

            out.write(fileContentInByte);

            out.close();

            System.out.println(hashedString);

        }

        catch (Exception e) {

            throw new Exception(e);

        }
    }
}