package org.openimmunizationsoftware.dqa.parse;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.dqa.SoftwareVersion;


public class HL7Util
{
  public static final String MESSAGE_TYPE_VXU = "VXU";
  public static final String MESSAGE_TYPE_QBP = "QBP";
  
  public static final String ACK_ERROR = "AE";
  public static final String ACK_ACCEPT = "AA";
  public static final String ACK_REJECT = "AR";
  
  public static final String SEVERITY_ERROR = "E";
  public static final String SEVERITY_WARNING = "W";
  public static final String SEVERITY_INFORMATION = "I";
  
  public static final String PROCESSING_ID_DEBUGGING = "D";
  public static final String PROCESSING_ID_PRODUCTION = "P";
  public static final String PROCESSING_ID_TRAINING = "T";

  public static final String QUERY_RESULT_NO_MATCHES = "Z34";
  public static final String QUERY_RESULT_LIST_OF_CANDIDATES = "Z31";
  public static final String QUERY_RESULT_IMMUNIZATION_HISTORY = "Z32";
  
  public static final String QUERY_RESPONSE_TYPE = "RSP^K11^RSP_K11";
  
  public static final int BAR = 0;
  public static final int CAR = 1;
  public static final int TIL = 2;
  public static final int SLA = 3;
  public static final int AMP = 4;
  
  private static int ackCount = 1;
  
  public static synchronized int getNextAckCount()
  {
    if (ackCount == Integer.MAX_VALUE)
    {
      ackCount = 1;
    }
    return ackCount++;
  }
  
  public static boolean setupSeparators(String messageText, char[] separators)
  {
    if (messageText.startsWith("MSH") && messageText.length() > 10)
    {
      separators[BAR] = messageText.charAt(BAR + 3);
      separators[CAR] = messageText.charAt(CAR + 3);
      separators[TIL] = messageText.charAt(TIL + 3);
      separators[SLA] = messageText.charAt(SLA + 3);
      separators[AMP] = messageText.charAt(AMP + 3);
      return true;
    } else
    {
      separators[BAR] = '|';
      separators[CAR] = '^';
      separators[TIL] = '~';
      separators[SLA] = '\\';
      separators[AMP] = '&';
      return false;
    }
  }
  
  public static String makeAckMessage(String ackType, String severityLevel, String message, PreParseMessageExaminer ppme)
  {
    String hl7ErrorCode = "0";
    StringBuilder ack = new StringBuilder();
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
    String messageDate = sdf.format(new Date());
    // MSH
    ack.append("MSH|^~\\&");
    ack.append("|" + ppme.getSendingApplication()); // MSH-3 Sending Application
    ack.append("|" + ppme.getSendingFacility()); // MSH-4 Sending Facility
    ack.append("|" + ppme.getReceivingApplication()); // MSH-5 Receiving Application
    ack.append("|" + ppme.getReceivingFacility()); // MSH-6 Receiving Facility
    ack.append("|" + messageDate); // MSH-7 Date/Time of Message
    ack.append("|"); // MSH-8 Security
    ack.append("|ACK"); // MSH-9
    // Message
    // Type
    ack.append("|" + messageDate + "." + getNextAckCount()); // MSH-10 Message
                                                             // Control ID
    ack.append("|P"); // MSH-11 Processing ID
    ack.append("|2.5.1"); // MSH-12 Version ID
    ack.append("|\r");
    ack.append("SFT|" + SoftwareVersion.VENDOR + "|" + SoftwareVersion.VERSION + "|" + SoftwareVersion.PRODUCT + "|" + SoftwareVersion.BINARY_ID
        + "|\r");
    ack.append("MSA|" + ackType + "|" + ppme.getMessageKey() + "|\r");
    makeERRSegment(ack, severityLevel, hl7ErrorCode, message);
    
    return ack.toString();

  }

  private static void makeERRSegment(StringBuilder ack, String severity, String hl7ErrorCode, String textMessage)
  {

    ack.append("ERR||");
    // 2 Error Location
    ack.append("|");
    // 3 HL7 Error Code
    ack.append(hl7ErrorCode);
    ack.append("|");
    // 4 Severity
    ack.append(severity);
    ack.append("|");
    // 5 Application Error Code
    ack.append("|");
    // 6 Application Error Parameter
    ack.append("|");
    // 7 Diagnostic Information
    ack.append("|");
    // 8 User Message
    ack.append("|");
    ack.append(escapeHL7Chars(textMessage));
    ack.append("|\r");

  }
  
  public static String escapeHL7Chars(String s)
  {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray())
    {
      if (c >= ' ')
      {
        switch (c) {
        case '~':
          sb.append("\\R\\");
          break;
        case '\\':
          sb.append("\\E\\");
          break;
        case '|':
          sb.append("\\F\\");
          break;
        case '^':
          sb.append("\\S\\");
          break;
        case '&':
          sb.append("\\T\\");
          break;
        default:
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }

}
