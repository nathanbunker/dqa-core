package org.openimmunizationsoftware.dqa.db.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.dqa.db.model.received.NextOfKin;
import org.openimmunizationsoftware.dqa.db.model.received.Patient;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;

public class MessageReceived implements Serializable
{
  
  private static final long serialVersionUID = 1l;
  
  private IssueAction issueAction = null;
  private List<IssueFound> issuesFound = new ArrayList<IssueFound>();
  private List<NextOfKin> nextOfKins = new ArrayList<NextOfKin>();
  private Patient patient = new Patient();
  private SubmitterProfile profile = null;
  private Date receivedDate = new Date();
  private long receivedId = 0l;
  private String requestText = null;
  private String responseText = null;
  private SubmitStatus submitStatus = null;
  private Map<String, Vaccination> vaccinationMap = new HashMap<String, Vaccination>();
  private List<Vaccination> vaccinations = new ArrayList<Vaccination>();
  private MessageHeader messageHeader = new MessageHeader();
  private boolean debugOn = false;
  private boolean successfulCompletion = true;
  private Exception exception = null;
  private String messageKey = "";

  public String getMessageKey()
  {
    return messageKey;
  }

  public void setMessageKey(String messageKey)
  {
    this.messageKey = messageKey;
  }

  public void setMessageHeader(MessageHeader header)
  {
    this.messageHeader = header;
  }

  public MessageHeader getMessageHeader()
  {
    return messageHeader;
  }

  public IssueAction getIssueAction()
  {
    return issueAction;
  }

  public List<IssueFound> getIssuesFound()
  {
    return issuesFound;
  }

  public List<NextOfKin> getNextOfKins()
  {
    return nextOfKins;
  }

  public Patient getPatient()
  {
    return patient;
  }

  public SubmitterProfile getProfile()
  {
    return profile;
  }

  public Date getReceivedDate()
  {
    return receivedDate;
  }

  public long getReceivedId()
  {
    return receivedId;
  }

  public String getRequestText()
  {
    return requestText;
  }

  public String getResponseText()
  {
    return responseText;
  }

  public SubmitStatus getSubmitStatus()
  {
    return submitStatus;
  }

  public Map<String, Vaccination> getVaccinationMap()
  {
    return vaccinationMap;
  }

  public List<Vaccination> getVaccinations()
  {
    return vaccinations;
  }

  public boolean hasErrors()
  {
    for (IssueFound issueFound : issuesFound)
    {
      if (issueFound.getIssueAction().equals(IssueAction.ERROR))
      {
        return true;
      }
    }
    return false;
  }

  public boolean hasWarns()
  {
    for (IssueFound issueFound : issuesFound)
    {
      if (issueFound.getIssueAction().equals(IssueAction.WARN))
      {
        return true;
      }
    }
    return false;
  }

  public void setIssueAction(IssueAction issueAction)
  {
    this.issueAction = issueAction;
  }

  public void setIssuesFound(List<IssueFound> issuesFound)
  {
    this.issuesFound = issuesFound;
  }

  public void setNextOfKins(List<NextOfKin> nextOfKins)
  {
    this.nextOfKins = nextOfKins;
  }

  public void setPatient(Patient patient)
  {
    this.patient = patient;
  }

  public void setProfile(SubmitterProfile profile)
  {
    this.profile = profile;
  }

  public void setReceivedDate(Date receivedDate)
  {
    this.receivedDate = receivedDate;
  }

  public void setReceivedId(long receivedId)
  {
    this.receivedId = receivedId;
  }

  public void setRequestText(String requestText)
  {
    this.requestText = requestText;
  }

  public void setResponseText(String responseText)
  {
    this.responseText = responseText;
  }

  public void setSubmitStatus(SubmitStatus submitStatus)
  {
    this.submitStatus = submitStatus;
  }

  public void setVaccinationMap(Map<String, Vaccination> vaccinationMap)
  {
    this.vaccinationMap = vaccinationMap;
  }

  public void setVaccinations(List<Vaccination> vaccinations)
  {
    this.vaccinations = vaccinations;
  }
  
  public void setDebug(boolean debug)
  {
	this.debugOn = debug;
  }

  public boolean isDebugOn()
  {
	return this.debugOn;
  }
  public void setSuccessful(boolean success)
  {
    this.successfulCompletion = success;
  }

  public boolean isSuccessful()
  {
    return this.successfulCompletion;
  }
  public Exception getException()
  {
    return exception;
  }
  public void setException(Exception e)
  {
    this.exception = e;
  }
}
