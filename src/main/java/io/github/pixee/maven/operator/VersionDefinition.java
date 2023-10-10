package io.github.pixee.maven.operator;

public class VersionDefinition {
    private Kind kind;
    private String value;

    public VersionDefinition(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Kind getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }
}
