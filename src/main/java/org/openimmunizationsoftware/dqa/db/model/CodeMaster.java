package org.openimmunizationsoftware.dqa.db.model;

import java.io.Serializable;

public class CodeMaster implements Serializable
{
  
  private static final long serialVersionUID = 1l;
  
  private int codeMasterId = 0;
  private CodeTable table = null;
  private String codeValue = "";
  private String codeLabel = "";
  private String useValue = "";
  private CodeStatus codeStatus = null;
  
  public int getCodeMasterId()
  {
    return codeMasterId;
  }
  public void setCodeMasterId(int codeMasterId)
  {
    this.codeMasterId = codeMasterId;
  }
  public CodeTable getTable()
  {
    return table;
  }
  public void setTable(CodeTable table)
  {
    this.table = table;
  }
  public String getCodeValue()
  {
    return codeValue;
  }
  public void setCodeValue(String codeValue)
  {
    this.codeValue = codeValue;
  }
  public String getCodeLabel()
  {
    return codeLabel;
  }
  public void setCodeLabel(String codeLabel)
  {
    this.codeLabel = codeLabel;
  }
  public String getUseValue()
  {
    return useValue;
  }
  public void setUseValue(String useValue)
  {
    this.useValue = useValue;
  }
  public CodeStatus getCodeStatus()
  {
    return codeStatus;
  }
  public void setCodeStatus(CodeStatus codeStatus)
  {
    this.codeStatus = codeStatus;
  }
  
}
