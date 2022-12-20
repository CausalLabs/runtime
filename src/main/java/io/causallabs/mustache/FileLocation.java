package io.causallabs.mustache;

/** The source code location where the entity was defined */
public interface FileLocation {
    /** The filename */
    public String getFile();

    /** The line number */
    public int getLine();

    /** The column position withing the line */
    public int getColumn();
}
