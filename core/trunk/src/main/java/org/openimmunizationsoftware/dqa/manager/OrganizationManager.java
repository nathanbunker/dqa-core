package org.openimmunizationsoftware.dqa.manager;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.openimmunizationsoftware.dqa.db.model.Organization;

public class OrganizationManager
{
  
  private static SessionFactory factory;
  
  public static SessionFactory getSessionFactory()
  {
    if (factory == null)
    {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory;
  }
  
}
