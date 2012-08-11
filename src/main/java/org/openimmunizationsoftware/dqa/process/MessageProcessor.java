package org.openimmunizationsoftware.dqa.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.dqa.db.model.BatchReport;
import org.openimmunizationsoftware.dqa.db.model.CodeReceived;
import org.openimmunizationsoftware.dqa.db.model.CodeTable;
import org.openimmunizationsoftware.dqa.db.model.IssueAction;
import org.openimmunizationsoftware.dqa.db.model.MessageBatch;
import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.manager.CodesReceived;
import org.openimmunizationsoftware.dqa.manager.MessageReceivedManager;
import org.openimmunizationsoftware.dqa.parse.VaccinationUpdateParserHL7;
import org.openimmunizationsoftware.dqa.quality.QualityCollector;
import org.openimmunizationsoftware.dqa.validate.Validator;

public class MessageProcessor
{

  // set for webservice
  // results.setBatchId(qualityCollector.getMessageBatch().getBatchId());

  public static String processDebugOutput(Session session, SubmitterProfile profile, QualityCollector qualityCollector)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter out = new PrintWriter(stringWriter);
    if (qualityCollector != null)
    {
      processMessageBatch(out, qualityCollector);
    }
    out.print("\r");
    out.print("\r");
    CodesReceived cr = CodesReceived.getCodesReceived(profile, session);
    CodesReceived masterCr = CodesReceived.getCodesReceived();
    for (CodeTable codeTable : cr.getCodeTableList())
    {
      List<CodeReceived> codesReceived = cr.getCodesReceived(codeTable);
      if (codesReceived.size() > 0)
      {
        out.print("-- " + padSlash(codeTable.getTableLabel() + " ", 92) + "\r");
        out.print("\r");
        out.print("VALUES RECEIVED\r");
        out.print(pad("value", 20));
        out.print(pad("label", 30));
        out.print(pad("use instead", 20));
        out.print(pad("status", 15));
        out.print(pad("count", 7));
        out.print("\r");
        for (CodeReceived codeReceived : codesReceived)
        {
          out.print(pad(codeReceived.getReceivedValue(), 20));
          out.print(pad(codeReceived.getCodeLabel(), 30));
          if (codeReceived.getCodeValue() == null || (codeReceived.getCodeValue().equals(codeReceived.getReceivedValue())))
          {
            out.print(pad("", 20));
          } else
          {
            out.print(pad(codeReceived.getCodeValue(), 20));
          }
          out.print(pad(codeReceived.getCodeStatus().getCodeLabel(), 15));
          out.print(pad(String.valueOf(codeReceived.getReceivedCount()), 7));
          out.print("\r");
        }
        codesReceived = masterCr.getCodesReceived(codeTable);
        out.print("\r");
        out.print("MASTER VALUE LIST\r");
        out.print(pad("value", 20));
        out.print(pad("label", 30));
        out.print(pad("use instead", 20));
        out.print(pad("status", 15));
        out.print("\r");

        for (CodeReceived codeReceived : codesReceived)
        {
          out.print(pad(codeReceived.getReceivedValue(), 20));
          out.print(pad(codeReceived.getCodeLabel(), 30));
          if (codeReceived.getCodeValue() == null || (codeReceived.getCodeValue().equals(codeReceived.getReceivedValue())))
          {
            out.print(pad("", 20));
          } else
          {
            out.print(pad(codeReceived.getCodeValue(), 20));
          }
          out.print(pad(codeReceived.getCodeStatus().getCodeLabel(), 15));
          out.print("\r");
        }
        out.print("\r");
      }
    }
    return stringWriter.toString();
  }

  /**
   * @deprecated
   * @param debugFlag
   * @param parser
   * @param sb
   * @param profile
   * @param session
   * @param qualityCollector
   * @return
   */
  public static MessageReceived processMessage(boolean debugFlag, VaccinationUpdateParserHL7 parser, String sb, SubmitterProfile profile,
      Session session, QualityCollector qualityCollector)
  {
    MessageProcessRequest request = new MessageProcessRequest();
    request.setDebugFlag(debugFlag);
    request.setMessageText(sb);
    request.setParser(parser);
    request.setProfile(profile);
    request.setSession(session);
    request.setQualityCollector(qualityCollector);

    return processMessage(request).getMessageReceived();
  }

  public static MessageProcessResponse processMessage(MessageProcessRequest request)
  {
    MessageProcessResponse response = new MessageProcessResponse();
    MessageReceived messageReceived = new MessageReceived();
    messageReceived.setMessageKey(request.getMessageKey());
    response.setMessageReceived(messageReceived);
    messageReceived.setDebug(request.isDebugFlag());

    Transaction tx = request.getSession().beginTransaction();
    try
    {

      request.getProfile().initPotentialIssueStatus(request.getSession());
      messageReceived.setProfile(request.getProfile());
      messageReceived.setRequestText(request.getMessageText());
      request.getParser().createVaccinationUpdateMessage(messageReceived);
      if (request.getProfile().isProfileStatusTest()
          && messageReceived.getMessageHeader().getProcessingStatusCode()
              .equals(org.openimmunizationsoftware.dqa.db.model.MessageHeader.PROCESSING_ID_DEBUGGING))
      {
        messageReceived.setDebug(true);
      }
      if (!messageReceived.hasErrors())
      {
        Validator validator = new Validator(request.getProfile(), request.getSession());
        validator.validateVaccinationUpdateMessage(messageReceived, null);
      }
      request.getQualityCollector().registerProcessedMessage(messageReceived);

      String ackMessage = request.getParser().makeAckMessage(messageReceived);
      messageReceived.setResponseText(ackMessage);
      messageReceived.setIssueAction(IssueAction.ACCEPT);
      MessageReceivedManager.saveMessageReceived(request.getProfile(), messageReceived, request.getSession());

      tx.commit();
      messageReceived.setSuccessful(true);

    } catch (Exception exception)
    {
      tx.rollback();
      String ackMessage = "MSH|^~\\&|||||201105231008000||ACK^|201105231008000|P|2.3.1|\r" + "MSA|AE|TODO|Exception occurred: "
          + exception.getMessage() + "|\r";
      messageReceived.setResponseText(ackMessage);
      messageReceived.setSuccessful(false);
      messageReceived.setException(exception);
    }
    return response;
  }

  private static final String PAD = "                                                                                                          ";

  private static String pad(String s, int size)
  {
    s += PAD;
    return s.substring(0, size - 1) + " ";
  }

  private static final String PAD_SLASH = "-----------------------------------------------------------------------------------------------------";

  private static String padSlash(String s, int size)
  {
    s += PAD_SLASH;
    return s.substring(0, size - 1) + "-";
  }

  private static void processMessageBatch(PrintWriter out, QualityCollector qualityCollector)
  {
    MessageBatch mb = qualityCollector.getMessageBatch();
    BatchReport r = mb.getBatchReport();
    out.print("\r");
    out.print("\r");
    out.print("Message Batch Summary: \r");
    out.print("Message Count:       " + r.getMessageCount() + "\r");
    out.print("Patient Count:       " + r.getMessageCount() + "\r");
    out.print("Vaccination      \r");
    out.print(" + Administered:     " + r.getVaccinationAdministeredCount() + "\r");
    out.print(" + Historical:       " + r.getVaccinationHistoricalCount() + "\r");
    out.print(" + Not Administered: " + r.getVaccinationNotAdministeredCount() + "\r");
    out.print(" + Deleted:          " + r.getVaccinationDeleteCount() + "\r");
  }
}
