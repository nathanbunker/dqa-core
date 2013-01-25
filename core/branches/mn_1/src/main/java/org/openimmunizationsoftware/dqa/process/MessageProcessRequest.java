package org.openimmunizationsoftware.dqa.process;

import org.hibernate.Session;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.parse.VaccinationUpdateParserHL7;
import org.openimmunizationsoftware.dqa.quality.QualityCollector;

public class MessageProcessRequest
{

  private boolean debugFlag = false;
  private VaccinationUpdateParserHL7 parser = null;
  private String messageText = null;
  private SubmitterProfile profile = null;
  private Session session = null;
  private QualityCollector qualityCollector = null;
  private String messageKey = "";
  
  public String getMessageKey()
  {
    return messageKey;
  }
  public void setMessageKey(String messageKey)
  {
    this.messageKey = messageKey;
  }
  public boolean isDebugFlag()
  {
    return debugFlag;
  }
  public void setDebugFlag(boolean debugFlag)
  {
    this.debugFlag = debugFlag;
  }
  public VaccinationUpdateParserHL7 getParser()
  {
    return parser;
  }
  public void setParser(VaccinationUpdateParserHL7 parser)
  {
    this.parser = parser;
  }
  public String getMessageText()
  {
    return messageText;
  }
  public void setMessageText(String sb)
  {
    this.messageText = sb;
  }
  public SubmitterProfile getProfile()
  {
    return profile;
  }
  public void setProfile(SubmitterProfile profile)
  {
    this.profile = profile;
  }
  public Session getSession()
  {
    return session;
  }
  public void setSession(Session session)
  {
    this.session = session;
  }
  public QualityCollector getQualityCollector()
  {
    return qualityCollector;
  }
  public void setQualityCollector(QualityCollector qualityCollector)
  {
    this.qualityCollector = qualityCollector;
  }
  
}
