package com.example.noise.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

  @NotBlank(message = "旧密码不能为空")
  private String oldPassword;

  /** 新密码，6-32 字符 */
  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 32, message = "新密码长度须为6-32字符")
  private String newPassword;
}
