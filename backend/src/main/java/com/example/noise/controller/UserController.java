package com.example.noise.controller;

import com.example.noise.common.Result;
import com.example.noise.entity.dto.ChangePasswordRequest;
import com.example.noise.entity.dto.UserLoginRequest;
import com.example.noise.entity.dto.UserLoginResponse;
import com.example.noise.entity.dto.UserRegisterRequest;
import com.example.noise.entity.dto.UserProfileResponse;
import com.example.noise.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** 用户注册（公开） */
  @PostMapping("/api/auth/register")
  public Result<Void> register(@Valid @RequestBody UserRegisterRequest req) {
    userService.register(req);
    return Result.success(null, "注册成功");
  }

  /** 用户登录（公开），返回 JWT token */
  @PostMapping("/api/auth/login")
  public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest req) {
    UserLoginResponse data = userService.login(req);
    return Result.success(data, "登录成功");
  }

  /** 查看当前登录用户个人信息（需登录，从 JWT 解析 userId） */
  @GetMapping("/api/users/me")
  public Result<UserProfileResponse> getProfile(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    UserProfileResponse data = userService.getProfile(userId);
    return Result.success(data);
  }

  /** 修改当前用户密码（需登录，从 JWT 解析 userId） */
  @PutMapping("/api/users/me/password")
  public Result<Void> changePassword(HttpServletRequest request,
                                      @Valid @RequestBody ChangePasswordRequest req) {
    Long userId = (Long) request.getAttribute("userId");
    userService.changePassword(userId, req);
    return Result.success(null, "密码修改成功");
  }
}
