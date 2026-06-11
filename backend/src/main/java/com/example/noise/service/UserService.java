package com.example.noise.service;

import com.example.noise.entity.dto.UserLoginRequest;
import com.example.noise.entity.dto.UserLoginResponse;
import com.example.noise.entity.dto.UserProfileResponse;
import com.example.noise.entity.dto.UserRegisterRequest;
import com.example.noise.entity.dto.ChangePasswordRequest;

/**
 * 用户 Service 接口 — 注册/登录/个人信息/修改密码
 */
public interface UserService {

  /**
   * 用户注册：校验用户名唯一性、role取值 → BCrypt 加密密码 → 写入数据库
   */
  void register(UserRegisterRequest req);

  /**
   * 用户登录：校验用户名存在、密码匹配、账号状态 → 签发 JWT token
   */
  UserLoginResponse login(UserLoginRequest req);

  /**
   * 查看个人信息：根据 userId 查询用户信息
   */
  UserProfileResponse getProfile(Long userId);

  /**
   * 修改密码：校验原密码 → BCrypt 加密新密码 → 更新
   */
  void changePassword(Long userId, ChangePasswordRequest req);
}
