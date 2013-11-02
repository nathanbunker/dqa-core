package org.openimmunizationsoftware.dqa.db.model.received.types;

import org.openimmunizationsoftware.dqa.db.model.CodeTable;
import org.openimmunizationsoftware.dqa.db.model.received.Patient;

public class PatientIdNumber extends Id
{
  public PatientIdNumber()
  {
    super(CodeTable.Type.PATIENT_ID);
  }
  
  private int idNumberId = 0;
  private Patient patient = null;
  private int positionId = 0;
  private boolean skipped = false;
  
  public int getIdNumberId()
  {
    return idNumberId;
  }
  public void setIdNumberId(int idNumberId)
  {
    this.idNumberId = idNumberId;
  }
  public Patient getPatient()
  {
    return patient;
  }
  public void setPatient(Patient patient)
  {
    this.patient = patient;
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
  
}
