package org.openimmunizationsoftware.dqa.validate;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.manager.PotentialIssues;

public class PatientClassValidator extends SectionValidator
{
  public PatientClassValidator() {
    super("Class");
  }

  @Override
  public void validatePatient(MessageReceived message, Validator validator)
  {
    super.validatePatient(message, validator);
    validator
        .documentParagraph("Indicates the category for this patient by site. For immunization registries this should always be R for Recurring. ");
    if (validator.getIssueFoundFirst(pi.Hl7Pv1SegmentIsMissing) == null)
    {
      validator.handleCodeReceived(message.getPatient().getPatientClass(), PotentialIssues.Field.PATIENT_CLASS);
    }
    if (message.getPatient().getPatientClass().equals(""))
    {
      validator.documentParagraph("No patient class indicating, defaulting to R for recurring.");
      message.getPatient().getPatientClass().setCode("R");
    }
  }
}
