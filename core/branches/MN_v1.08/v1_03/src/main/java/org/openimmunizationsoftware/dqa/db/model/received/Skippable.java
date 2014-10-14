package org.openimmunizationsoftware.dqa.db.model.received;

public interface Skippable
{
  public boolean isSkipped();
  
  public void setSkipped(boolean skipped);
}
