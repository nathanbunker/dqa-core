/*
 * Copyright 2013 by Dandelion Software & Research, Inc (DSR)
 * 
 * This application was written for immunization information system (IIS) community and has
 * been released by DSR under an Apache 2 License with the hope that this software will be used
 * to improve Public Health.  
 */
package org.openimmunizationsoftware.dqa.db.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.dqa.db.model.received.Patient;

public class KnownName
{
  public static String INVALID_NAME = "Invalid Name";
  public static String UNNAMED_NEWBORN = "Unnamed Newborn";
  public static String TEST_PATIENT = "Test Patient";
  public static String INVALID_PREFIXES = "Invalid Prefixes";
  public static String TEST_PREFIXES = "Test Prefixes";
  public static String UNNAMED_PATIENT = "Unnamed Patient";

  private int knownNameId = 0;
  private String nameLast = "";
  private String nameFirst = "";
  private String nameMiddle = "";
  private Date birthDate = null;
  private String nameType = "";

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (nameLast != null)
    {
      sb.append(nameLast);
      sb.append(" ");
    }
    if (nameFirst != null)
    {
      sb.append(nameFirst);
      sb.append(" ");
    }
    if (nameMiddle != null)
    {
      sb.append(nameMiddle);
      sb.append(" ");
    }
    if (birthDate != null)
    {
      SimpleDateFormat sdf = new SimpleDateFormat("mm/DD/yyyy");
      sb.append(sdf.format(birthDate));
      sb.append(" ");
    }
    // TODO Auto-generated method stub
    return sb.toString();
  }

  public boolean match(Patient patient)
  {
    if (nameLast != null && patient.getNameLast() != null && !nameLast.equalsIgnoreCase(patient.getNameLast()))
    {
      return false;
    }
    if (nameFirst != null && patient.getNameFirst() != null && !nameFirst.equalsIgnoreCase(patient.getNameFirst()))
    {
      return false;
    }
    if (nameMiddle != null && patient.getNameMiddle() != null && !nameMiddle.equalsIgnoreCase(patient.getNameMiddle()))
    {
      return false;
    }
    if (birthDate != null && patient.getBirthDate() != null && birthDate.equals(patient.getBirthDate()))
    {
      return false;
    }
    return true;
  }

  public int getKnownNameId()
  {
    return knownNameId;
  }

  public boolean onlyNameLast()
  {
    return nameFirst == null && nameMiddle == null && birthDate == null;
  }

  public boolean onlyNameFirst()
  {
    return nameLast == null && nameMiddle == null && birthDate == null;
  }

  public boolean onlyNameMiddle()
  {
    return nameFirst == null && nameLast == null && birthDate == null;
  }

  public void setKnownNameId(int knownNameId)
  {
    this.knownNameId = knownNameId;
  }

  public String getNameLast()
  {
    return nameLast;
  }

  public void setNameLast(String nameLast)
  {
    this.nameLast = nameLast;
  }

  public String getNameFirst()
  {
    return nameFirst;
  }

  public void setNameFirst(String nameFirst)
  {
    this.nameFirst = nameFirst;
  }

  public String getNameMiddle()
  {
    return nameMiddle;
  }

  public void setNameMiddle(String nameMiddle)
  {
    this.nameMiddle = nameMiddle;
  }

  public Date getBirthDate()
  {
    return birthDate;
  }

  public void setBirthDate(Date birthDate)
  {
    this.birthDate = birthDate;
  }

  public String getNameType()
  {
    return nameType;
  }

  public void setNameType(String nameType)
  {
    this.nameType = nameType;
  }
}
