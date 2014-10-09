package org.openimmunizationsoftware.dqa;

public class InitializationException extends RuntimeException
{
  public InitializationException()
  {
    super();
  }
  
  public InitializationException(String message)
  {
    super(message);
  }
  
  public InitializationException(Throwable t)
  {
    super(t);
  }
  
  public InitializationException(String message, Throwable t)
  {
    super(message, t);
  }
}
