package git.enums;

public enum Command {

    INIT("init"),
    CAT_FILE("cat-file"),
    HASH_OBJECT("hash-object"),
    LS_TREE("ls-tree"),
    WRITE_TREE("write-tree"),
    COMMIT_TREE("commit-tree"),
    CLONE("clone");

    private final String value;

    Command(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
