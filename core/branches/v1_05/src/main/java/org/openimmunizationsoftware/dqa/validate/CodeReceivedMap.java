package org.openimmunizationsoftware.dqa.validate;

import java.util.HashMap;

import org.openimmunizationsoftware.dqa.db.model.CodeReceived;


public class CodeReceivedMap extends HashMap<String, CodeReceived>
{
  private CodeReceivedMap parent = null;
  
  public CodeReceivedMap()
  {
    super();
  }
  
  public CodeReceivedMap(CodeReceivedMap parent)
  {
    this.parent = parent;
  }
  
  public CodeReceived getCodeReceived(String receivedValue)
  {
    CodeReceived codeReceived = get(receivedValue);
    if (codeReceived == null && parent != null)
    {
      return parent.getCodeReceived(receivedValue);
    }
    return codeReceived;
  }
}
