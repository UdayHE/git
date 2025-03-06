package git.enums;

public enum Command {

    INIT("init"),
    CAT_FILE("cat-file"),
    HASH_OBJECT("hash-object"),
    LS_TREE("ls-tree"),
    WRITE_TREE("write-tree");

    private final String value;

    Command(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
