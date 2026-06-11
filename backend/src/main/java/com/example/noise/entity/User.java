package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户名，唯一索引 uq_username */
  @TableField("username")
  private String username;

  /** BCrypt 密文，禁止在响应中返回 */
  @JsonIgnore
  @TableField("password")
  private String password;

  /** 角色：普通用户 / 管理员 */
  @TableField("role")
  private String role;

  /** 状态：1=正常 0=禁用 */
  @TableField("status")
  private Integer status;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;

  /** 逻辑删除：1=已删除 0=未删除 */
  @TableLogic
  @TableField("is_deleted")
  private Integer isDeleted;
}
