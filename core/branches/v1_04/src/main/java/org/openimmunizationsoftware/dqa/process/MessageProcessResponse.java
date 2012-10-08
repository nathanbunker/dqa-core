package org.openimmunizationsoftware.dqa.process;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;

public class MessageProcessResponse
{
  private MessageReceived messageReceived = null;

  public MessageReceived getMessageReceived()
  {
    return messageReceived;
  }

  public void setMessageReceived(MessageReceived messageReceived)
  {
    this.messageReceived = messageReceived;
  }
  
}
