package org.openimmunizationsoftware.dqa.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerThreadMulti extends ManagerThread
{
  protected Map<String, ManagerThread> threads = new HashMap<String, ManagerThread>();
  
  public ManagerThreadMulti(String label) {
    super(label);
  }
  
  public List<ManagerThread> getManagerThreads()
  {
    return new ArrayList<ManagerThread>(threads.values());
  }
}
