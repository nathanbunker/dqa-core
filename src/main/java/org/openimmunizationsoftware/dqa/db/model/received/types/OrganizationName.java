package org.openimmunizationsoftware.dqa.db.model.received.types;

import org.openimmunizationsoftware.dqa.db.model.CodeTable;

public class OrganizationName
{
  private String name = "";
  private Id id = new Id(CodeTable.Type.ORGANIZATION);
  public String getName()
  {
    return name;
  }
  public void setName(String name)
  {
    this.name = name;
  }
  public String getIdNumber()
  {
    return id.getNumber();
  }
  public Id getId()
  {
    return id;
  }
  public void setIdNumber(String idNumber)
  {
    this.id.setNumber(idNumber);
  }
}
