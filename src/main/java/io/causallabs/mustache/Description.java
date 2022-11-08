package io.causallabs.mustache;

public interface Description {

    /** Return the FDL comment */
    public String getDescription();

    public String getFullDescription();

    public String[] getDescriptionLines();

    public boolean isDeprecated();

    public String getDeprecatedReason();
}
