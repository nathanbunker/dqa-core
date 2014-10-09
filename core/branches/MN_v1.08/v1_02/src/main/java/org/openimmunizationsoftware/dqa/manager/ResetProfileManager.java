package org.openimmunizationsoftware.dqa.manager;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.dqa.db.model.CodeMaster;
import org.openimmunizationsoftware.dqa.db.model.CodeReceived;
import org.openimmunizationsoftware.dqa.db.model.CodeStatus;
import org.openimmunizationsoftware.dqa.db.model.IssueAction;
import org.openimmunizationsoftware.dqa.db.model.PotentialIssue;
import org.openimmunizationsoftware.dqa.db.model.PotentialIssueStatus;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;

public class ResetProfileManager
{
  public void resetProfile(SubmitterProfile profile, Session session)
  {
    Transaction trans = session.beginTransaction();
    SubmitterProfile parentProfile = null;
    if (profile.getReportTemplate() != null)
    {
      parentProfile = profile.getReportTemplate().getBaseProfile();
    }

    // Go through each code received value and update it. 
    Query query = session.createQuery("from CodeReceived where profile = ?");
    query.setParameter(0, profile);
    List<CodeReceived> codeReceivedList = query.list();
    for (CodeReceived codeReceived : codeReceivedList)
    {
      CodeStatus codeStatus = null;
      if (parentProfile != null)
      {
        // Look for code status first on the parent profile level
        query = session.createQuery("from CodeReceived where profile = ? and table = ? and receivedValue = ?");
        query.setParameter(0, parentProfile);
        query.setParameter(1, codeReceived.getTable());
        query.setParameter(2, codeReceived.getReceivedValue());
        List<CodeReceived> parentCodeReceivedList = query.list();
        if (parentCodeReceivedList.size() > 0)
        {
          CodeReceived parentCodeReceived = parentCodeReceivedList.get(0);
          codeStatus = parentCodeReceived.getCodeStatus();
        }
      }
      if (codeStatus == null)
      {
        // Was not found at parent profile level, or this is a template
        // Pull from code master 
        query = session.createQuery("from CodeMaster where table = ? and codeValue = ?");
        query.setParameter(0, codeReceived.getTable());
        query.setParameter(1, codeReceived.getReceivedValue());
        List<CodeMaster> codeMasterList = query.list();
        if (codeMasterList.size() > 0)
        {
          CodeMaster codeMaster = codeMasterList.get(0);
          codeStatus = codeMaster.getCodeStatus();
        }
      }
      if (codeStatus != null && !codeStatus.equals(codeReceived.getCodeStatus()))
      {
        codeReceived.setCodeStatus(codeStatus);
        session.update(codeReceived);
      }
    }

    // Now go through each potential issue status and update it
    query = session.createQuery("from PotentialIssueStatus where profile = ?");
    query.setParameter(0, profile);
    List<PotentialIssueStatus> potentialIssueStatusList = query.list();
    for (PotentialIssueStatus potentialIssueStatus : potentialIssueStatusList)
    {
      IssueAction issueAction = null;
        
      if (parentProfile != null)
      {
        query = session.createQuery("from PotentialIssueStatus where profile = ? and issue = ?");
        query.setParameter(0, parentProfile);
        query.setParameter(1, potentialIssueStatus.getIssue());
        List<PotentialIssueStatus> parentPotentialIssueStatusList = query.list();
        if (parentPotentialIssueStatusList.size() > 0)
        {
          PotentialIssueStatus parentPotentialIssueStatus = parentPotentialIssueStatusList.get(0);
          issueAction = parentPotentialIssueStatus.getAction();
        }
      }
      if (issueAction == null)
      {
        query = session.createQuery("from PotentialIssue where issue = ?");
        query.setParameter(0, potentialIssueStatus.getIssue());
        List<PotentialIssue> potentialIssueList = query.list();
        if (potentialIssueList.size() > 0)
        {
          PotentialIssue potentialIssue = potentialIssueList.get(0);
          issueAction = potentialIssue.getDefaultIssueAction();
        }
      }
      if (issueAction != null && !issueAction.equals(potentialIssueStatus.getIssue()))
      {
        potentialIssueStatus.setAction(issueAction);
        session.update(potentialIssueStatus);
      }
    }
        
    trans.commit();
  }
}
