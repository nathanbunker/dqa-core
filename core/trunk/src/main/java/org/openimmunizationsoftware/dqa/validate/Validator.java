/*
 * Copyright 2013 by Dandelion Software & Research, Inc (DSR)
 * 
 * This application was written for immunization information system (IIS) community and has
 * been released by DSR under an Apache 2 License with the hope that this software will be used
 * to improve Public Health.  
 */
package org.openimmunizationsoftware.dqa.validate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.dqa.db.model.CodeReceived;
import org.openimmunizationsoftware.dqa.db.model.CodeStatus;
import org.openimmunizationsoftware.dqa.db.model.CodeTable;
import org.openimmunizationsoftware.dqa.db.model.KnownName;
import org.openimmunizationsoftware.dqa.db.model.MessageHeader;
import org.openimmunizationsoftware.dqa.db.model.MessageReceived;
import org.openimmunizationsoftware.dqa.db.model.PotentialIssue;
import org.openimmunizationsoftware.dqa.db.model.SubmitterProfile;
import org.openimmunizationsoftware.dqa.db.model.VaccineCpt;
import org.openimmunizationsoftware.dqa.db.model.VaccineCvx;
import org.openimmunizationsoftware.dqa.db.model.VaccineCvxGroup;
import org.openimmunizationsoftware.dqa.db.model.VaccineMvx;
import org.openimmunizationsoftware.dqa.db.model.VaccineProduct;
import org.openimmunizationsoftware.dqa.db.model.received.NextOfKin;
import org.openimmunizationsoftware.dqa.db.model.received.Observation;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;
import org.openimmunizationsoftware.dqa.db.model.received.VaccinationVIS;
import org.openimmunizationsoftware.dqa.db.model.received.types.Address;
import org.openimmunizationsoftware.dqa.db.model.received.types.CodedEntity;
import org.openimmunizationsoftware.dqa.db.model.received.types.Id;
import org.openimmunizationsoftware.dqa.db.model.received.types.Name;
import org.openimmunizationsoftware.dqa.db.model.received.types.PatientImmunity;
import org.openimmunizationsoftware.dqa.db.model.received.types.PhoneNumber;
import org.openimmunizationsoftware.dqa.manager.CodesReceived;
import org.openimmunizationsoftware.dqa.manager.KeyedSettingManager;
import org.openimmunizationsoftware.dqa.manager.KnownNames;
import org.openimmunizationsoftware.dqa.manager.PotentialIssues;
import org.openimmunizationsoftware.dqa.manager.VaccineGroupManager;
import org.openimmunizationsoftware.dqa.manager.VaccineProductManager;
import org.openimmunizationsoftware.dqa.quality.QualityCollector;

public class Validator extends ValidateMessage
{
  private static final String OBX_VACCINE_FUNDING = "64994-7";
  private static final String OBX_VACCINE_TYPE = "30956-7";
  private static final String OBX_VIS_PUBLISHED = "29768-9";
  private static final String OBX_VIS_PRESENTED = "29769-7";
  private static final String OBX_DISEASE_WITH_PRESUMED_IMMUNITY = "59784-9";

  private static final long ABOUT_ONE_YEAR = (long) (1000.0 * 60.0 * 60.0 * 24.0 * 365.25);
  private static final long ABOUT_ONE_MONTH = (long) (1000.0 * 60.0 * 60.0 * 24.0 * 31);

  private static String[] VALID_SUFFIX = { "SR", "JR", "II", "III", "IV" };

  private Session session = null;
  private Date now = new Date();
  private PotentialIssues.Field piAddress;
  private PotentialIssues.Field piAddressCity;
  private PotentialIssues.Field piAddressStreet;
  private PotentialIssues.Field piAddressCountry;
  private PotentialIssues.Field piAddressCounty;
  private PotentialIssues.Field piAddressState;
  private PotentialIssues.Field piAddressStreet2;
  private PotentialIssues.Field piAddressZip;
  private PotentialIssues.Field piAddressType;
  protected KeyedSettingManager ksm;

  private static List<SectionValidator> headerValidators = new ArrayList<SectionValidator>();
  private static List<SectionValidator> patientValidators = new ArrayList<SectionValidator>();
  private static List<SectionValidator> vaccinationValidators = new ArrayList<SectionValidator>();

  private static KnownNames knownNames = null;

  private static boolean initialized = false;

  public List<SectionValidator> getHeaderValidators()
  {
    return headerValidators;
  }

  public List<SectionValidator> getPatientValidators()
  {
    return patientValidators;
  }

  public List<SectionValidator> getVaccinationValidators()
  {
    return vaccinationValidators;
  }

  private static void init()
  {
    if (!initialized)
    {
      // header validations
      headerValidators.add(new FacilityValidator());

      // patient validations
      patientValidators.add(new PatientClassValidator());
      patientValidators.add(new RegistryStatusValidator());

      // vaccination validations
      vaccinationValidators.add(new OrderControlValidator());
      vaccinationValidators.add(new OrderNumberValidator());
      vaccinationValidators.add(new FacilityValidator());
      vaccinationValidators.add(new GivenBy());
      initialized = true;

      knownNames = KnownNames.getKnownsNames();
    }
  }

  public Validator(SubmitterProfile profile, Session session) {
    super(profile);
    this.session = session;
    ksm = KeyedSettingManager.getKeyedSettingManager();
    init();
  }

  public void validateVaccinationUpdateMessage(MessageReceived message)
  {
    validateVaccinationUpdateMessage(message, null);
  }

  public void validateVaccinationUpdateMessage(MessageReceived message, QualityCollector qualityCollector)
  {
    this.message = message;
    this.patient = message.getPatient();
    this.qualityCollector = qualityCollector;
    positionId = 1;
    skippableItem = patient;
    this.issuesFound = message.getIssuesFound();
    validateHeader();
    validatePatient();
    for (NextOfKin nextOfKin : message.getNextOfKins())
    {
      if (!nextOfKin.isSkipped())
      {
        positionId = nextOfKin.getPositionId();
        skippableItem = nextOfKin;
        validateNextOfKin(nextOfKin);
      }
    }
    if (patient.getResponsibleParty() == null)
    {
      registerIssue(pi.PatientGuardianResponsiblePartyIsMissing);
    }
    for (Vaccination vaccination : message.getVaccinations())
    {
      if (!vaccination.isSkipped())
      {
        positionId = vaccination.getPositionId();
        skippableItem = vaccination;
        validateVaccination(vaccination);
      }
    }
  }

  private void validateNextOfKin(NextOfKin nextOfKin)
  {
    piAddress = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS;
    piAddressCity = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_CITY;
    piAddressStreet = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_STREET;
    piAddressCountry = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_COUNTRY;
    piAddressCounty = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_COUNTY;
    piAddressState = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_STATE;
    piAddressStreet2 = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_STREET2;
    piAddressZip = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_ZIP;
    piAddressType = PotentialIssues.Field.NEXT_OF_KIN_ADDRESS_TYPE;
    if (validateAddress(nextOfKin.getAddress()))
    {
      Address p = patient.getAddress();
      Address n = nextOfKin.getAddress();
      if (!p.getCity().equals(n.getCity()) || !p.getState().equals(n.getState()) || !p.getStreet().equals(n.getStreet())
          || !p.getStreet2().equals(p.getStreet2()))
      {
        registerIssue(pi.NextOfKinAddressIsDifferentFromPatientAddress);
      }
    }
    boolean patientUnderAged = patient.isUnderAged();
    boolean isResponsibleParty = false;
    handleCodeReceived(nextOfKin.getRelationship(), PotentialIssues.Field.NEXT_OF_KIN_RELATIONSHIP);
    if (patientUnderAged && !nextOfKin.getRelationship().isEmpty())
    {

      String relationshipCode = nextOfKin.getRelationshipCode();
      // Check for unexpected relationship
      if (NextOfKin.RELATIONSHIP_CHILD.equals(relationshipCode) || NextOfKin.RELATIONSHIP_FOSTER_CHILD.equals(relationshipCode)
          || NextOfKin.RELATIONSHIP_STEPCHILD.equals(relationshipCode))
      {
        // Normally don't expect under age patient to have a child recorded on
        // record. Often this issue happens when the relationship is being
        // recorded backwards, from the patient to the NK1, instead of from
        // the NK1 to the patient.
        registerIssue(pi.NextOfKinRelationshipIsUnexpected);
      } else
      {
        String[] responsibleParties = { NextOfKin.RELATIONSHIP_CARE_GIVER, NextOfKin.RELATIONSHIP_FATHER, NextOfKin.RELATIONSHIP_GRANDPARENT,
            NextOfKin.RELATIONSHIP_MOTHER, NextOfKin.RELATIONSHIP_PARENT, NextOfKin.RELATIONSHIP_GUARDIAN };
        for (String compare : responsibleParties)
        {
          if (compare.equals(relationshipCode))
          {
            isResponsibleParty = true;
            break;
          }
        }
      }
    }
    if (patientUnderAged && !isResponsibleParty)
    {
      registerIssue(pi.NextOfKinRelationshipIsNotResponsibleParty);
    }

    boolean notEmptyFirst = notEmpty(nextOfKin.getNameLast(), pi.NextOfKinNameLastIsMissing);
    boolean notEmptyLast = notEmpty(nextOfKin.getNameLast(), pi.NextOfKinNameFirstIsMissing);
    if (!notEmptyFirst && !notEmptyLast)
    {
      registerIssue(pi.NextOfKinNameIsMissing);
    }
    if (notEmptyFirst && notEmptyLast && patientUnderAged && isResponsibleParty)
    {
      if (areEqual(nextOfKin.getNameLast(), patient.getNameLast()) && areEqual(nextOfKin.getNameFirst(), patient.getNameFirst())
          && areEqual(nextOfKin.getNameMiddle(), patient.getNameMiddle()) && areEqual(nextOfKin.getNameSuffix(), patient.getNameMiddle())
          && areEqual(nextOfKin.getNameSuffix(), patient.getNameSuffix()))
      {
        registerIssue(pi.PatientGuardianNameIsSameAsUnderagePatient);
      }
    }
    validatePhone(nextOfKin.getPhone(), PotentialIssues.Field.NEXT_OF_KIN_PHONE_NUMBER);
    if ((isResponsibleParty && patientUnderAged && (notEmptyFirst || notEmptyLast)))
    {
      if (patient.getResponsibleParty() == null)
      {
        // Patient does not yet have an assigned responsible party, so assigning
        // one now
        patient.setResponsibleParty(nextOfKin);
        notEmpty(nextOfKin.getAddressCity(), PotentialIssues.Field.PATIENT_GUARDIAN_ADDRESS_CITY);
        notEmpty(nextOfKin.getAddressStateCode(), PotentialIssues.Field.PATIENT_GUARDIAN_ADDRESS_STATE);
        notEmpty(nextOfKin.getAddressCity(), PotentialIssues.Field.PATIENT_GUARDIAN_ADDRESS_CITY);
        notEmpty(nextOfKin.getAddressZip(), PotentialIssues.Field.PATIENT_GUARDIAN_ADDRESS_ZIP);

        boolean firstNotEmpty = notEmpty(nextOfKin.getNameFirst(), pi.PatientGuardianNameFirstIsMissing);
        boolean lastNotEmpty = notEmpty(nextOfKin.getNameLast(), pi.PatientGuardianNameLastIsMissing);
        if (!firstNotEmpty && !lastNotEmpty)
        {
          registerIssue(pi.PatientGuardianNameIsMissing);
        }
        String pFirst = patient.getNameFirst();
        String pLast = patient.getNameLast();
        String nFirst = nextOfKin.getNameFirst();
        String nLast = nextOfKin.getNameLast();
        if (pFirst != null && !pFirst.equals("") && pLast != null && !pLast.equals(""))
        {
          if (pFirst.equals(nFirst) && pLast.equals(nLast))
          {
            registerIssue(pi.PatientGuardianNameIsSameAsUnderagePatient);
          }
        }
        notEmpty(nextOfKin.getPhoneNumber(), pi.PatientGuardianPhoneIsMissing);
        notEmpty(nextOfKin.getRelationshipCode(), pi.PatientGuardianRelationshipIsMissing);
      }
    }
    // TODO NextOfKinSsnIsMissing
  }

  private void validateVaccination(Vaccination vaccination)
  {
    handleCodeReceived(vaccination.getAction(), PotentialIssues.Field.VACCINATION_ACTION_CODE);
    if (vaccination.isActionAdd())
    {
      registerIssue(pi.VaccinationActionCodeIsValuedAsAdd);
      registerIssue(pi.VaccinationActionCodeIsValuedAsAddOrUpdate);
    } else if (vaccination.isActionUpdate())
    {
      registerIssue(pi.VaccinationActionCodeIsValuedAsUpdate);
      registerIssue(pi.VaccinationActionCodeIsValuedAsAddOrUpdate);
    } else if (vaccination.isActionDelete())
    {
      registerIssue(pi.VaccinationActionCodeIsValuedAsDelete);
    }

    // If vaccination is not actually administered then this is a waiver. Need
    // to check that now, here to see if we need to enforce a value in RXA-9 to
    // indicate that the vaccination is historical or administered.
    // By default we assume that the vaccination was completed.
    handleCodeReceived(vaccination.getCompletion(), PotentialIssues.Field.VACCINATION_COMPLETION_STATUS);
    if (vaccination.isCompletionCompleted())
    {
      registerIssue(pi.VaccinationCompletionStatusIsValuedAsCompleted);
    } else if (vaccination.isCompletionRefused())
    {
      registerIssue(pi.VaccinationCompletionStatusIsValuedAsRefused);
    } else if (vaccination.isCompletionNotAdministered())
    {
      registerIssue(pi.VaccinationCompletionStatusIsValuedAsNotAdministered);
    } else if (vaccination.isCompletionPartiallyAdministered())
    {
      registerIssue(pi.VaccinationCompletionStatusIsValuedAsPartiallyAdministered);
    }

    boolean vaccinationAdministeredOrHistorical = false;
    if ((vaccination.getCompletion().isEmpty() || vaccination.isCompletionCompletedOrPartiallyAdministered())
        && (vaccination.getAdminCvxCode() != null && !vaccination.getAdminCvxCode().equals("998")))
    {
      vaccinationAdministeredOrHistorical = true;
    }

    if (vaccinationAdministeredOrHistorical)
    {
      if (notEmpty(vaccination.getInformationSourceCode(), pi.VaccinationInformationSourceIsMissing))
      {
        handleCodeReceived(vaccination.getInformationSource(), PotentialIssues.Field.VACCINATION_INFORMATION_SOURCE);
        vaccination.setAdministered(Vaccination.INFO_SOURCE_ADMIN.equals(vaccination.getInformationSourceCode()));
        if (vaccination.isAdministered())
        {
          registerIssue(pi.VaccinationInformationSourceIsValuedAsAdministered);
        } else if (Vaccination.INFO_SOURCE_HIST.equals(vaccination.getInformationSourceCode()))
        {
          registerIssue(pi.VaccinationInformationSourceIsValuedAsHistorical);
        }
      }
    }

    handleCodeReceived(vaccination.getAdminCpt(), PotentialIssues.Field.VACCINATION_CPT_CODE);
    String cptCode = vaccination.getAdminCptCode();
    vaccination.setAdminCptCode(cptCode);

    handleCodeReceived(vaccination.getAdminCvx(), PotentialIssues.Field.VACCINATION_CVX_CODE);
    String cvxCode = vaccination.getAdminCvxCode();

    CodeReceived vaccineCr = vaccination.getAdminCvx().getCodeReceived();

    // TODO Need to figure out status for CPT can CVX
    // CVX \ CPT | Deprecated | Ignored | Invalid | Missing | Not Specific |
    // Unrecognized
    // +--------------+------------+---------+---------+---------+--------------+-------------
    // | Deprecated | CVX |
    // | Ignored | CVX |
    // | Invalid | CPT |
    // | Missing | CPT |
    // | Not Specific | CVX |
    // | Unrecognized | CPT |
    // |
    // |
    VaccineCvx vaccineCvx = null;
    VaccineCpt vaccineCpt = null;
    if (cptCode != null && !cptCode.equals(""))
    {
      Query query = session.createQuery("from VaccineCpt where cptCode = ? and validStartDate <= ? and validEndDate > ?");
      query.setString(0, cptCode);
      query.setDate(1, vaccination.getAdminDate());
      query.setDate(2, vaccination.getAdminDate());
      List<VaccineCpt> vaccineCpts = query.list();
      if (vaccineCpts.size() > 0)
      {
        vaccineCpt = vaccineCpts.get(0);
      }
      if (vaccineCpt != null && vaccination.getAdminDate() != null)
      {
        if (vaccineCpt.getValidStartDate().after(vaccination.getAdminDate()) || trunc(vaccination.getAdminDate()).after(vaccineCpt.getValidEndDate()))
        {
          registerIssue(pi.VaccinationCptCodeIsInvalidForDateAdministered, vaccination.getAdminCpt().getCodeReceived());
        } else if (vaccineCpt.getUseStartDate().after(vaccination.getAdminDate())
            || trunc(vaccination.getAdminDate()).after(vaccineCpt.getUseEndDate()))
        {
          registerIssue(pi.VaccinationCptCodeIsUnexpectedForDateAdministered, vaccination.getAdminCpt().getCodeReceived());
        }
      }
    }
    if (cvxCode != null && !cvxCode.equals(""))
    {
      try
      {
        int cvxId = Integer.parseInt(cvxCode);
        vaccineCvx = (VaccineCvx) session.get(VaccineCvx.class, cvxId);
      } catch (NumberFormatException nfe)
      {
        // ignore
      }
      if (vaccineCvx != null && vaccination.getAdminDate() != null)
      {
        if (vaccineCvx.getValidStartDate().after(vaccination.getAdminDate()) || trunc(vaccination.getAdminDate()).after(vaccineCvx.getValidEndDate()))
        {
          if (vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_FOREIGN_VACCINE)
              || vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_UNSPECIFIED))
          {
            if (vaccination.isAdministered())
            {
              registerIssue(pi.VaccinationCvxCodeIsInvalidForDateAdministered, vaccineCr);
            }
          } else
          {
            registerIssue(pi.VaccinationCvxCodeIsInvalidForDateAdministered, vaccineCr);
          }
        } else if (vaccineCvx.getUseStartDate().after(vaccination.getAdminDate())
            || trunc(vaccination.getAdminDate()).after(vaccineCvx.getUseEndDate()))
        {
          if (vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_FOREIGN_VACCINE)
              || vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_UNSPECIFIED))
          {
            if (vaccination.isAdministered())
            {
              registerIssue(pi.VaccinationCvxCodeIsUnexpectedForDateAdministered, vaccineCr);
            }
          } else
          {
            registerIssue(pi.VaccinationCvxCodeIsUnexpectedForDateAdministered, vaccineCr);
          }
        }
      }
    }
    boolean useCptInsteadOfCvx = (vaccineCvx == null || vaccination.getAdminCvx().isInvalid() || vaccination.getAdminCvx().isIgnored())
        && vaccineCpt != null;

    if (useCptInsteadOfCvx)
    {
      vaccineCvx = vaccineCpt.getCvx();
      vaccineCr = vaccination.getAdminCpt().getCodeReceived();
    }
    CodedEntity ce = useCptInsteadOfCvx ? vaccination.getAdminCpt() : vaccination.getAdminCvx();
    if (ce == null)
    {
      registerIssue(pi.VaccinationAdminCodeIsMissing);
    } else
    {
      if (ce.isEmpty())
      {
        registerIssue(pi.VaccinationAdminCodeIsMissing);
      }
      if (ce.getCodeReceived() != null)
      {
        if (ce.getCodeReceived().getCodeStatus().isDeprecated())
        {
          registerIssue(pi.VaccinationAdminCodeIsDeprecated);
        }
        if (ce.getCodeReceived().getCodeStatus().isIgnored())
        {
          registerIssue(pi.VaccinationAdminCodeIsIgnored);
        }
        if (ce.getCodeReceived().getCodeStatus().isInvalid())
        {
          registerIssue(pi.VaccinationAdminCodeIsInvalid);
        }
        if (ce.getCodeReceived().getCodeStatus().isUnrecognized())
        {
          registerIssue(pi.VaccinationAdminCodeIsUnrecognized);
        }
      }
    }

    vaccination.setVaccineCvx(vaccineCvx);

    if (notEmpty(vaccination.getAdminDate(), pi.VaccinationAdminDateIsMissing))
    {
      Calendar cal = Calendar.getInstance();
      int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
      int lastDayofMonth = cal.getMaximum(Calendar.DAY_OF_MONTH);
      if (dayOfMonth == 1)
      {
        registerIssue(pi.VaccinationAdminDateIsOnFirstDayOfMonth);
      } else if (dayOfMonth == 15)
      {
        registerIssue(pi.VaccinationAdminDateIsOn15ThDayOfMonth);
      } else if (dayOfMonth == lastDayofMonth)
      {
        registerIssue(pi.VaccinationAdminDateIsOnLastDayOfMonth);
      }
    }
    if (vaccineCvx != null)
    {
      if (vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_UNSPECIFIED))
      {
        if (vaccination.isAdministered())
        {
          registerIssue(pi.VaccinationAdminCodeIsNotSpecific, vaccineCr);
        }
      } else if (vaccineCvx.getCvxCode().equals(VaccineCvx.NO_VACCINE_ADMINISTERED))
      {
        registerIssue(pi.VaccinationAdminCodeIsValuedAsNotAdministered, vaccineCr);
      } else if (vaccineCvx.getCvxCode().equals(VaccineCvx.UNKNOWN))
      {
        registerIssue(pi.VaccinationAdminCodeIsValuedAsUnknown, vaccineCr);
      } else if (vaccineCvx.getCvxCode().equals(VaccineCvx.CONCEPT_TYPE_NON_VACCINE))
      {
        registerIssue(pi.VaccinationAdminCodeIsNotVaccine, vaccineCr);
      }
      if (vaccination.getAdminDate() != null)
      {
        if (vaccineCvx.getValidStartDate().after(vaccination.getAdminDate()) || trunc(vaccination.getAdminDate()).after(vaccineCvx.getValidEndDate()))
        {
          if (vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_FOREIGN_VACCINE)
              || vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_UNSPECIFIED))
          {
            if (vaccination.isAdministered())
            {
              registerIssue(pi.VaccinationAdminDateIsBeforeOrAfterLicensedVaccineRange, vaccineCr);
              registerIssue(pi.VaccinationAdminCodeIsInvalidForDateAdministered, vaccineCr);
            }
          } else
          {
            registerIssue(pi.VaccinationAdminDateIsBeforeOrAfterLicensedVaccineRange, vaccineCr);
            registerIssue(pi.VaccinationAdminCodeIsInvalidForDateAdministered, vaccineCr);
          }
        } else if (vaccineCvx.getUseStartDate().after(vaccination.getAdminDate())
            || trunc(vaccination.getAdminDate()).after(vaccineCvx.getUseEndDate()))
        {

          if (vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_FOREIGN_VACCINE)
              || vaccineCvx.getConceptType().equals(VaccineCvx.CONCEPT_TYPE_UNSPECIFIED))
          {
            if (vaccination.isAdministered())
            {
              registerIssue(pi.VaccinationAdminDateIsBeforeOrAfterExpectedVaccineUsageRange, vaccineCr);
              registerIssue(pi.VaccinationAdminCodeIsUnexpectedForDateAdministered, vaccineCr);
            }
          } else
          {
            registerIssue(pi.VaccinationAdminDateIsBeforeOrAfterExpectedVaccineUsageRange, vaccineCr);
            registerIssue(pi.VaccinationAdminCodeIsUnexpectedForDateAdministered, vaccineCr);
          }
        }
        if (patient.getBirthDate() != null)
        {
          int monthsBetween = monthsBetween(patient.getBirthDate(), vaccination.getAdminDate());
          if (monthsBetween < vaccineCvx.getUseMonthStart() || monthsBetween > vaccineCvx.getUseMonthEnd())
          {
            registerIssue(pi.VaccinationAdminDateIsBeforeOrAfterWhenExpectedForPatientAge, vaccineCr);
          }
        }
      }
    }
    handleCodeReceived(vaccination.getManufacturer(), PotentialIssues.Field.VACCINATION_MANUFACTURER_CODE, vaccination.isAdministered());
    VaccineMvx vaccineMvx = null;
    if (vaccination.getManufacturerCode() != null && !vaccination.getManufacturerCode().equals(""))
    {
      vaccineMvx = (VaccineMvx) session.get(VaccineMvx.class, vaccination.getManufacturerCode());
    }
    if (vaccination.isAdministered())
    {
      if (vaccineMvx != null && !vaccineMvx.getMvxCode().equals("") && vaccination.getAdminDate() != null)
      {
        documentParagraph("Verifying that manufacturer is being used correctly considering the administration date.");
        if (vaccineMvx.getValidStartDate().after(vaccination.getAdminDate()) || vaccination.getAdminDate().after(vaccineMvx.getValidEndDate()))
        {
          registerIssue(pi.VaccinationManufacturerCodeIsInvalidForDateAdministered, vaccination.getManufacturer().getCodeReceived());
          if (isDocument())
          {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            documentParagraph("Manufacture code " + vaccination.getManufacturerCode() + " is not valid before "
                + sdf.format(vaccineMvx.getValidStartDate()) + " or after " + sdf.format(vaccineMvx.getValidEndDate())
                + " for the administration date " + sdf.format(vaccination.getAdminDate()) + " ");
          }
        } else if (vaccineMvx.getUseStartDate().after(vaccination.getAdminDate()) || vaccination.getAdminDate().after(vaccineMvx.getUseEndDate()))
        {
          registerIssue(pi.VaccinationManufacturerCodeIsUnexpectedForDateAdministered, vaccination.getManufacturer().getCodeReceived());
          if (isDocument())
          {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            documentParagraph("Manufacture code " + vaccination.getManufacturerCode() + " is not expected before "
                + sdf.format(vaccineMvx.getUseStartDate()) + " or after " + sdf.format(vaccineMvx.getUseEndDate()) + " for the administration date "
                + sdf.format(vaccination.getAdminDate()) + " ");
          }
        }
      }
      if (vaccineMvx != null && !vaccineMvx.getMvxCode().equals("") && vaccineCvx != null && !vaccineCvx.getCvxCode().equals("")
          && !vaccineCvx.getCvxCode().equals("998") && !vaccineCvx.getCvxCode().equals("999")
          && (vaccination.getManufacturer().isValid() || vaccination.getManufacturer().isDeprecated()))
      {
        vaccination.getProduct().setCode(vaccineCvx.getCvxCode() + "-" + vaccineMvx.getMvxCode());
        handleCodeReceived(vaccination.getProduct(), PotentialIssues.Field.VACCINATION_PRODUCT);
        List<VaccineProduct> vaccineProductList = VaccineProductManager.getVaccineProductManager().getVaccineProducts(vaccineCvx, vaccineMvx);
        VaccineProduct valVp = null;
        if (vaccineProductList != null)
        {
          VaccineProduct useVp = null;
          if (vaccination.getAdminDate() != null)
          {
            documentParagraph("Vaccination product is recognized and the vaccination date is given. Verifying that vaccination product is valid for the date administered. ");
            for (VaccineProduct vp : vaccineProductList)
            {
              if (!vp.getValidStartDate().after(vaccination.getAdminDate()) && !vaccination.getAdminDate().after(vp.getValidEndDate()))
              {
                valVp = vp;
                if (!vp.getUseStartDate().after(vaccination.getAdminDate()) && !vaccination.getAdminDate().after(vp.getUseEndDate()))
                {
                  useVp = vp;
                  break;
                }
              } else
              {
                if (isDocument())
                {
                  SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                  documentParagraph("Vaccination product administered " + sdf.format(vaccination.getAdminDate()) + " before "
                      + sdf.format(vp.getValidStartDate()) + " or after " + sdf.format(vp.getValidEndDate()) + " valid range.");
                }
              }
            }
            if (valVp != null)
            {
              // valid product
              if (useVp == null)
              {
                // shouldn't be used at this point
                registerIssue(pi.VaccinationProductIsUnexpectedForDateAdministered);
              }
            } else
            {
              registerIssue(pi.VaccinationProductIsInvalidForDateAdministered);
            }
          }
        }
      } else
      {
        registerIssue(pi.VaccinationProductIsMissing);
      }
    }

    // TODO VaccinationAdminCodeMayBeVariationOfPreviouslyReportedCodes

    if (vaccination.getAdminDate() != null)
    {
      if (vaccination.isAdministered())
      {
        if (vaccination.getExpirationDate() != null)
        {
          if (trunc(vaccination.getAdminDate()).after(vaccination.getExpirationDate()))
          {
            registerIssue(pi.VaccinationAdminDateIsAfterLotExpirationDate);
          }
        }
      }
      if (trunc(vaccination.getAdminDate()).after(message.getReceivedDate()))
      {
        registerIssue(pi.VaccinationAdminDateIsAfterMessageSubmitted);
      }
      if (patient.getDeathDate() != null)
      {
        if (trunc(vaccination.getAdminDate()).after(patient.getDeathDate()))
        {
          registerIssue(pi.VaccinationAdminDateIsAfterPatientDeathDate);
        }
      }
      if (patient.getBirthDate() != null)
      {
        if (vaccination.getAdminDate().before(trunc(patient.getBirthDate())))
        {
          registerIssue(pi.VaccinationAdminDateIsBeforeBirth);
        }
      }
      if (vaccination.getSystemEntryDate() != null)
      {

        if (trunc(vaccination.getAdminDate()).after(vaccination.getSystemEntryDate()))
        {
          registerIssue(pi.VaccinationAdminDateIsAfterSystemEntryDate);
        }
      }
      if (notEmpty(vaccination.getAdminDateEnd(), pi.VaccinationAdminDateEndIsMissing))
      {
        if (!vaccination.getAdminDateEnd().equals(vaccination.getAdminDate()))
        {
          registerIssue(pi.VaccinationAdminDateEndIsDifferentFromStartDate);
        }
      }

    }
    // TODO
    // registerIssue(pi.VaccinationAdminDateIsBeforeExpectedReportingWindow);

    boolean amountValued = false;
    if (vaccination.getAmount() == null || vaccination.getAmount().equals("") || vaccination.getAmount().equals("999"))
    {
      if (vaccination.isAdministered())
      {
        registerIssue(pi.VaccinationAdministeredAmountIsMissing);
        registerIssue(pi.VaccinationAdministeredAmountIsValuedAsUnknown);
      }
      vaccination.setAmount("");
    } else
    {
      try
      {
        float amount = Float.parseFloat(vaccination.getAmount());
        if (amount == 0)
        {
          if (vaccination.isAdministered())
          {
            registerIssue(pi.VaccinationAdministeredAmountIsValuedAsZero);
          }
        } else
        {
          amountValued = true;
        }
      } catch (NumberFormatException nfe)
      {
        if (vaccination.isAdministered())
        {
          registerIssue(pi.VaccinationAdministeredAmountIsInvalid);
        }
        vaccination.setAmount("");
      }
    }
    handleCodeReceived(vaccination.getAmountUnit(), PotentialIssues.Field.VACCINATION_ADMINISTERED_UNIT, vaccination.isAdministered() && amountValued);
    handleCodeReceived(vaccination.getBodyRoute(), PotentialIssues.Field.VACCINATION_BODY_ROUTE, vaccination.isAdministered());
    handleCodeReceived(vaccination.getBodySite(), PotentialIssues.Field.VACCINATION_BODY_SITE, vaccination.isAdministered());

    // TODO VaccinationBodyRouteIsInvalidForVaccineIndicated
    // TODO VaccinationBodySiteIsInvalidForVaccineIndicated

    handleCodeReceived(vaccination.getConfidentiality(), PotentialIssues.Field.VACCINATION_CONFIDENTIALITY_CODE);

    if (vaccination.getConfidentialityCode().equals("R") || vaccination.getConfidentialityCode().equals("V"))
    {
      registerIssue(pi.VaccinationConfidentialityCodeIsValuedAsRestricted);
    }

    if (vaccineCpt != null && vaccineCpt.getCvx() != null && vaccineCvx != null)
    {
      if (!checkGroupMatch(vaccineCvx, vaccineCpt))
      {
        registerIssue(pi.VaccinationCvxCodeAndCptCodeAreInconsistent);
      }
    }

    for (SectionValidator sectionValidator : vaccinationValidators)
    {
      sectionValidator.validateVaccination(vaccination, this);
    }

    handleCodeReceived(vaccination.getOrderedBy(), PotentialIssues.Field.VACCINATION_ORDERED_BY, vaccination.isAdministered());
    handleCodeReceived(vaccination.getEnteredBy(), PotentialIssues.Field.VACCINATION_RECORDED_BY);

    // TODO VaccinationIdOfReceiverIsMissing
    // TODO VaccinationIdOfReceiverIsUnrecognized
    // TODO VaccinationIdOfSenderIsMissing
    // TODO VaccinationIdOfSenderIsUnrecognized

    if (vaccination.isAdministered())
    {
      notEmpty(vaccination.getFacilityName(), pi.VaccinationFacilityNameIsMissing);
      notEmpty(vaccination.getExpirationDate(), pi.VaccinationLotExpirationDateIsMissing);
      if (notEmpty(vaccination.getLotNumber(), pi.VaccinationLotNumberIsMissing))
      {
        String lotNumber = vaccination.getLotNumber();
        if (lotNumber.startsWith("LOT") || lotNumber.length() <= 4)
        {
          registerIssue(pi.VaccinationLotNumberIsInvalid);
        }
      }
    }
    if (vaccination.isCompletionCompleted() && !vaccination.getRefusalCode().equals(""))
    {
      registerIssue(pi.VaccinationRefusalReasonConflictsCompletionStatus);
    } else if (vaccination.isCompletionRefused() && vaccination.getRefusalCode().equals(""))
    {
      registerIssue(pi.VaccinationRefusalReasonIsMissing);
    }
    handleCodeReceived(vaccination.getRefusal(), PotentialIssues.Field.VACCINATION_REFUSAL_REASON, vaccination.isCompletionRefused());

    if (notEmpty(vaccination.getSystemEntryDate(), pi.VaccinationSystemEntryTimeIsMissing) && message.getReceivedDate() != null)
    {
      if (message.getReceivedDate().before(trunc(vaccination.getSystemEntryDate())))
      {
        registerIssue(pi.VaccinationSystemEntryTimeIsInFuture);
      }
    }
    String financialEligibilityCode = null;
    Map<String, VaccinationVIS> vaccinationVISMap = new HashMap<String, VaccinationVIS>();
    for (Observation observation : vaccination.getObservations())
    {
      skippableItem = observation;
      handleCodeReceived(observation.getValueType(), PotentialIssues.Field.OBSERVATION_VALUE_TYPE);
      handleCodeReceived(observation.getObservationIdentifier(), PotentialIssues.Field.OBSERVATION_OBSERVATION_IDENTIFIER_CODE);
      if (!observation.isSkipped())
      {
        if (financialEligibilityCode == null && observation.getObservationIdentifierCode().equals(OBX_VACCINE_FUNDING))
        {
          if (notEmpty(observation.getObservationValue(), pi.ObservationObservationValueIsMissing))
          {
            financialEligibilityCode = observation.getObservationValue();
          }
        } else if (observation.getObservationIdentifierCode().equals(OBX_VACCINE_TYPE)
            || observation.getObservationIdentifierCode().equals(OBX_VIS_PRESENTED)
            || observation.getObservationIdentifierCode().equals(OBX_VIS_PUBLISHED))
        {
          String key = observation.getObservationSubId();
          VaccinationVIS vaccinationVIS = vaccinationVISMap.get(key);
          if (vaccinationVIS == null)
          {
            vaccinationVIS = new VaccinationVIS();
            vaccinationVIS.setVaccination(vaccination);
            vaccinationVISMap.put(key, vaccinationVIS);
            vaccination.getVaccinationVisList().add(vaccinationVIS);
          }
          if (observation.getObservationIdentifierCode().equals(OBX_VACCINE_TYPE))
          {
            vaccinationVIS.setCvxCode(observation.getObservationValue());
          } else if (observation.getObservationIdentifierCode().equals(OBX_VIS_PRESENTED))
          {
            if (!observation.getObservationValue().equals(""))
            {
              vaccinationVIS.setPresentedDate(createDate(pi.VaccinationVisPresentedDateIsInvalid, observation.getObservationValue()));
            }
          } else if (observation.getObservationIdentifierCode().equals(OBX_VIS_PUBLISHED))
          {

            if (!observation.getObservationValue().equals(""))
            {
              vaccinationVIS.setPublishedDate(createDate(pi.VaccinationVisPublishedDateIsInvalid, observation.getObservationValue()));
            }
          }
        } else if (observation.getObservationIdentifierCode().equals(OBX_DISEASE_WITH_PRESUMED_IMMUNITY))
        {

          PatientImmunity patientImmunity = new PatientImmunity();
          patientImmunity.setPatient(patient);
          patientImmunity.setImmunityCode(observation.getObservationValue());
          patient.getPatientImmunityList().add(patientImmunity);
          handleCodeReceived(patientImmunity.getImmunity(), PotentialIssues.Field.PATIENT_IMMUNITY_CODE);
        }
      }
    }
    skippableItem = vaccination;
    if (vaccinationVISMap.size() == 0)
    {
      if (vaccination.isAdministered())
      {
        registerIssue(pi.VaccinationVisIsMissing);
      }
    } else
    {
      boolean firstOne = true;
      int positionId = 0;
      for (VaccinationVIS vaccinationVIS : vaccination.getVaccinationVisList())
      {
        positionId++;
        vaccinationVIS.setPositionId(positionId);
        // handleCodeReceived(vaccinationVIS.getDocument(),
        // PotentialIssues.Field.VACCINATION_VIS_DOCUMENT_TYPE,
        // vaccination.isAdministered());
        handleCodeReceived(vaccinationVIS.getCvx(), PotentialIssues.Field.VACCINATION_VIS_CVX_CODE, vaccination.isAdministered());
        if (vaccinationVIS.getPublishedDate() == null)
        {
          if (vaccination.isAdministered())
          {
            registerIssue(pi.VaccinationVisPublishedDateIsMissing);
          }
        }
        if (vaccinationVIS.getPresentedDate() == null)
        {
          if (vaccination.isAdministered())
          {
            registerIssue(pi.VaccinationVisPresentedDateIsMissing);
          }
        } else
        {
          if (vaccination.getAdminDate() != null)
          {
            if (vaccination.getAdminDate().after(vaccinationVIS.getPresentedDate()))
            {
              registerIssue(pi.VaccinationVisPresentedDateIsAfterAdminDate);
            } else if (vaccination.getAdminDate().before(vaccinationVIS.getPresentedDate()))
            {
              registerIssue(pi.VaccinationVisPresentedDateIsNotAdminDate);
            }
          }
          if (vaccinationVIS.getPublishedDate() != null)
          {
            if (vaccinationVIS.getPresentedDate().before(vaccinationVIS.getPublishedDate()))
            {
              registerIssue(pi.VaccinationVisPresentedDateIsBeforePublishedDate);
            }
          }
        }
        if (vaccinationVIS.getDocumentCode().equals(""))
        {
          if (vaccinationVIS.getCvxCode().equals("") || vaccinationVIS.getPublishedDate() == null)
          {
            // TODO verify that code and date are recognized, this logic will
            // work for now
            registerIssue(pi.VaccinationVisIsUnrecognized);
            if (firstOne && vaccination.isAdministered())
            {
              registerIssue(pi.VaccinationVisIsMissing);
            }
          }
        }
        firstOne = false;
      }
    }

    if (financialEligibilityCode == null)
    {
      if (vaccination.isAdministered())
      {
        registerIssue(pi.VaccinationFinancialEligibilityCodeIsMissing);
      }
    } else
    {
      vaccination.setFinancialEligibilityCode(financialEligibilityCode);
      handleCodeReceived(vaccination.getFinancialEligibility(), PotentialIssues.Field.VACCINATION_FINANCIAL_ELIGIBILITY_CODE,
          vaccination.isAdministered());
    }

    {
      // Created rough scoring system that gives a point to other attributes
      // that suggest a vaccination
      // was administered or not. The idea is that if the sender knows a lot
      // about the vaccination and it
      // was given recently then it is probably administered. Otherwise it must
      // be historical.
      int administeredScore = 0;
      if (vaccination.getAdminDate() != null)
      {
        long elapsed = vaccination.getAdminDate().getTime() - message.getReceivedDate().getTime();
        if (elapsed < ABOUT_ONE_MONTH)
        {
          administeredScore += 5;
        }
      }
      if (!vaccination.getLotNumber().equals(""))
      {
        administeredScore += 2;
      }
      if (vaccination.getExpirationDate() != null)
      {
        administeredScore += 2;
      }
      if (!vaccination.getManufacturerCode().equals(""))
      {
        administeredScore += 2;
      }
      if (!vaccination.getFinancialEligibilityCode().equals(""))
      {
        administeredScore += 2;
      }
      if (!vaccination.getBodyRouteCode().equals(""))
      {
        administeredScore += 1;
      }
      if (!vaccination.getBodySiteCode().equals(""))
      {
        administeredScore += 1;
      }
      if (!vaccination.getAmount().equals("") && !vaccination.getAmount().equals("999") && !vaccination.getAmount().equals("0"))
      {
        administeredScore += 3;
      }
      if (!vaccination.getFacilityIdNumber().equals("") || !vaccination.getFacilityName().equals(""))
      {
        administeredScore += 4;
      }
      if (!vaccination.getGivenBy().equals("") || !vaccination.getGivenByNameFirst().equals("") || !vaccination.getGivenByNameLast().equals(""))
      {
        administeredScore += 4;
      }

      if (vaccination.isAdministered() && administeredScore < 10)
      {
        registerIssue(pi.VaccinationInformationSourceIsAdministeredButAppearsToHistorical);
      }
      if (!vaccination.isAdministered() && administeredScore >= 10)
      {
        registerIssue(pi.VaccinationInformationSourceIsHistoricalButAppearsToBeAdministered);
      }

    }
    


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

  private boolean checkGroupMatch(VaccineCvx vaccineCvx, VaccineCpt vaccineCpt)
  {
    if (vaccineCvx.equals(vaccineCpt.getCvx()))
    {
      return true;
    }
    // CPT doesn't map to CVX, so need to check if it's in the same family
    VaccineGroupManager vgm = VaccineGroupManager.getVaccineGroupManager();
    for (VaccineCvxGroup cvxGroup : vgm.getVaccineCvxGroups(vaccineCvx))
    {
      for (VaccineCvxGroup cptGroup : vgm.getVaccineCvxGroups(vaccineCpt.getCvx()))
      {
        if (cvxGroup.getVaccineGroup().equals(cptGroup.getVaccineGroup()))
        {
          return true;
        }
      }
    }
    return false;
  }

  private void validateHeader()
  {
    documentHeaderMain("Header");
    MessageHeader header = message.getMessageHeader();
    if (notEmpty(header.getReceivingApplication(), pi.Hl7MshReceivingApplicationIsMissing))
    {
      // TODO
    }
    if (notEmpty(header.getReceivingFacility(), pi.Hl7MshReceivingFacilityIsMissing))
    {
      // TODO
    }
    if (notEmpty(header.getSendingApplication(), pi.Hl7MshSendingApplicationIsMissing))
    {
      // TODO
    }
    for (SectionValidator sectionValidator : headerValidators)
    {
      sectionValidator.validateHeader(message, this);
    }

    handleCodeReceived(header.getAckTypeApplication(), PotentialIssues.Field.HL7_MSH_APP_ACK_TYPE);
    handleCodeReceived(header.getAckTypeAccept(), PotentialIssues.Field.HL7_MSH_ACCEPT_ACK_TYPE);

    if (notEmpty(header.getMessageControl(), pi.Hl7MshMessageControlIdIsMissing))
    {
      message.setMessageKey(header.getMessageControl());
    }
    if (notEmpty(header.getMessageDate(), pi.Hl7MshMessageDateIsMissing))
    {
      // Give the calendar a 12 hour lee-way. This accomodates systems that use
      // a clock that is out of time.
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(header.getMessageDate());
      calendar.add(Calendar.HOUR_OF_DAY, -12);
      if (message.getReceivedDate().before(calendar.getTime()))
      {
        registerIssue(pi.Hl7MshMessageDateIsInFuture);
      }
    }
    if (notEmpty(header.getMessageType(), pi.Hl7MshMessageTypeIsMissing))
    {
      if (!header.getMessageType().equals("VXU"))
      {
        registerIssue(pi.Hl7MshMessageTypeIsUnrecognized);
      }
      if (notEmpty(header.getMessageTrigger(), pi.Hl7MshMessageTriggerIsMissing))
      {
        if (!header.getMessageTrigger().equals("V04"))
        {
          registerIssue(pi.Hl7MshMessageTriggerIsUnrecognized);
        }
        if (!header.getMessageVersion().equals("2.3.1") && !header.getMessageVersion().equals("2.4"))
        {
          if (notEmpty(header.getMessageStructure(), pi.Hl7MshMessageStructureIsMissing))
          {
            if (!header.getMessageStructure().equals("VXU_V04"))
            {
              registerIssue(pi.Hl7MshMessageStructureIsUnrecognized);
            }
          }
        }
        // TODO
      }
      // TODO
    }
    handleCodeReceived(header.getProcessingStatus(), PotentialIssues.Field.HL7_MSH_PROCESSING_ID);
    if (header.getProcessingStatusCode().equals("T"))
    {
      registerIssue(pi.Hl7MshProcessingIdIsValuedAsTraining);
    } else if (header.getProcessingStatusCode().equals("P"))
    {
      registerIssue(pi.Hl7MshProcessingIdIsValuedAsProduction);
    } else if (header.getProcessingStatusCode().equals("D"))
    {
      registerIssue(pi.Hl7MshProcessingIdIsValuedAsDebug);
    }
    if (notEmpty(header.getMessageVersion(), pi.Hl7MshVersionIsMissing))
    {
      if (header.getMessageVersion().startsWith("2.5"))
      {
        registerIssue(pi.Hl7MshVersionIsValuedAs2_5);
      } else if (header.getMessageVersion().startsWith("2.3"))
      {
        registerIssue(pi.Hl7MshVersionIsValuedAs2_3_1);
      } else if (header.getMessageVersion().startsWith("2.4"))
      {
        registerIssue(pi.Hl7MshVersionIsValuedAs2_4);
      } else
      {
        registerIssue(pi.Hl7MshVersionIsUnrecognized);
      }
    }

    handleCodeReceived(header.getCountry(), PotentialIssues.Field.HL7_MSH_COUNTRY_CODE);
    handleCodeReceived(header.getCharacterSet(), PotentialIssues.Field.HL7_MSH_CHARACTER_SET);
    handleCodeReceived(header.getCharacterSetAlt(), PotentialIssues.Field.HL7_MSH_ALT_CHARACTER_SET);
  }

  private void validatePatient()
  {
    piAddress = PotentialIssues.Field.PATIENT_ADDRESS;
    piAddressCity = PotentialIssues.Field.PATIENT_ADDRESS_CITY;
    piAddressStreet = PotentialIssues.Field.PATIENT_ADDRESS_STREET;
    piAddressCountry = PotentialIssues.Field.PATIENT_ADDRESS_COUNTRY;
    piAddressCounty = PotentialIssues.Field.PATIENT_ADDRESS_COUNTY;
    piAddressState = PotentialIssues.Field.PATIENT_ADDRESS_STATE;
    piAddressStreet2 = PotentialIssues.Field.PATIENT_ADDRESS_STREET2;
    piAddressZip = PotentialIssues.Field.PATIENT_ADDRESS_ZIP;
    piAddressType = PotentialIssues.Field.PATIENT_ADDRESS_TYPE;
    validateAddress(patient.getAddress());

    String aliasFirst = patient.getAliasFirst();
    String aliasLast = patient.getAliasLast();
    notEmpty(aliasFirst + aliasLast, PotentialIssues.Field.PATIENT_ALIAS);

    if (patient.getBirthDate() == null)
    {
      registerIssue(pi.PatientBirthDateIsMissing);
    } else
    {
      if (message.getReceivedDate().before(trunc(patient.getBirthDate())))
      {
        registerIssue(pi.PatientBirthDateIsInFuture);
      }
      if (message.getMessageHeader().getMessageDate() != null)
      {
        if (message.getMessageHeader().getMessageDate().before(trunc(patient.getBirthDate())))
        {
          registerIssue(pi.PatientBirthDateIsAfterSubmission);
        }
      }
    }

    if (notEmpty(patient.getBirthMultiple(), pi.PatientBirthIndicatorIsMissing))
    {
      if (patient.getBirthMultiple().equals("Y"))
      {
        handleCodeReceived(patient.getBirthOrder(), PotentialIssues.Field.PATIENT_BIRTH_ORDER);
        if (patient.getBirthOrder().isEmpty())
        {
          registerIssue(pi.PatientBirthOrderIsMissingAndMultipleBirthIndicated);
        }
      } else if (patient.getBirthMultiple().equals("N"))
      {
        if (!patient.getBirthOrder().isEmpty() && !patient.getBirthOrderCode().equals("1"))
        {
          registerIssue(pi.PatientBirthOrderIsInvalid);
        }
      } else
      {
        registerIssue(pi.PatientBirthIndicatorIsInvalid);
      }
    } else
    {
      if (!patient.getBirthOrder().isEmpty())
      {
        registerIssue(pi.PatientBirthIndicatorIsMissing);
      }
    }
    notEmpty(patient.getBirthPlace(), pi.PatientBirthPlaceIsMissing);
    handleCodeReceived(patient.getEthnicity(), PotentialIssues.Field.PATIENT_ETHNICITY);

    if (patient.getNameFirst().length() > 3 && patient.getNameMiddle().length() == 0)
    {
      int pos = patient.getNameFirst().lastIndexOf(' ');
      if (pos > -1 && pos == patient.getNameFirst().length() - 2)
      {
        registerIssue(pi.PatientNameFirstMayIncludeMiddleInitial);
      }
    }

    specialNameHandling1(patient.getName());
    specialNameHandling2(patient.getName());
    specialNameHandling3(patient.getName());
    specialNameHandling5(patient.getName());
    specialNameHandling6(patient.getName());
    specialNameHandling7(patient.getName());
    if (notEmpty(patient.getNameFirst(), pi.PatientNameFirstIsMissing))
    {
      for (KnownName invalidName : knownNames.getKnownNameList(KnownName.INVALID_NAME))
      {
        if (invalidName.onlyNameFirst() && patient.getNameFirst().equalsIgnoreCase(invalidName.getNameFirst()))
        {
          registerIssue(pi.PatientNameFirstIsInvalid);
        }
      }
      if (!validNameChars(patient.getNameFirst()))
      {
        registerIssue(pi.PatientNameFirstIsInvalid);
      }
      // TODO PatientNameFirstMayIncludeMiddleInitial
    }
    handleCodeReceived(patient.getSex(), PotentialIssues.Field.PATIENT_GENDER);

    if (notEmpty(patient.getNameLast(), pi.PatientNameLastIsMissing))
    {
      for (KnownName invalidName : knownNames.getKnownNameList(KnownName.INVALID_NAME))
      {
        if (invalidName.onlyNameLast() && patient.getNameLast().equalsIgnoreCase(invalidName.getNameLast()))
        {
          registerIssue(pi.PatientNameLastIsInvalid);
        }
      }
      if (!validNameChars(patient.getNameLast()))
      {
        registerIssue(pi.PatientNameLastIsInvalid);
      }
    }
    if (notEmpty(patient.getIdMedicaid(), pi.PatientMedicaidNumberIsMissing))
    {
      String medicaid = patient.getIdMedicaidNumber();
      medicaid = validateNumber(medicaid, pi.PatientMedicaidNumberIsInvalid, 9);
      patient.setIdMedicaidNumber(medicaid);
    }

    String middleName = patient.getNameMiddle();
    if (notEmpty(middleName, pi.PatientMiddleNameIsMissing))
    {
      for (KnownName invalidName : knownNames.getKnownNameList(KnownName.INVALID_NAME))
      {
        if (invalidName.onlyNameMiddle() && patient.getNameMiddle().equalsIgnoreCase(invalidName.getNameMiddle()))
        {
          // TODO middle name is invalid
          patient.setNameMiddle("");
        }
      }

      // PatientMiddleNameMayBeInitial
      middleName = patient.getNameMiddle();
      if (middleName.length() == 1)
      {
        registerIssue(pi.PatientMiddleNameMayBeInitial);
      }
    }

    if (patient.getNameMiddle().endsWith("."))
    {
      middleName = patient.getNameMiddle();
      middleName = middleName.substring(0, middleName.length() - 1);
      patient.setNameMiddle(middleName);
      if (!validNameChars(patient.getNameMiddle()))
      {
        registerIssue(pi.PatientMiddleNameIsInvalid);
      }
    }

    if (!isEmpty(patient.getNameSuffix()))
    {
      if (patient.getNameSuffix().equalsIgnoreCase("11") || patient.getNameSuffix().equalsIgnoreCase("2nd"))
      {
        patient.setNameSuffix("II");
      } else if (patient.getNameSuffix().equalsIgnoreCase("111") || patient.getNameSuffix().equalsIgnoreCase("3rd"))
      {
        patient.setNameSuffix("III");
      } else if (patient.getNameSuffix().equalsIgnoreCase("4th"))
      {
        patient.setNameSuffix("IV");
      }
      boolean isValid = false;
      for (String valid : VALID_SUFFIX)
      {
        if (patient.getNameSuffix().equalsIgnoreCase(valid))
        {
          isValid = true;
        }
      }
      if (!isValid)
      {
        // TODO suffix is invalid
        patient.setNameSuffix("");
      }
    }

    handleCodeReceived(patient.getName().getType(), PotentialIssues.Field.PATIENT_NAME_TYPE_CODE);

    if (knownNames.match(patient, KnownName.UNNAMED_NEWBORN))
    {
      registerIssue(pi.PatientNameMayBeTemporaryNewbornName);
    }

    if (knownNames.match(patient, KnownName.TEST_PATIENT))
    {
      registerIssue(pi.PatientNameMayBeTestName);
    }

    if (knownNames.match(patient, KnownName.JUNK_NAME))
    {
      registerIssue(pi.PatientNameHasJunkName);
    }

    if (notEmpty(patient.getMotherMaidenName(), pi.PatientMotherSMaidenNameIsMissing))
    {
      for (KnownName invalidName : knownNames.getKnownNameList(KnownName.INVALID_NAME))
      {
        if (invalidName.onlyNameLast() && patient.getMotherMaidenName().equalsIgnoreCase(invalidName.getNameLast()))
        {
          registerIssue(pi.PatientMotherSMaidenNameIsInvalid);
          patient.setMotherMaidenName("");
        }
      }
      for (KnownName junkName : knownNames.getKnownNameList(KnownName.JUNK_NAME))
      {
        if (junkName.onlyNameLast() && patient.getMotherMaidenName().equalsIgnoreCase(junkName.getNameLast()))
        {
          registerIssue(pi.PatientMotherSMaidenNameHasJunkName);
          patient.setMotherMaidenName("");
        }
      }
      for (KnownName junkName : knownNames.getKnownNameList(KnownName.INVALID_PREFIXES))
      {
        if (junkName.onlyNameLast() && patient.getMotherMaidenName().equalsIgnoreCase(junkName.getNameLast()))
        {
          registerIssue(pi.PatientMotherSMaidenNameHasInvalidPrefixes);
          patient.setMotherMaidenName("");
        }
      }
      if (patient.getMotherMaidenName().length() == 1)
      {
        registerIssue(pi.PatientMotherSMaidenNameIsTooShort);
        patient.setMotherMaidenName("");
      }

    }

    // TODO PatientNameMayBeTemporaryNewbornName
    // TODO PatientNameMayBeTestName

    validatePhone(patient.getPhone(), PotentialIssues.Field.PATIENT_PHONE, PotentialIssues.Field.PATIENT_PHONE_TEL_USE_CODE,
        PotentialIssues.Field.PATIENT_PHONE_TEL_EQUIP_CODE);

    notEmpty(patient.getFacilityName(), pi.PatientPrimaryFacilityNameIsMissing);
    handleCodeReceived(patient.getFacility().getId(), PotentialIssues.Field.PATIENT_PRIMARY_FACILITY_ID);
    handleCodeReceived(patient.getPrimaryLanguage(), PotentialIssues.Field.PATIENT_PRIMARY_LANGUAGE);
    handleCodeReceived(patient.getPhysician(), PotentialIssues.Field.PATIENT_PRIMARY_PHYSICIAN_ID);
    notEmpty(patient.getPhysician().getName(), pi.PatientPrimaryPhysicianNameIsMissing);
    if (notEmpty(patient.getProtectionCode(), PotentialIssues.Field.PATIENT_PROTECTION_INDICATOR))
    {
      handleCodeReceived(patient.getProtection(), PotentialIssues.Field.PATIENT_PROTECTION_INDICATOR);
      if (patient.getProtectionCode().equals("Y"))
      {
        registerIssue(pi.PatientProtectionIndicatorIsValuedAsYes);
      } else if (patient.getProtectionCode().equals("N"))
      {
        registerIssue(pi.PatientProtectionIndicatorIsValuedAsNo);
      }
    }
    handleCodeReceived(patient.getPublicity(), PotentialIssues.Field.PATIENT_PUBLICITY_CODE);
    handleCodeReceived(patient.getRace(), PotentialIssues.Field.PATIENT_RACE);
    notEmpty(patient.getIdRegistry(), pi.PatientRegistryIdIsMissing);

    if (notEmpty(patient.getIdRegistryNumber(), PotentialIssues.Field.PATIENT_REGISTRY_ID))
    {
      // TODO PatientRegistryIdIsUnrecognized
    }
    if (notEmpty(patient.getIdSsnNumber(), PotentialIssues.Field.PATIENT_SSN))
    {
      String ssn = patient.getIdSsnNumber();
      ssn = validateSsn(ssn, pi.PatientSsnIsInvalid);
      patient.setIdSsnNumber(ssn);
    }
    if (notEmpty(patient.getIdSubmitterNumber(), PotentialIssues.Field.PATIENT_SUBMITTER_ID))
    {
      notEmpty(patient.getIdSubmitterAssigningAuthorityCode(), PotentialIssues.Field.PATIENT_SUBMITTER_ID_AUTHORITY);
      notEmpty(patient.getIdSubmitterTypeCode(), PotentialIssues.Field.PATIENT_SUBMITTER_ID_TYPE_CODE);
    }

    // TODO PatientVfcEffectiveDateIsBeforeBirth
    // TODO PatientVfcEffectiveDateIsInFuture
    // TODO PatientVfcEffectiveDateIsInvalid
    // TODO PatientVfcEffectiveDateIsMissing
    handleCodeReceived(patient.getFinancialEligibility(), PotentialIssues.Field.PATIENT_VFC_STATUS);
    if (patient.getFinancialEligibilityDate() != null)
    {
      if (patient.getBirthDate() != null && patient.getFinancialEligibilityDate().before(trunc(patient.getBirthDate())))
      {
        registerIssue(pi.PatientVfcEffectiveDateIsBeforeBirth);
      }
      if (message.getReceivedDate().before(trunc(patient.getFinancialEligibilityDate())))
      {
        registerIssue(pi.PatientVfcEffectiveDateIsInFuture);
      }
    }
    if (notEmpty(patient.getDeathIndicator(), pi.PatientDeathIndicatorIsMissing))
    {
      if (patient.getDeathIndicator().equals("Y"))
      {
        if (notEmpty(patient.getDeathDate(), pi.PatientDeathDateIsMissing))
        {
          if (patient.getBirthDate() != null && patient.getDeathDate().before(trunc(patient.getBirthDate())))
          {
            registerIssue(pi.PatientDeathDateIsBeforeBirth);
          }
          if (message.getReceivedDate().before(trunc(patient.getDeathDate())))
          {
            registerIssue(pi.PatientDeathDateIsInFuture);
          }
        }
      } else
      {
        if (patient.getDeathDate() != null)
        {
          registerIssue(pi.PatientDeathIndicatorIsInconsistent);
        }
      }
    }

    boolean patientUnderAged = false;
    if (patient.getBirthDate() != null)
    {
      long age = now.getTime() - patient.getBirthDate().getTime();
      patientUnderAged = age < (ABOUT_ONE_YEAR * 18);

      if (age > (ABOUT_ONE_YEAR * 99))
      {
        // patient is over 99 years old, flag record. May be a bad birth date
        registerIssue(pi.PatientBirthDateIsVeryLongAgo);
      }
    }
    patient.setUnderAged(patientUnderAged);

    if (patient.getSystemCreationDate() == null)
    {
      registerIssue(pi.PatientSystemCreationDateIsMissing);
    } else
    {
      if (patient.getBirthDate() != null)
      {
        if (patient.getSystemCreationDate().before(patient.getBirthDate()))
        {
          registerIssue(pi.PatientSystemCreationDateIsBeforeBirth);
        }
      }
      if (message.getReceivedDate().before(trunc(patient.getSystemCreationDate())))
      {
        registerIssue(pi.PatientSystemCreationDateIsInFuture);
      }
    }

    for (SectionValidator sectionValidator : patientValidators)
    {
      sectionValidator.validatePatient(message, this);
    }

  }

  private String validateNumber(String numericId, PotentialIssue invalidIssue, int length)
  {
    if (numericId.length() != length || numericId.equals("123456789") || numericId.equals("987654321") || hasTooManyConsecutiveChars(numericId, 6))
    {
      registerIssue(invalidIssue);
      numericId = "";
    }

    return numericId;
  }

  private String validateSsn(String numericSsn, PotentialIssue invalidIssue)
  {
    // fail if: starts with 000, middle two digits are 00, is not 9 numeric
    // chars, is 123456789, is 987654321, contains more than 6 consecutive
    // digits
    if ((numericSsn.indexOf("000") == 0) || (numericSsn.indexOf("00", 3) == 3) || (!numericSsn.matches("[0-9]{9}"))
        || (numericSsn.equals("123456789")) || numericSsn.equals("987654321") || (hasTooManyConsecutiveChars(numericSsn, 6)))
    {
      registerIssue(invalidIssue);
      numericSsn = "";
    }

    return numericSsn;
  }

  private void validatePhone(PhoneNumber phone, PotentialIssues.Field piPhone)
  {
    validatePhone(phone, piPhone, null, null);
  }

  private void validatePhone(PhoneNumber phone, PotentialIssues.Field piPhone, PotentialIssues.Field piPhoneTelUse,
      PotentialIssues.Field piPhoneTelEquip)
  {
    if (notEmpty(phone.getNumber(), piPhone))
    {
      if (phone.getAreaCode().equals("") || phone.getLocalNumber().equals(""))
      {
        registerIssue(pi.getIssue(piPhone, PotentialIssue.ISSUE_TYPE_IS_INCOMPLETE));
      }
      if (piPhoneTelUse != null)
      {
        handleCodeReceived(phone.getTelUse(), piPhoneTelUse);
      }
      if (piPhoneTelEquip != null)
      {
        handleCodeReceived(phone.getTelEquip(), piPhoneTelEquip);
      }
      if (!isValidPhone(phone))
      {
        registerIssue(pi.getIssue(piPhone, PotentialIssue.ISSUE_TYPE_IS_INVALID));
      }
    }
  }

  protected static boolean isValidPhone(PhoneNumber phone)
  {
    if (phone.getCountryCode().equals("") || phone.getCountryCode().equals("1") || phone.getCountryCode().equals("+1"))
    {
      // Validating all phone numbers using the North American Numbering Plan
      // (NANP)
      if (!phone.getAreaCode().equals(""))
      {
        if (!validPhone3Digit(phone.getAreaCode()))
        {
          return false;
        }
      }
      if (!phone.getLocalNumber().equals(""))
      {
        String num = phone.getLocalNumber();
        StringBuilder numOnly = new StringBuilder();
        for (int i = 0; i < num.length(); i++)
        {
          if (num.charAt(i) >= '0' && num.charAt(i) <= '9')
          {
            numOnly.append(num.charAt(i));
          }
        }
        num = numOnly.toString();
        if (num.length() != 7)
        {
          return false;
        }
        if (!validPhone3Digit(num.substring(0, 3)))
        {
          return false;
        }
        if (num.substring(1, 3).equals("11"))
        {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean validPhone3Digit(String s)
  {
    if (s == null || s.length() != 3)
    {
      return false;
    }
    if (s.charAt(0) < '2' || s.charAt(0) > '9')
    {
      return false;
    }
    if (s.charAt(1) < '0' || s.charAt(1) > '9')
    {
      return false;
    }
    if (s.charAt(2) < '0' || s.charAt(2) > '9')
    {
      return false;
    }
    return true;
  }

  private boolean validateAddress(Address address)
  {
    String city = address.getCity();
    if (notEmpty(city, piAddressCity))
    {
      if (city.equalsIgnoreCase("ANYTOWN") || city.length() <= 1)
      {
        registerIssue(pi.getIssue(piAddressCity, PotentialIssue.ISSUE_TYPE_IS_INVALID));
      }
    }
    if (address.getState().getCode().equalsIgnoreCase("us"))
    {
      address.getState().setCode("");
      address.getCountry().setCode("USA");
      address.getCountry().getCodeReceived().setCodeId(20006);
    } else if (address.getState().getCode().equalsIgnoreCase("mx") || address.getState().getCode().equalsIgnoreCase("mex")
        || address.getState().getCode().equalsIgnoreCase("mexico"))
    {
      address.getCountry().setCode(address.getState().getCode());
    }
    handleCodeReceived(address.getCountry(), piAddressCountry);
    CodeReceived countryCodeReceived = address.getCountry().getCodeReceived();
    if (countryCodeReceived == null || countryCodeReceived.getCodeValue() == null || countryCodeReceived.getCodeValue().equals(""))
    {
      countryCodeReceived = new CodeReceived();
      countryCodeReceived.setCodeValue("USA");
    }
    handleCodeReceived(address.getState(), piAddressState, countryCodeReceived);
    handleCodeReceived(address.getCountyParish(), piAddressCounty, address.getState().getCodeReceived());
    notEmpty(address.getStreet(), piAddressStreet);
    notEmpty(address.getStreet2(), piAddressStreet2);
    if (notEmpty(address.getZip(), piAddressZip))
    {
      if (address.getCountryCode().equals("") || address.getCountryCode().equals("USA"))
      {
        String zip = address.getZip();
        int dash = zip.indexOf('-');
        boolean valid = true;
        if (dash == -1)
        {
          if (zip.length() != 5)
          {
            valid = false;
          }
        } else if (dash != 5)
        {
          valid = false;
        }
        if (valid && zip.length() >= 5)
        {
          for (int i = 0; i < zip.length(); i++)
          {
            if (i == 5)
            {
              continue;
            }
            char c = zip.charAt(i);
            if (c < '0' || c > '9')
            {
              valid = false;
              break;
            }
          }
        }
        if (!valid)
        {
          registerIssue(pi.getIssue(piAddressZip, PotentialIssue.ISSUE_TYPE_IS_INVALID));
        }
      }
    }

    if (isEmpty(address.getStreet()) || isEmpty(city) || isEmpty(address.getZip()) || isEmpty(address.getState()))
    {
      registerIssue(pi.getIssue(piAddress, PotentialIssue.ISSUE_TYPE_IS_MISSING));
      return false;
    }

    if (piAddressType != null)
    {
      // Only validate if address type is set if address has been set
      notEmpty(address.getTypeCode(), piAddressType);
    }
    return true;
  }

  private boolean notEmpty(Id id, PotentialIssue potentialIssue)
  {
    return notEmpty(id.getNumber(), potentialIssue);
  }

  private boolean notEmpty(Date date, PotentialIssue potentialIssue)
  {
    if (date == null)
    {
      registerIssue(potentialIssue);
      return false;
    }
    return true;
  }

  private boolean notEmpty(String fieldValue, PotentialIssues.Field field)
  {
    return notEmpty(fieldValue, pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_MISSING), true);
  }

  private boolean notEmpty(String fieldValue, PotentialIssues.Field field, boolean notSilent)
  {
    return notEmpty(fieldValue, pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_MISSING), notSilent);
  }

  protected boolean notEmpty(String fieldValue, PotentialIssue potentialIssue)
  {
    return notEmpty(fieldValue, potentialIssue, true);
  }

  protected boolean notEmpty(String fieldValue, PotentialIssue potentialIssue, boolean notSilent)
  {
    boolean empty = fieldValue == null || fieldValue.equals("");
    if (notSilent && empty)
    {
      registerIssue(potentialIssue);
    }
    return !empty;
  }

  private boolean notEmpty(Name name, PotentialIssue potentialIssue)
  {
    boolean empty = true;
    if (name != null)
    {
      if (name.getFirst() != null && !name.getFirst().equals(""))
      {
        empty = false;
      } else if (name.getLast() != null && !name.getLast().equals(""))
      {
        empty = false;
      }
    }
    if (empty)
    {
      registerIssue(potentialIssue);
    }
    return !empty;
  }

  protected void handleCodeReceived(Id id, PotentialIssues.Field field)
  {

    handleCodeReceived(id, field, true, null);
  }

  protected void handleCodeReceived(Id id, PotentialIssues.Field field, CodeReceived context)
  {

    handleCodeReceived(id, field, true, context);
  }

  protected void handleCodeReceived(Id id, PotentialIssues.Field field, boolean notSilent)
  {
    handleCodeReceived(id, field, notSilent, null);
  }

  protected void handleCodeReceived(Id id, PotentialIssues.Field field, boolean notSilent, CodeReceived context)
  {
    CodeReceived cr = null;
    if (notEmpty(id.getNumber(), field))
    {
      CodeTable codeTable = CodesReceived.getCodeTable(id.getTableType());
      cr = getCodeReceived(id.getNumber(), id.getName().getFullName(), codeTable, context);
      cr.incReceivedCount();
      session.saveOrUpdate(cr);
      if (cr.getCodeStatus().isValid())
      {
        id.setCodeReceived(cr);
      } else if (cr.getCodeStatus().isInvalid())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_INVALID), cr);
        }
      } else if (cr.getCodeStatus().isUnrecognized())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_UNRECOGNIZED), cr);
        }
      } else if (cr.getCodeStatus().isDeprecated())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_DEPRECATE), cr);
        }
      } else if (cr.getCodeStatus().isIgnored())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_IGNORED), cr);
        }
      }
      id.setNumber(cr.getCodeValue());
    }
    id.setCodeReceived(cr);
  }

  protected void handleCodeReceived(CodedEntity codedEntity, PotentialIssues.Field field)
  {
    handleCodeReceived(codedEntity, field, true, null);
  }

  protected void handleCodeReceived(CodedEntity codedEntity, PotentialIssues.Field field, CodeReceived context)
  {
    handleCodeReceived(codedEntity, field, true, context);
  }

  protected void handleCodeReceived(CodedEntity codedEntity, PotentialIssues.Field field, boolean notSilent)
  {
    handleCodeReceived(codedEntity, field, notSilent, null);
  }

  protected void handleCodeReceived(CodedEntity codedEntity, PotentialIssues.Field field, boolean notSilent, CodeReceived context)
  {
    CodeReceived cr = null;
    if (notEmpty(codedEntity.getCode(), field, notSilent))
    {
      CodeTable codeTable = CodesReceived.getCodeTable(codedEntity.getTableType());
      cr = getCodeReceived(codedEntity.getCode(), codedEntity.getText(), codeTable, context);

      cr.incReceivedCount();
      session.update(cr);
      if (cr.getCodeStatus().isValid())
      {
        codedEntity.setCodeReceived(cr);
        return;
      } else if (cr.getCodeStatus().isInvalid())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_INVALID), cr);
        }
      } else if (cr.getCodeStatus().isUnrecognized())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_UNRECOGNIZED), cr);
        }
      } else if (cr.getCodeStatus().isDeprecated())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_DEPRECATE), cr);
        }
      } else if (cr.getCodeStatus().isIgnored())
      {
        if (notSilent)
        {
          registerIssue(pi.getIssue(field, PotentialIssue.ISSUE_TYPE_IS_IGNORED), cr);
        }
      }
      codedEntity.setCode(cr.getCodeValue());

    }
    codedEntity.setCodeReceived(cr);
  }

  private static String trunc(String s, int length)
  {
    if (s == null || s.length() < length)
    {
      return s;
    }
    return s.substring(0, length);
  }

  protected CodeReceived getCodeReceived(String receivedValue, String receivedLabel, CodeTable codeTable, CodeReceived context)
  {
    receivedValue = trunc(receivedValue, 50);
    receivedLabel = trunc(receivedLabel, 30);
    CodesReceived crs = profile.getCodesReceived(session);
    CodeReceived cr = crs.getCodeReceived(receivedValue, codeTable, context);
    if (cr == null)
    {
      cr = new CodeReceived();
      cr.setProfile(profile);
      cr.setTable(codeTable);
      cr.setReceivedValue(receivedValue);
      cr.setCodeValue(codeTable.getDefaultCodeValue());
      cr.setCodeStatus(CodeStatus.UNRECOGNIZED);
      cr.setCodeLabel(receivedLabel);
      if (context != null)
      {
        cr.setContextValue(context.getContextWithCodeValue());
      }
      profile.registerCodeReceived(cr, context, session);
      session.saveOrUpdate(cr);
    } else if (!cr.getProfile().equals(profile))
    {
      cr = new CodeReceived(cr, profile, receivedLabel);
      profile.registerCodeReceived(cr, context, session);
      session.saveOrUpdate(cr);
      // first time code was received
    }

    if (qualityCollector != null)
    {
      qualityCollector.registerCodeReceived(cr);
    }

    return cr;
  }

  protected static int monthsBetween(Date startDate, Date endDate)
  {
    Calendar cal = Calendar.getInstance();
    cal.setTime(endDate);
    int endMonth = cal.get(Calendar.MONTH);
    int endYear = cal.get(Calendar.YEAR);
    cal.setTime(startDate);
    int startMonth = cal.get(Calendar.MONTH);
    int startYear = cal.get(Calendar.YEAR);
    return ((endYear - startYear) * cal.getMaximum(Calendar.MONTH)) + (endMonth - startMonth);
  }

  protected static boolean hasTooManyConsecutiveChars(String input, int maxConsecutiveChars)
  {
    char lastC = 'S';
    int count = 0;
    for (char c : input.toCharArray())
    {
      if (lastC == c)
      {
        count++;
      } else
      {
        count = 1;
      }
      if (count > maxConsecutiveChars)
      {
        return true;
      }
      lastC = c;
    }

    return false;
  }

  private static boolean isEmpty(String s)
  {
    return s == null || s.equals("");
  }

  private static boolean isEmpty(CodedEntity ce)
  {
    return ce == null || ce.isEmpty();
  }

  private static boolean areEqual(String field1, String field2)
  {
    return field1 != null && field1.equals(field2);
  }

  protected static void specialNameHandling1(Name name)
  {
    name.setFirst(specialNameHandling1(name.getFirst()));
    name.setLast(specialNameHandling1(name.getLast()));
    name.setMiddle(specialNameHandling1(name.getMiddle()));
    name.setSuffix(specialNameHandling1(name.getSuffix()));
  }

  protected static String specialNameHandling1(String s)
  {
    return s.replace('0', 'o');
  }

  protected static void specialNameHandling2(Name name)
  {
    name.setFirst(specialNameHandling2(name.getFirst()));
    name.setLast(specialNameHandling2(name.getLast()));
    name.setMiddle(specialNameHandling2(name.getMiddle()));
    name.setSuffix(specialNameHandling2(name.getSuffix()));
  }

  protected static String specialNameHandling2(String s)
  {
    return s.replace(',', ' ');
  }

  protected static void specialNameHandling3(Name name)
  {
    if (isEmpty(name.getMiddle()))
    {
      int pos = name.getFirst().lastIndexOf(' ');
      if (pos > 0)
      {
        String middleName = name.getFirst().substring(pos).trim();
        name.setMiddle(middleName);
      }
    }
  }

  protected static void specialNameHandling5(Name name)
  {
    name.setFirst(specialNameHandling5(name.getFirst()));
    name.setLast(specialNameHandling5(name.getLast()));
    name.setMiddle(specialNameHandling5(name.getMiddle()));
    name.setSuffix(specialNameHandling5(name.getSuffix()));
  }

  protected static String specialNameHandling5(String s)
  {
    int pos = s.indexOf('(');
    if (pos > 0)
    {
      s = s.substring(0, pos).trim();
    }
    pos = s.indexOf('{');
    if (pos > 0)
    {
      s = s.substring(0, pos).trim();
    }
    pos = s.indexOf('[');
    if (pos > 0)
    {
      s = s.substring(0, pos).trim();
    }
    return s;
  }

  protected static void specialNameHandling6(Name name)
  {
    if (name.getFirst().toUpperCase().endsWith(" JR"))
    {
      name.setSuffix("Jr");
      name.setFirst(name.getFirst().substring(0, name.getFirst().length() - " JR".length()));
    }
  }

  protected static void specialNameHandling7(Name name)
  {
    if (!isEmpty(name.getMiddle()))
    {
      int pos = name.getMiddle().lastIndexOf(".");
      if (pos > 0)
      {
        name.setMiddle(name.getMiddle().substring(0, pos));
      }
    }
  }

  protected static boolean validNameChars(String s)
  {
    for (char c : s.toUpperCase().toCharArray())
    {
      if ((c < 'A' || c > 'Z') && c != '-' && c != '\'' && c != ' ' && c != '.')
      {
        return false;
      }
    }
    return true;
  }

  protected static Date trunc(Date d)
  {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }
}
