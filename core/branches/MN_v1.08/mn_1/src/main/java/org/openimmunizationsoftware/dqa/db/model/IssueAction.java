package org.openimmunizationsoftware.dqa.db.model;

import java.io.Serializable;

public class IssueAction implements Serializable
{
  
  private static final long serialVersionUID = 1l;
  
  public static final IssueAction ERROR = new IssueAction("E", "Error");
  public static final IssueAction WARN = new IssueAction("W", "Warn");
  public static final IssueAction ACCEPT = new IssueAction("A", "Accept");
  public static final IssueAction SKIP = new IssueAction("S", "Skip");
  
  public boolean isError()
  {
    return this.actionCode.equals(ERROR.actionCode);
  }
  
  public boolean isWarn()
  {
    return this.actionCode.equals(WARN.actionCode);
  }
  
  public boolean isAccept()
  {
    return this.actionCode.equals(ACCEPT.actionCode);
  }
  
  public boolean isSkip()
  {
    return this.actionCode.equals(SKIP.actionCode);
  }
  
  public IssueAction()
  {
    // default
  }
  
  private IssueAction(String actionCode, String actionLabel)
  {
    this.actionCode = actionCode;
    this.actionLabel = actionLabel;
  }
  
  private String actionCode = "";
  private String actionLabel = "";

  public String getActionCode()
  {
    return actionCode;
  }
  public void setActionCode(String actionCode)
  {
    this.actionCode = actionCode;
  }
  public String getActionLabel()
  {
    return actionLabel;
  }
  public void setActionLabel(String actionLabel)
  {
    this.actionLabel = actionLabel;
  }
  @Override
  public String toString()
  {
    return this.getActionLabel();
  }
  
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof IssueAction)
    {
      return this.toString().equals(((IssueAction) obj).toString());
    }
    return toString().equals(obj.toString());
  }
  
  @Override
  public int hashCode()
  {
    return actionCode.hashCode();
  }
}
