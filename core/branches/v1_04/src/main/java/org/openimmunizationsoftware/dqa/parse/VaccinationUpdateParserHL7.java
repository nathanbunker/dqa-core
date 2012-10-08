package org.openimmunizationsoftware.dqa.parse;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.dqa.SoftwareVersion;
import org.openimmunizationsoftware.dqa.db.model.IssueFound;
import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.MessageHeader;
import org.openimmunizationsoftware.dqa.db.model.PotentialIssue;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.db.model.received.NextOfKin;
import org.openimmunizationsoftware.dqa.db.model.received.Observation;
import org.openimmunizationsoftware.dqa.db.model.received.Patient;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;
import org.openimmunizationsoftware.dqa.db.model.received.types.Address;
import org.openimmunizationsoftware.dqa.db.model.received.types.CodedEntity;
import org.openimmunizationsoftware.dqa.db.model.received.types.Id;
import org.openimmunizationsoftware.dqa.db.model.received.types.Name;
import org.openimmunizationsoftware.dqa.db.model.received.types.OrganizationName;
import org.openimmunizationsoftware.dqa.db.model.received.types.PhoneNumber;
import org.openimmunizationsoftware.dqa.manager.PotentialIssues;

public class VaccinationUpdateParserHL7 extends VaccinationUpdateParser
{

  public static final String ACK_ACCEPT = "AA";
  public static final String ACK_ERROR = "AE";
  public static final String ACK_REJECT = "AR";
  public static final String PROCESSING_ID_DEBUG = "D";

  private char[] separators = new char[5];
  private static final int BAR = 0;
  private static final int CAR = 1;
  private static final int TIL = 2;
  private static final int SLA = 3;
  private static final int AMP = 4;

  public VaccinationUpdateParserHL7(SubmitterProfile profile) {
    super(profile);
  }

  private int startField = 0;
  private int currentSegmentPos = -1;
  private String segmentName = "";
  private List<List<String>> segments;
  private List<String> currentSegment;
  private int vaccinationCount = 0;
  private int nextOfKinCount = 0;

  private void setup()
  {
    startField = 0;
    currentSegmentPos = -1;
    segmentName = "";
    segments = new ArrayList<List<String>>();
    currentSegment = new ArrayList<String>();

    patient = null;
    vaccination = null;
    nextOfKin = null;

    vaccinationCount = 0;
    nextOfKinCount = 0;

    pi = PotentialIssues.getPotentialIssues();
  }

  private static final String[] RECOGNIZED_SEGMENTS = { "MSH", "SFT", "PID", "PD1", "NK1", "PV1", "IN1", "IN3", "IN4", "PD2", "RXA", "ORC", "OBX",
      "BTS", "FTS", "FHS", "BHS", "RXR", "GT1" };

  @Override
  public void createVaccinationUpdateMessage(MessageReceived messageReceived)
  {
    String messageText = messageReceived.getRequestText();
    message = messageReceived;
    issuesFound = message.getIssuesFound();
    setup();
    readSeparators(messageText);
    readFields(messageText);

    patient = message.getPatient();
    currentSegment = segments.get(0);
    boolean foundPID = false;
    boolean foundPV1 = false;
    populateMSH(message);
    while (moveNext())
    {
      if (segmentName.equals("PID"))
      {
        if (foundPID)
        {
          registerIssue(pi.Hl7PidSegmentIsRepeated);
        } else
        {
          foundPID = true;
          populatePID(message);
        }
      } else if (segmentName.equals("PV1"))
      {
        if (foundPV1)
        {
          registerIssue(pi.Hl7Pv1SegmentIsRepeated);
        } else
        {
          foundPID = assertPIDFound(foundPID);
          populatePV1(message);
          foundPV1 = true;
        }
      } else if (segmentName.equals("PD1"))
      {
        foundPID = assertPIDFound(foundPID);
        populatePD1(message);
      } else if (segmentName.equals("NK1"))
      {
        if (foundPV1)
        {
          registerIssue(pi.Hl7SegmentsOutOfOrder);
        }
        foundPID = assertPIDFound(foundPID);
        nextOfKinCount++;
        nextOfKin = new NextOfKin();
        skippableItem = nextOfKin;
        nextOfKin.setPositionId(nextOfKinCount);
        message.getNextOfKins().add(nextOfKin);
        nextOfKin.setReceivedId(message.getNextOfKins().size());
        populateNK1(message);
      } else if (segmentName.equals("ORC") || segmentName.equals("RXA"))
      {
        if (!foundPV1)
        {
          registerIssue(pi.Hl7Pv1SegmentIsMissing);
          foundPV1 = true;
        }
        foundPID = assertPIDFound(foundPID);
        if (vaccination == null)
        {
          positionId = 0;
        }
        positionId++;
        vaccinationCount++;
        vaccination = new Vaccination();
        skippableItem = vaccination;
        vaccination.setPositionId(vaccinationCount);
        message.getVaccinations().add(vaccination);
        if (segmentName.equals("ORC"))
        {
          populateORC(messageReceived);
          if (!moveNext() || !segmentName.equals("RXA"))
          {
            registerIssue(pi.Hl7RxaSegmentIsMissing);
            moveBack();
            continue;
          }
        } else
        {
          if (!message.getMessageHeader().getMessageVersion().startsWith("2.3") && !message.getMessageHeader().getMessageVersion().startsWith("2.4"))
          {
            registerIssue(pi.Hl7OrcSegmentIsMissing);
          }
        }
        populateRXA(message);
      } else if (segmentName.equals("RXR"))
      {
        if (vaccination == null)
        {
          registerIssue(pi.Hl7RxaSegmentIsMissing);
          continue;
        }
        populateRXR(message);
      } else if (segmentName.equals("OBX"))
      {
        if (vaccination == null)
        {
          registerIssue(pi.Hl7RxaSegmentIsMissing);
          continue;
        }
        populateOBX(message);
      } else
      {
        if (segmentName.length() > 0 && segmentName.charAt(0) > ' ')
        {
          boolean recognized = false;
          for (String recognizedSegment : RECOGNIZED_SEGMENTS)
          {
            if (recognizedSegment.equals(segmentName))
            {
              recognized = true;
              continue;
            }
          }
          if (!recognized)
          {
            registerIssue(pi.Hl7SegmentIsUnrecognized);
          }
        }
      }
    }
    positionId = 0;
    assertPIDFound(foundPID);
    if (!foundPV1)
    {
      registerIssue(pi.Hl7Pv1SegmentIsMissing);
    }
  }

  private boolean assertPIDFound(boolean foundPID)
  {
    if (!foundPID)
    {
      registerIssue(pi.Hl7PidSegmentIsMissing);
      foundPID = true;
    }
    return foundPID;
  }

  private boolean moveNext()
  {
    currentSegmentPos++;
    while (currentSegmentPos < segments.size())
    {
      currentSegment = segments.get(currentSegmentPos);
      if (currentSegment.size() > 0)
      {
        segmentName = currentSegment.get(0);
        return true;
      }
      currentSegmentPos++;
    }
    return false;
  }

  private void moveBack()
  {
    currentSegmentPos--;
    while (currentSegmentPos >= 0)
    {
      currentSegment = segments.get(currentSegmentPos);
      if (currentSegment.size() > 0)
      {
        segmentName = currentSegment.get(0);
        return;
      }
      currentSegmentPos--;
    }

  }

  private void populateMSH(MessageReceived message)
  {
    MessageHeader header = message.getMessageHeader();
    header.setSendingApplication(getValue(3));
    header.setSendingFacility(getValue(4));
    header.setReceivingApplication(getValue(5));
    header.setReceivingFacility(getValue(6));
    header.setMessageDate(getValueDate(7, pi.Hl7MshMessageDateIsInvalid));
    String[] field = getValues(9);
    header.setMessageType(field.length >= 1 ? field[0] : "");
    header.setMessageTrigger(field.length >= 2 ? field[1] : "");
    header.setMessageStructure(field.length >= 3 ? field[2] : "");
    header.setMessageControl(getValue(10));
    header.setProcessingIdCode(getValue(11));
    header.setMessageVersion(getValue(12));
    header.setAckTypeAcceptCode(getValue(15));
    header.setAckTypeApplicationCode(getValue(16));
    header.setCountryCode(getValue(17));
    header.setCharacterSetCode(getValue(18));
    header.setCharacterSetAltCode(getValue(20));
    header.setMessageProfile(getValue(21));
  }

  private void populatePID(MessageReceived message)
  {

    readAddress(11, patient.getAddress());
    // private Name alias = new Name();
    patient.setBirthDate(getValueDate(7, pi.PatientBirthDateIsInvalid));
    patient.setBirthMultiple(getValue(24));
    patient.setBirthOrderCode(getValue(25));
    patient.setBirthPlace(getValue(23));
    patient.setDeathDate(getValueDate(29, pi.PatientDeathDateIsInvalid));
    patient.setDeathIndicator(getValue(30));
    readCodeEntity(22, patient.getEthnicity());
    // TODO private OrganizationName facility = new OrganizationName();
    readPatientId(patient);
    patient.setMotherMaidenName(getValue(6));
    readName(5, patient.getName());
    readPhoneNumber(13, patient.getPhone());
    readCodeEntity(15, patient.getPrimaryLanguage());
    readCodeEntity(10, patient.getRace());
    patient.setSexCode(getValue(8));
  }

  private void populatePD1(MessageReceived message)
  {
    patient.setRegistryStatusCode(getValue(16));
    patient.setPublicityCode(getValue(11));
    patient.setProtectionCode(getValue(12));
    // TODO private OrganizationName facility = new OrganizationName();
    // TODO private Id idSubmitter = new Id();
    // TODO private Id physician = new Id();
  }

  private void populateNK1(MessageReceived message)
  {
    String setId = getValue(1);
    if (setId.equals(""))
    {
      registerIssue(pi.Hl7Nk1SetIdIsMissing);
    }
    readAddress(4, nextOfKin.getAddress());
    readPhoneNumber(5, nextOfKin.getPhone());
    readCodeEntity(3, nextOfKin.getRelationship());
    readName(2, nextOfKin.getName());
  }

  private void populateRXA(MessageReceived message)
  {
    registerIssueIfEmpty(1, pi.Hl7RxaGiveSubIdIsMissing);
    registerIssueIfEmpty(2, pi.Hl7RxaAdminSubIdCounterIsMissing);
    vaccination.setAdminDate(getValueDate(3, pi.VaccinationAdminDateIsInvalid));
    vaccination.setAdminDateEnd(getValueDate(4, null));
    readCodeEntity(5, vaccination.getAdmin());
    CodedEntity admin = vaccination.getAdmin();
    readCptCvxCodes(admin);
    vaccination.setAmount(getValue(6));
    vaccination.setAmountUnitCode(getValue(7));
    readCodeEntity(9, vaccination.getInformationSource());
    // TODO 10 XCN private Id givenBy = new Id();
    readLocationWithAddress(11, vaccination.getFacility());
    vaccination.setLotNumber(getValue(15));
    vaccination.setExpirationDate(getValueDate(16, pi.VaccinationLotExpirationDateIsInvalid));
    readCodeEntity(17, vaccination.getManufacturer());
    readCodeEntity(18, vaccination.getRefusal());
    vaccination.setCompletionCode(getValue(20));
    vaccination.setActionCode(getValue(21));
    vaccination.setSystemEntryDate(getValueDate(22, pi.VaccinationSystemEntryTimeIsInvalid));
  }

  private void populateRXR(MessageReceived message)
  {
    readCodeEntity(1, vaccination.getBodyRoute());
    readCodeEntity(2, vaccination.getBodySite());
  }

  private void populateOBX(MessageReceived message)
  {
    Observation obs = new Observation();
    vaccination.getObservations().add(obs);
    readCodeEntity(2, obs.getValueType());
    readCodeEntity(3, obs.getObservationIdentifier());
    obs.setObservationValue(getValue(5));
  }

  private void readCptCvxCodes(CodedEntity admin)
  {
    boolean codeFound = false;
    if (admin.getTable().equals("CVX") || admin.getTable().equals("HL70292"))
    {
      vaccination.getAdminCvx().setCode(admin.getCode());
      vaccination.getAdminCvx().setText(admin.getText());
      vaccination.getAdminCvx().setTable(admin.getTable());
      codeFound = true;
    } else if (admin.getAltTable().equals("CVX") || admin.getAltTable().equals("HL70292"))
    {
      vaccination.getAdminCvx().setCode(admin.getAltCode());
      vaccination.getAdminCvx().setText(admin.getAltText());
      vaccination.getAdminCvx().setTable(admin.getAltTable());
      codeFound = true;
    }
    if (admin.getTable().equals("CPT") || admin.getTable().equals("C4"))
    {
      vaccination.getAdminCpt().setCode(admin.getCode());
      vaccination.getAdminCpt().setText(admin.getText());
      vaccination.getAdminCpt().setTable(admin.getTable());
      codeFound = true;
    } else if (admin.getAltTable().equals("CPT") || admin.getAltTable().equals("C4"))
    {
      vaccination.getAdminCpt().setCode(admin.getAltCode());
      vaccination.getAdminCpt().setText(admin.getAltText());
      vaccination.getAdminCpt().setTable(admin.getAltTable());
      codeFound = true;
    }
    if (!codeFound)
    {
      if (admin.getCode().equals(""))
      {
        registerIssue(pi.VaccinationAdminCodeTableIsMissing);
      } else
      {
        registerIssue(pi.VaccinationAdminCodeTableIsInvalid);
      }
      String possible = admin.getCode();
      if (possible != null)
      {
        try
        {
          Integer.parseInt(possible);
          if (possible.length() >= 2 && possible.length() <= 3)
          {
            vaccination.getAdminCvx().setCode(admin.getCode());
            vaccination.getAdminCvx().setText(admin.getText());
            vaccination.getAdminCvx().setTable(admin.getTable());
          } else if (possible.length() == 5 && possible.startsWith("90"))
          {
            vaccination.getAdminCpt().setCode(admin.getCode());
            vaccination.getAdminCpt().setText(admin.getText());
            vaccination.getAdminCpt().setTable(admin.getTable());
          }
        } catch (NumberFormatException nfe)
        {
          // not CVX
        }
      }
    }

  }

  private void populateORC(MessageReceived message)
  {
    readCodeEntity(1, vaccination.getOrderControl());
    vaccination.setIdPlacer(getValue(2));
    vaccination.setIdSubmitter(getValue(3));
    readCodeEntity(28, vaccination.getConfidentiality());
  }

  private void populatePV1(MessageReceived message)
  {
    patient.setPatientClassCode(getValue(2));
    String[] field = getValues(20);
    if (field.length > 0)
    {
      patient.setFinancialEligibilityCode(field[0]);
      if (field.length > 1)
      {
        patient.setFinancialEligibilityDate(createDate(pi.PatientVfcEffectiveDateIsInvalid, field[1]));
      }
    }
  }

  private void readPhoneNumber(int fieldNumber, PhoneNumber phoneNumber)
  {
    String[] field = getValues(fieldNumber);
    if (field.length == 0)
    {
      return;
    }
    phoneNumber.setNumber(field.length >= 1 ? field[0] : "");
    if (field.length >= 2 && field[1].length() > 0)
    {
      phoneNumber.setTelUseCode(field[1]);
    }
    if (field.length >= 3 && field[2].length() > 0)
    {
      phoneNumber.setTelEquipCode(field[2]);
    }
    if (field.length >= 4 && field[3].length() > 0)
    {
      phoneNumber.setEmail(field[3]);
    }
    if (field.length >= 5 && field[4].length() > 0)
    {
      phoneNumber.setCountryCode(field[4]);
    }
    if (field.length >= 6 && field[5].length() > 0)
    {
      phoneNumber.setAreaCode(field[5]);
    }
    if (field.length >= 7 && field[6].length() > 0)
    {
      phoneNumber.setLocalNumber(field[6]);
    }
    if (field.length >= 8 && field[7].length() > 0)
    {
      phoneNumber.setExtension(field[7]);
    }
  }

  private void readAddress(int fieldNumber, Address address)
  {
    String[] field = getValues(fieldNumber);
    address.setStreet(field.length >= 1 ? field[0] : "");
    address.setStreet2(field.length >= 2 ? field[1] : "");
    address.setCity(field.length >= 3 ? field[2] : "");
    address.setStateCode(field.length >= 4 ? field[3] : "");
    address.setZip(field.length >= 5 ? field[4] : "");
    address.setCountryCode(field.length >= 6 ? field[5] : "");
    address.setTypeCode(field.length >= 7 ? field[6] : "");
    address.setCountyParishCode(field.length >= 9 ? field[8] : "");
  }

  private void readPatientId(Patient patient)
  {
    List<String[]> values = getRepeatValues(3);
    if (values.size() > 0)
    {
      String[] fields = values.get(0);
      readId(fields, patient.getIdSubmitter());
    }
    for (int i = 1; i < values.size(); i++)
    {
      String[] fields = values.get(i);
      if (fields.length >= 5)
      {
        String typeCode = fields[4];
        if ("SS".equals(typeCode))
        {
          readId(fields, patient.getIdSsn());
        } else if ("MA".equals(typeCode))
        {
          readId(fields, patient.getIdMedicaid());
        }
      }
    }
  }

  private void readId(String[] fields, Id id)
  {
    id.setNumber(fields.length >= 1 ? fields[0] : "");
    id.setAssigningAuthorityCode(fields.length >= 4 ? fields[3] : "");
    id.setTypeCode(fields.length >= 5 ? fields[4] : "");
  }

  private void readCodeEntity(int fieldNumber, CodedEntity ce)
  {
    String[] field = getValues(fieldNumber);
    ce.setCode(field.length >= 1 ? field[0] : "");
    ce.setText(field.length >= 2 ? field[1] : "");
    ce.setTable(field.length >= 3 ? field[2] : "");
    ce.setAltCode(field.length >= 4 ? field[3] : "");
    ce.setAltText(field.length >= 5 ? field[4] : "");
    ce.setAltTable(field.length >= 6 ? field[5] : "");
  }

  private void readLocationWithAddress(int fieldNumber, OrganizationName orgName)
  {
    String[] field = getValues(fieldNumber);
    orgName.getId().setNumber(field.length >= 4 ? field[3] : "");
    if (orgName.getId().getNumber().isEmpty())
    {
      orgName.getId().setNumber(field.length >= 1 ? field[0] : "");
    }
    orgName.setName(field.length >= 4 ? field[3] : "");
  }

  private void readName(int fieldNumber, Name name)
  {
    String[] field = getValues(fieldNumber);
    name.setLast(field.length >= 1 ? field[0] : "");
    name.setFirst(field.length >= 2 ? field[1] : "");
    name.setMiddle(field.length >= 3 ? field[2] : "");
    name.setSuffix(field.length >= 4 ? field[3] : "");
    name.setPrefix(field.length >= 5 ? field[4] : "");
    name.setTypeCode(field.length >= 7 ? field[6] : "");
  }

  private void readFields(String messageText)
  {
    int totalLength = messageText.length();
    startField = 0;
    for (int i = 0; i < totalLength; i++)
    {
      char c = messageText.charAt(i);
      if (c < ' ')
      {
        // end of segment, break
        currentSegment.add(messageText.substring(startField, i));
        segments.add(currentSegment);
        currentSegment = new ArrayList<String>();
        while (c < ' ' && i < (totalLength - 1))
        {
          i++;
          c = messageText.charAt(i);
        }
        startField = i;
      } else if (c == separators[BAR])
      {
        String fieldValue = messageText.substring(startField, i);
        currentSegment.add(fieldValue);
        if (currentSegment.size() == 1 && fieldValue.equals("MSH"))
        {
          // MSH is a special case where the first separator is the first
          // field.
          currentSegment.add(String.valueOf(separators[0]));
        }
        startField = i + 1;
      }
    }
    // The last segment should have ended with a \r so the startField would be
    // equal to the length of the message. If not, then there is a mistake, but
    // the last line should still be added on as is.
    if (startField < messageText.length())
    {
      currentSegment.add(messageText.substring(startField));
    }
    // This shouldn't happen, unless no \r is sent at the end of the last
    // segment.
    if (currentSegment.size() > 0)
    {
      segments.add(currentSegment);
    }
  }

  private Date getValueDate(int fieldNumber, PotentialIssue pi)
  {
    String fieldValue = getValue(fieldNumber);
    return createDate(pi, fieldValue);
  }

  private Date createDate(PotentialIssue pi, String fieldValue)
  {
    if (fieldValue.equals(""))
    {
      return null;
    }
    if (fieldValue.length() < 8)
    {
      if (pi != null)
      {
        registerIssue(pi);
      }
      return null;
    }
    if (fieldValue.length() < 14)
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      sdf.setLenient(false);
      try
      {
        return sdf.parse(fieldValue.substring(0, 8));
      } catch (java.text.ParseException e)
      {
        if (pi != null)
        {
          registerIssue(pi);
        }
        return null;
      }
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    sdf.setLenient(false);
    try
    {
      return sdf.parse(fieldValue.substring(0, 14));
    } catch (java.text.ParseException e)
    {
      if (pi != null)
      {
        registerIssue(pi);
      }
      return null;
    }
  }

  private void registerIssueIfEmpty(int fieldNumber, PotentialIssue pi)
  {
    if (isEmpty(fieldNumber))
    {
      registerIssue(pi);
    }
  }

  private boolean isEmpty(int fieldNumber)
  {
    return getValue(fieldNumber).equals("");
  }

  private String getValue(int fieldNumber)
  {
    String value = null;
    if (currentSegment.size() > fieldNumber)
    {
      value = currentSegment.get(fieldNumber);
    }
    if (value == null)
    {
      return "";
    }
    int valueLength = value.length();
    for (int i = 0; i < valueLength; i++)
    {
      char c = value.charAt(i);
      if (c == separators[TIL] || c == separators[CAR] || c == separators[AMP])
      {
        return value.substring(0, i);
      }
    }
    return value;
  }

  private String[] getValues(int fieldNumber)
  {
    String value = null;
    if (currentSegment.size() > fieldNumber)
    {
      value = currentSegment.get(fieldNumber);
    }
    if (value == null)
    {
      return new String[] { "" };
    }
    int valueLength = value.length();
    for (int i = 0; i < valueLength; i++)
    {
      char c = value.charAt(i);
      if (c == separators[TIL])
      {
        value = value.substring(0, i);
        break;
      }
    }
    return value.split("\\" + separators[CAR]);
  }

  private List<String[]> getRepeatValues(int fieldNumber)
  {
    List<String[]> values = new ArrayList<String[]>();
    String value = null;
    if (currentSegment.size() > fieldNumber)
    {
      value = currentSegment.get(fieldNumber);
    }
    if (value == null)
    {
      values.add(new String[] { "" });
      return values;
    }
    int valueLength = value.length();
    int startPos = 0;
    for (int i = 0; i < valueLength; i++)
    {
      char c = value.charAt(i);
      if (c == separators[TIL])
      {
        values.add(value.substring(startPos, i).split("\\" + separators[CAR]));
        startPos = i + 1;
      }
    }
    if (startPos < valueLength)
    {
      values.add(value.substring(startPos).split("\\" + separators[CAR]));
    }
    return values;
  }

  private void readSeparators(String messageText)
  {
    if (messageText.startsWith("MSH") && messageText.length() > 10)
    {
      separators[BAR] = messageText.charAt(BAR + 3);
      separators[CAR] = messageText.charAt(CAR + 3);
      separators[TIL] = messageText.charAt(TIL + 3);
      separators[SLA] = messageText.charAt(SLA + 3);
      separators[AMP] = messageText.charAt(AMP + 3);
      // Make sure separators are unique for each other
      for (int i = 0; i < separators.length; i++)
      {
        for (int j = i + 1; j < separators.length; j++)
        {
          if (separators[i] == separators[j])
          {
            registerError(pi.Hl7MshEncodingCharacterIsInvalid);
          }
        }
      }
      if (separators[BAR] != '|' || separators[CAR] != '^' || separators[TIL] != '~' || separators[SLA] != '\\' || separators[AMP] != '&')
      {
        registerIssue(pi.Hl7MshEncodingCharacterIsNonStandard);
      }
      // Bar should be used again after the 4 encoding characters
      if (separators[BAR] != messageText.charAt(8))
      {
        registerError(pi.Hl7MshEncodingCharacterIsInvalid);
      }
    } else
    {
      separators[BAR] = '|';
      separators[CAR] = '^';
      separators[TIL] = '~';
      separators[SLA] = '\\';
      separators[AMP] = '&';
      if (!messageText.startsWith("MSH"))
      {
        registerError(pi.Hl7MshSegmentIsMissing);
      } else if (messageText.length() < 10)
      {
        registerError(pi.Hl7MshEncodingCharacterIsMissing);
      } else
      {
        registerError(pi.GeneralParseException);
      }
    }

  }

  public String makeAckMessage(MessageReceived messageReceived)
  {
    String controlId = messageReceived.getMessageHeader().getMessageControl();
    String processingId = message.getMessageHeader().getProcessingStatusCode();
    String ackCode = ACK_ACCEPT;
    int countVaccNotSkipped = 0;
    for (Vaccination vaccination : messageReceived.getVaccinations())
    {
      if (!vaccination.isSkipped())
      {
        countVaccNotSkipped++;
      }
    }
    String text = "Message accepted";
    if (countVaccNotSkipped == 0)
    {
      text = "Message accepted with no vaccinations";
    } else if (countVaccNotSkipped == 1)
    {
      text = "Message accepted with 1 vaccination";
    } else
    {
      text = "Message accepted with " + countVaccNotSkipped + " vaccinations";
    }
    if (messageReceived.hasErrors())
    {
      text = "Message rejected: ";
      ackCode = ACK_ERROR;
      for (IssueFound issueFound : messageReceived.getIssuesFound())
      {
        if (issueFound.isError())
        {
          text += issueFound.getDisplayText();
          break;
        }
      }
    }
    if (messageReceived.getPatient().isSkipped())
    {
      text = "Message skipped: ";
      ackCode = ACK_ACCEPT;
      for (IssueFound issueFound : messageReceived.getIssuesFound())
      {
        if (issueFound.isSkip())
        {
          text += issueFound.getDisplayText();
          break;
        }
      }
    }
    if (text.length() > 80)
    {
      // HL7 has a max length for the ack text of 80 characters
      text = text.substring(0, 80);
    }
    StringBuilder ack = new StringBuilder();
    String receivingApplication = message.getMessageHeader().getSendingApplication();
    String receivingFacility = message.getMessageHeader().getSendingFacility();
    String sendingApplication = message.getMessageHeader().getReceivingApplication();
    String sendingFacility = message.getMessageHeader().getReceivingFacility();
    if (receivingApplication == null)
    {
      receivingApplication = "";
    }
    if (receivingFacility == null)
    {
      receivingFacility = "";
    }
    if (sendingApplication == null || sendingApplication.equals(""))
    {
      sendingApplication = "MCIR";
    }
    if (sendingFacility == null || sendingFacility.equals(""))
    {
      sendingFacility = "MCIR";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
    String messageDate = sdf.format(new Date());
    // MSH
    ack.append("MSH|^~\\&");
    ack.append("|" + sendingApplication); // MSH-3 Sending Application
    ack.append("|" + sendingFacility); // MSH-4 Sending Facility
    ack.append("|" + receivingApplication); // MSH-5 Receiving Application
    ack.append("|" + receivingFacility); // MSH-6 Receiving Facility
    ack.append("|" + messageDate); // MSH-7 Date/Time of Message
    ack.append("|"); // MSH-8 Security
    ack.append("|ACK^" + message.getMessageHeader().getMessageTrigger()); // MSH-9
    // Message
    // Type
    ack.append("|" + messageDate + "." + getNextAckCount()); // MSH-10 Message
                                                             // Control ID
    ack.append("|P"); // MSH-11 Processing ID
    ack.append("|2.5.1"); // MSH-12 Version ID
    ack.append("|\r");
    ack.append("SFT|" + SoftwareVersion.VENDOR + "|" + SoftwareVersion.VERSION + "|" + SoftwareVersion.PRODUCT + "|" + SoftwareVersion.BINARY_ID
        + "|\r");
    ack.append("MSA|" + ackCode + "|" + controlId + "|" + escapeHL7Chars(text) + "|\r");
    for (IssueFound issueFound : messageReceived.getIssuesFound())
    {
      if (issueFound.isError())
      {
        ack.append("ERR|||0|E||||" + issueFound.getDisplayText() + "|\r");
      }
    }
    for (IssueFound issueFound : messageReceived.getIssuesFound())
    {
      if (issueFound.isWarn())
      {
        ack.append("ERR|||0|W||||" + issueFound.getDisplayText() + "|\r");
      }
      if (issueFound.isSkip())
      {
        ack.append("ERR|||0|W||||Skipped: " + issueFound.getDisplayText() + "|\r");
      }
    }
    if (processingId.equals(PROCESSING_ID_DEBUG))
    {
      for (IssueFound issueFound : messageReceived.getIssuesFound())
      {
        if (issueFound.isAccept())
        {
          ack.append("ERR|||0|I||||" + issueFound.getDisplayText() + "|\r");
        }
      }
    }
    return ack.toString();

  }

  private int ackCount = 1;

  private int getNextAckCount()
  {
    if (ackCount == Integer.MAX_VALUE)
    {
      ackCount = 1;
    }
    return ackCount++;
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
