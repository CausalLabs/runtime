package io.causallabs.mustache;

import java.util.List;

public interface Description {

    /** Return the FDL comment */
    public String getDescription();

    public String getFullDescription();

    public List<String> getDescriptionLines();

    public boolean isDeprecated();

    public String getDeprecatedReason();
}
