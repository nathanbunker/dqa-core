package org.openimmunizationsoftware.dqa.process;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.dqa.db.model.received.NextOfKin;
import org.openimmunizationsoftware.dqa.db.model.received.Patient;
import org.openimmunizationsoftware.dqa.db.model.received.Vaccination;
import org.tch.fc.model.ForecastActual;

public class QueryResult
{
  private Patient patient = null;
  private List<NextOfKin> nextOfKinList = new ArrayList<NextOfKin>();
  private List<Vaccination> vaccinationList = new ArrayList<Vaccination>();
  
  private List<ForecastActual> forecastActualList = new ArrayList<ForecastActual>();
  
  public List<ForecastActual> getForecastActualList()
  {
    return forecastActualList;
  }
  public void setForecastActualList(List<ForecastActual> forecastActualList)
  {
    this.forecastActualList = forecastActualList;
  }
  public Patient getPatient()
  {
    return patient;
  }
  public void setPatient(Patient patient)
  {
    this.patient = patient;
  }
  public List<NextOfKin> getNextOfKinList()
  {
    return nextOfKinList;
  }
  public void setNextOfKinList(List<NextOfKin> nextOfKinList)
  {
    this.nextOfKinList = nextOfKinList;
  }
  public List<Vaccination> getVaccinationList()
  {
    return vaccinationList;
  }
  public void setVaccinationList(List<Vaccination> vaccinationList)
  {
    this.vaccinationList = vaccinationList;
  }
  
}
