package org.openimmunizationsoftware.dqa.process;

import org.openimmunizationsoftware.dqa.db.model.MessageReceivedGeneric;

public class MessageProcessResponse
{
  private MessageReceivedGeneric messageReceived = null;

  public MessageReceivedGeneric getMessageReceived()
  {
    return messageReceived;
  }

  public void setMessageReceived(MessageReceivedGeneric messageReceived)
  {
    this.messageReceived = messageReceived;
  }
  
}
