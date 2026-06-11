package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.User;
import com.example.noise.entity.dto.UserLoginRequest;
import com.example.noise.entity.dto.UserLoginResponse;
import com.example.noise.entity.dto.UserProfileResponse;
import com.example.noise.entity.dto.UserRegisterRequest;
import com.example.noise.entity.dto.ChangePasswordRequest;
import com.example.noise.mapper.UserMapper;
import com.example.noise.service.UserService;
import com.example.noise.util.JwtUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;

  public UserServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /** 允许的角色值（业务规则校验） */
  private static final Set<String> VALID_ROLES = Set.of("普通用户", "管理员");

  @Override
  public void register(UserRegisterRequest req) {
    // 校验角色取值
    if (req.getRole() == null || !VALID_ROLES.contains(req.getRole())) {
      throw new BusinessException(1002, "角色取值错误");
    }

    // 校验用户名唯一性
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, req.getUsername());
    User existing = userMapper.selectOne(wrapper);
    if (existing != null) {
      throw new BusinessException(1001, "用户名已存在");
    }

    // BCrypt 加密密码后写入数据库
    User user = new User();
    user.setUsername(req.getUsername());
    user.setPassword(new BCryptPasswordEncoder().encode(req.getPassword()));
    user.setRole(req.getRole());
    user.setStatus(1);
    userMapper.insert(user);
  }

  @Override
  public UserLoginResponse login(UserLoginRequest req) {
    // 根据用户名查询用户
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, req.getUsername());
    User user = userMapper.selectOne(wrapper);

    // 用户名不存在 → 统一提示"用户名或密码错误"（安全考虑，不泄露用户是否存在）
    if (user == null) {
      throw new BusinessException(1003, "用户名或密码错误");
    }

    // 校验账号状态
    if (user.getStatus() != null && user.getStatus() == 0) {
      throw new BusinessException(1004, "账号已被禁用");
    }

    // BCrypt 校验密码
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    if (!encoder.matches(req.getPassword(), user.getPassword())) {
      throw new BusinessException(1003, "用户名或密码错误");
    }

    // 签发 JWT token
    String token = JwtUtils.generateToken(user.getId(), user.getRole());

    // 组装登录响应
    UserLoginResponse response = new UserLoginResponse();
    response.setToken(token);
    response.setUserId(user.getId());
    response.setRole(user.getRole());
    return response;
  }

  @Override
  public UserProfileResponse getProfile(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(1006, "用户不存在");
    }

    UserProfileResponse response = new UserProfileResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setRole(user.getRole());
    response.setStatus(user.getStatus());
    response.setCreateTime(user.getCreateTime());
    return response;
  }

  @Override
  public void changePassword(Long userId, ChangePasswordRequest req) {
    User user = userMapper.selectById(userId);

    // 校验原密码
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    if (user == null || !encoder.matches(req.getOldPassword(), user.getPassword())) {
      throw new BusinessException(1005, "原密码错误");
    }

    // BCrypt 加密新密码并更新
    user.setPassword(encoder.encode(req.getNewPassword()));
    userMapper.updateById(user);
  }
}
