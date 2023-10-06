package io.github.pixee.maven.operator;

import lombok.Getter;

import java.io.File;
import java.io.IOException;

@Getter
public class InvalidPathException extends IOException {
    private final File parentPath;
    private final String relativePath;
    private final boolean loop;

    public InvalidPathException(File parentPath, String relativePath, boolean loop) {
        super("Invalid Relative Path " + relativePath + " (from " + parentPath.getAbsolutePath() + ") (loops? " + loop + ")");
        this.parentPath = parentPath;
        this.relativePath = relativePath;
        this.loop = loop;
    }

}

