package org.openimmunizationsoftware.dqa.validate;

import org.openimmunizationsoftware.dqa.db.model.received.types.PhoneNumber;

import junit.framework.TestCase;

public class ValidatorTest extends TestCase
{
  public void testValidatePhone()
  {
    PhoneNumber phone;
    phone = new PhoneNumber("(801)285-7998");
    assertTrue(Validator.isValidPhone(phone));
    phone = new PhoneNumber("801-285-7998");
    assertTrue(Validator.isValidPhone(phone));
    phone = new PhoneNumber("8012857998");
    assertTrue(Validator.isValidPhone(phone));
    phone = new PhoneNumber("801.285.7998");
    assertTrue(Validator.isValidPhone(phone));
    phone = new PhoneNumber("801", "285-7998");
    assertTrue(Validator.isValidPhone(phone));
    phone = new PhoneNumber("123", "285-7998");
    assertFalse("123 is an invalid area code", Validator.isValidPhone(phone));
    phone = new PhoneNumber("801", "123-7998");
    assertFalse("123 is an invalid local exchange", Validator.isValidPhone(phone));
    phone = new PhoneNumber("801", "811-7998");
    assertFalse("811 is an invalid local exchange", Validator.isValidPhone(phone));
    phone = new PhoneNumber("801", "285-1234");
    assertTrue(Validator.isValidPhone(phone));
  }
}
