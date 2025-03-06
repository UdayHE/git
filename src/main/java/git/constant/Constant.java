package git.constant;

public class Constant {

    private Constant() {
    }

    public static final String ARG_P = "-p";
    public static final String ARG_M = "-m";
    public static final String OBJECTS_BASE_PATH = ".git/objects/%s/%s";
    public static final char NULL_CHAR = '\0';

    public static final String SHA_1 = "SHA-1";
    public static final String OBJECTS_PATH = ".git/objects/";

    public static final byte[] OBJECT_TYPE_BLOB = "blob".getBytes();
    public static final byte[] SPACE_BYTES = " ".getBytes();
    public static final byte[] HEADS_REFS_BYTES = "ref: refs/heads/main\n".getBytes();
    public static final byte[] NULL_BYTES = {0};

    public static final String FORWARD_SLASH = "/";
    public static final String HEX_CHAR = "%02x";
    public static final String BLOB = "blob ";
    public static final String SPACE = " ";
    public static final String NULL_STRING = "\0";
    public static final String TREE = "tree ";
    public static final String PARENT = "parent ";
    public static final String COMMIT = "commit ";
    public static final String FILE_MODE_BLOB = "100644";  // Regular file (non-executable)
    public static final String TREE_MODE_DIRECTORY = "40000"; // Directory (tree object)
    public static final String GIT_DIRECTORY = ".git";
    public static final String OBJECTS = "objects";
    public static final String REFS = "refs";
    public static final String HEAD = "HEAD";
    public static final String CURRENT_DIR = ".";
    public static final char EMPTY_CHAR = ' ';


    public static final String COMMIT_TREE = "commit-tree";

}
