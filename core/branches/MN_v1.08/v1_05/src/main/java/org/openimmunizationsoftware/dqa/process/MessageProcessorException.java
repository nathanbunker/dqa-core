package org.openimmunizationsoftware.dqa.process;

public class MessageProcessorException extends Exception {

  static final long serialVersionUID = 1L;
  
  public MessageProcessorException()
  {
    super();
  }
	  
  public MessageProcessorException(String message)
  {
    super(message);
  }
  
  public MessageProcessorException(Throwable t)
  {
    super(t);
  }
  
  public MessageProcessorException(String message, Throwable t)
  {
    super(message, t);
  }
}
