package com.example.noise.common;

/**
 * 未登录异常 — 用于全局异常处理器返回 401
 */
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
