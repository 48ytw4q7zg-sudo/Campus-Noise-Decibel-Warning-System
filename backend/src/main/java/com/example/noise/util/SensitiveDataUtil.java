package com.example.noise.util;

/**
 * 日志脱敏工具
 */
public class SensitiveDataUtil {

  private SensitiveDataUtil() {}

  public static String maskToken(String token) {
    if (token == null || token.length() <= 20) {
      return "***";
    }
    return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
  }

  public static String maskPassword(String password) {
    return "***";
  }
}
