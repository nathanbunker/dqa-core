package org.openimmunizationsoftware.dqa.parse;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.quality.QualityCollector;
import org.openimmunizationsoftware.dqa.validate.ValidateMessage;


public abstract class VaccinationUpdateParser extends ValidateMessage
{
  public VaccinationUpdateParser(SubmitterProfile profile)
  {
    super(profile);
  }
  
  public abstract void createVaccinationUpdateMessage(MessageReceived messageReceived);
}
