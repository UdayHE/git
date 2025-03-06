import git.Git;

public class Main {


    public static void main(String[] args) throws Exception {
        Git.getInstance().process(args);
    }
}
