package git.enums;

public enum Command {

    INIT("init");

    private final String value;

    Command(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
