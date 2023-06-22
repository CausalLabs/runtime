package io.causallabs.mustache;

import java.util.List;

public interface Description {

  /**
   * return true if this Description has anything to say. A false means you do not have to add a
   * comment at all
   */
  public boolean hasDescription();

  /** Return the FDL comment */
  public String getDescription();

  public String getFullDescription();

  public List<String> getDescriptionLines();

  public boolean isDeprecated();

  public String getDeprecatedReason();
}
