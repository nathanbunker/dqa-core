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
import org.openimmunizationsoftware.dqa.db.model.received.VaccinationVIS;
import org.openimmunizationsoftware.dqa.db.model.received.types.PatientAddress;
import org.openimmunizationsoftware.dqa.db.model.received.types.PatientIdNumber;
import org.openimmunizationsoftware.dqa.db.model.received.types.PatientImmunity;
import org.openimmunizationsoftware.dqa.db.model.received.types.PatientPhone;

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
    for (PatientIdNumber patientIdNumber : patient.getPatientIdNumberList())
    {
      session.save(patientIdNumber);
    }
    for (PatientPhone patientPhone : patient.getPatientPhoneList())
    {
      session.save(patientPhone);
    }
    for (PatientAddress patientAddress : patient.getPatientAddressList())
    {
      session.save(patientAddress);
    }
    for (PatientImmunity patientImmunity : patient.getPatientImmunityList())
    {
      session.save(patientImmunity);
    }
    MessageHeader messageHeader = messageReceived.getMessageHeader();
    messageHeader.setMessageReceived(messageReceived);
    session.save(messageHeader);
    for (Vaccination vaccination : messageReceived.getVaccinations())
    {
      vaccination.setMessageReceived(messageReceived);
      session.saveOrUpdate(vaccination);
      for (VaccinationVIS vaccinationVIS : vaccination.getVaccinationVisList())
      {
        vaccinationVIS.setVaccination(vaccination);
        session.saveOrUpdate(vaccinationVIS);
      }
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
