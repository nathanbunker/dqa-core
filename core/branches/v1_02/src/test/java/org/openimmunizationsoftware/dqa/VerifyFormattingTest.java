package org.openimmunizationsoftware.dqa;

import java.util.Formatter;

import junit.framework.TestCase;

public class VerifyFormattingTest extends TestCase
{

   public void testSomething() {
     Formatter formatter = new Formatter();
    assertEquals("Hi 0.56", formatter.format("Hi %.2f", 0.56)); 
   }
   
}
