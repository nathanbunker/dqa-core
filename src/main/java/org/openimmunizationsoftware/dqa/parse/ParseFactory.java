package org.openimmunizationsoftware.dqa.parse;

import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.validate.ValidateMessage;



public class ParseFactory
{
  public static ValidateMessage getVaccinationUpdateParser(SubmitterProfile profile)
  {
    return new VaccinationParserHL7(profile);
  }
}
