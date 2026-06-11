package com.example.noise.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

  /** 用户名，2-20 字符 */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 2, max = 20, message = "用户名长度须为2-20字符")
  private String username;

  /** R-08-4: 密码至少8位，必须包含字母和数字 */
  @NotBlank(message = "密码不能为空")
  @Size(min = 8, max = 32, message = "密码长度须为8-32字符")
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,32}$", message = "密码必须至少8位且包含字母和数字")
  private String password;

  /** 角色：普通用户 / 管理员 */
  @NotBlank(message = "角色不能为空")
  private String role;
}
