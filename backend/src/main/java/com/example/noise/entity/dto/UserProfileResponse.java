package com.example.noise.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileResponse {

  private Long id;

  private String username;

  /** 角色：普通用户 / 管理员 */
  private String role;

  /** 状态：1=正常 0=禁用 */
  private Integer status;

  private LocalDateTime createTime;
}
