package com.jstakun.gms.android.utils;

public class Token 
{
  private final String token;
  private final String secret;

  /**
   * Default constructor
   * 
   * @param token token value
   * @param secret token secret
   */
  public Token(String token, String secret)
  {
    this.token = token;
    this.secret = secret;
  }

  public String getToken()
  {
    return token;
  }

  public String getSecret()
  {
    return secret;
  }

  @Override
  public String toString()
  {
    return String.format("Token[%s , %s]", token, secret);
  }
}
