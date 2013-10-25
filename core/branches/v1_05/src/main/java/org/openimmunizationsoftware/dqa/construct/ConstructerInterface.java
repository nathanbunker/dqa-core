package org.openimmunizationsoftware.dqa.construct;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;

public interface ConstructerInterface
{
  public String constructMessage(MessageReceived messageReceived);
  
  public String makeHeader(MessageReceived messageReceived);
  
  public String makeFooter(MessageReceived messageReceived);
}
