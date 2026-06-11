package com.example.noise.common;

/**
 * 无权限异常 — 用于全局异常处理器返回 403
 */
public class ForbiddenException extends RuntimeException {
  public ForbiddenException(String message) {
    super(message);
  }
}
