package org.openimmunizationsoftware.dqa.db.model.received.types;

import org.openimmunizationsoftware.dqa.db.model.received.Patient;

public class PatientAddress extends Address
{
  private long addressId = 0;
  private Patient patient = null;
  private int positionId = 0;
  private boolean skipped = false;

  public long getAddressId()
  {
    return addressId;
  }

  public void setAddressId(long addressId)
  {
    this.addressId = addressId;
  }

  public int getPositionId()
  {
    return positionId;
  }

  public void setPositionId(int positionId)
  {
    this.positionId = positionId;
  }

  public boolean isSkipped()
  {
    return skipped;
  }

  public void setSkipped(boolean skipped)
  {
    this.skipped = skipped;
  }

  public Patient getPatient()
  {
    return patient;
  }

  public void setPatient(Patient patient)
  {
    this.patient = patient;
  }
  
}
