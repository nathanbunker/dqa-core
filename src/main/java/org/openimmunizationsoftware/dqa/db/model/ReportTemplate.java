package org.openimmunizationsoftware.dqa.db.model;

import java.io.Serializable;

public class ReportTemplate implements Serializable
{
  
  private static final long serialVersionUID = 1l;
  
  private int templateId = 0;
  private String templateLabel = "";
  private ReportType reportType = null;
  private String reportDefinition = "";
  private SubmitterProfile baseProfile = null;
  private String testCaseScript = null;

  public String getTestCaseScript()
  {
    return testCaseScript;
  }

  public void setTestCaseScript(String testCaseScript)
  {
    this.testCaseScript = testCaseScript;
  }

  public int getTemplateId()
  {
    return templateId;
  }

  public void setTemplateId(int templateId)
  {
    this.templateId = templateId;
  }

  public String getTemplateLabel()
  {
    return templateLabel;
  }

  public void setTemplateLabel(String templateLabel)
  {
    this.templateLabel = templateLabel;
  }

  public ReportType getReportType()
  {
    return reportType;
  }

  public void setReportType(ReportType reportType)
  {
    this.reportType = reportType;
  }

  public String getReportDefinition()
  {
    return reportDefinition;
  }

  public void setReportDefinition(String reportDefinition)
  {
    this.reportDefinition = reportDefinition;
  }

  public SubmitterProfile getBaseProfile()
  {
    return baseProfile;
  }

  public void setBaseProfile(SubmitterProfile baseProfile)
  {
    this.baseProfile = baseProfile;
  }
}
