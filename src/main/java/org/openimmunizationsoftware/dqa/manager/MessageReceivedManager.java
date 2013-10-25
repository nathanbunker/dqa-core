package org.openimmunizationsoftware.dqa.manager;

import org.hibernate.Session;
import org.openimmunizationsoftware.dqa.db.model.IssueFound;
import org.openimmunizationsoftware.dqa.db.model.MessageHeader;
import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.SubmitStatus;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.db.model.received.NextOfKin;
import org.openimmunizationsoftware.dqa.db.model.received.Patient;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;

public class MessageReceivedManager
{
  public static void saveMessageReceived(SubmitterProfile profile, MessageReceived messageReceived, Session session)
  {
    messageReceived.setProfile(profile);
    messageReceived.setSubmitStatus(SubmitStatus.HOLD);
    session.saveOrUpdate(messageReceived);
    Patient patient = messageReceived.getPatient();
    patient.setMessageReceived(messageReceived);
    session.save(patient);
    MessageHeader messageHeader = messageReceived.getMessageHeader();
    messageHeader.setMessageReceived(messageReceived);
    session.save(messageHeader);
    for (Vaccination vaccination : messageReceived.getVaccinations())
    {
      vaccination.setMessageReceived(messageReceived);
      session.saveOrUpdate(vaccination);
    }
    for (NextOfKin nextOfKin : messageReceived.getNextOfKins())
    {
      nextOfKin.setMessageReceived(messageReceived);
      session.saveOrUpdate(nextOfKin);
    }
    for (IssueFound issueFound : messageReceived.getIssuesFound())
    {
      issueFound.setMessageReceived(messageReceived);
      session.saveOrUpdate(issueFound);
    }
  }
}
