package git.command.implementation;

import git.command.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

public class LsTree implements Command {


    @Override
    public void execute(String[] args) throws Exception {
        String hash = args[2];
        File file = new File(".git/objects/" + hash.substring(0, 2) + "/" + hash.substring(2));
        BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(file))));
        String line;
        StringBuilder treeObjectContent = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            treeObjectContent.append(line);
        }
        String temp = new String(treeObjectContent);
        String[] array = temp.split("\0");
        ArrayList<String> dirStructure = new ArrayList<>();
        for (int i = 1; i < array.length; i++) {
            String[] tempArray = array[i].split(" ", 2);
            if (tempArray.length == 2) {
                String name = tempArray[1];
                dirStructure.add(name);
            }
        }
        dirStructure.sort(null);
        for (String s : dirStructure)
            System.out.println(s);
    }
}
