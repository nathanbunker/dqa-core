/*
 * Copyright 2013 by Dandelion Software & Research, Inc (DSR)
 * 
 * This application was written for immunization information system (IIS) community and has
 * been released by DSR under an Apache 2 License with the hope that this software will be used
 * to improve Public Health.  
 */
package org.openimmunizationsoftware.dqa.manager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashManager
{

  public static String getMD5Hash(String text) throws NoSuchAlgorithmException
  {

    byte[] results;
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(text.getBytes());
    results = md.digest();
    // logger.debug(Support.viewAsHex(results));
    return viewAsHex(results);
  }

  public static String viewAsHex(String aString)
  {
    return viewAsHex(aString.getBytes());
  }

  public static String viewAsHex(byte[] bytes)
  {
    String sb = "";
    try
    {
      sb = getHexString(bytes);
    } catch (UnsupportedEncodingException e)
    {
      sb = "UnsupportedEncodingException thrown";
    }
    return sb;
  }

  static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8',
      (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F' };

  public static String getHexString(byte[] raw) throws UnsupportedEncodingException
  {
    if (raw == null)
      return "";
    byte[] hex = new byte[2 * raw.length];
    int index = 0;

    for (byte b : raw)
    {
      int v = b & 0xFF;
      hex[index++] = HEX_CHAR_TABLE[v >>> 4];
      hex[index++] = HEX_CHAR_TABLE[v & 0xF];
    }
    return new String(hex, "ASCII");
  }
}
