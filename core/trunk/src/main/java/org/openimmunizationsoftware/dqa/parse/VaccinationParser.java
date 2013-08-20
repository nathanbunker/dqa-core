package org.openimmunizationsoftware.dqa.parse;

import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.QueryReceived;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.quality.QualityCollector;
import org.openimmunizationsoftware.dqa.validate.ValidateMessage;


public abstract class VaccinationParser extends ValidateMessage
{
  public VaccinationParser(SubmitterProfile profile)
  {
    super(profile);
  }
  
  public abstract void createVaccinationUpdateMessage(MessageReceived messageReceived);
  
  public abstract void createQueryMessage(QueryReceived queryReceived);
}
