package org.openimmunizationsoftware.dqa.validate;

import java.text.SimpleDateFormat;

import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;

public class OrderNumberValidator extends SectionValidator
{
  public OrderNumberValidator() {
    super("Order Number");
  }

  @Override
  public void validateVaccination(Vaccination vaccination, Validator validator)
  {
    super.validateVaccination(vaccination, validator);
    validator.notEmpty(vaccination.getIdPlacer(), pi.VaccinationPlacerOrderNumberIsMissing);
    validator.notEmpty(vaccination.getIdSubmitter(), pi.VaccinationFillerOrderNumberIsMissing);
    validator.documentParagraph("The vaccination id should be sent as the filler order number regardless of wehther the vaccination was administered by the sending facility. The filler number is a unique id for the vaccination so that subsquent updates can be associated with the original report. ");
    if (!validator.notEmpty(vaccination.getIdSubmitter(), pi.VaccinationIdIsMissing))
    {
      // vaccination id is empty, need to set to default value
      if (vaccination.getAdminDate() == null)
      {
        vaccination.setIdSubmitter("dqa-none-" + vaccination.getAdminCvxCode());
      } else
      {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        vaccination.setIdSubmitter("dqa-" + sdf.format(vaccination.getAdminDate()) + "-"
            + vaccination.getAdminCvxCode());
      }
      validator.documentParagraph("No vaccination id was found so it was auto generated as '" + vaccination.getIdSubmitter() + "'.");
    }
   
  }
  
}
