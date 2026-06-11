package com.example.noise.entity.dto;

import lombok.Data;

@Data
public class UserLoginResponse {

  /** JWT token */
  private String token;

  /** 用户 ID */
  private Long userId;

  /** 角色：普通用户 / 管理员 */
  private String role;
}
