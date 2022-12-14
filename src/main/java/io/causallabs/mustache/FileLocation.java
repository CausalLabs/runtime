package io.causallabs.mustache;

public interface FileLocation {
    public String getFile();

    public int getLine();

    public int getColumn();
}
