package org.openimmunizationsoftware.dqa.validate;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;
import org.openimmunizationsoftware.dqa.manager.PotentialIssues;

public class SectionValidator
{
  protected PotentialIssues pi = PotentialIssues.getPotentialIssues();
  private String fieldName = "";

  public SectionValidator(String fieldName) {
    this.fieldName = fieldName;
  }

  public void validateVaccination(Vaccination vaccination, Validator validator)
  {
    validator.documentHeaderSub("Vaccination " + fieldName);
  }

  public void validatePatient(MessageReceived message, Validator validator)
  {
    validator.documentHeaderSub("Patient " + fieldName);
  }

  public void validateHeader(MessageReceived message, Validator validator)
  {
    validator.documentHeaderSub("Header " + fieldName);
  }
}
