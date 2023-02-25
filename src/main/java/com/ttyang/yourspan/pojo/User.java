package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    /**
     * 用户id
     */
    @TableId(value = "uid", type = IdType.AUTO)
    private Integer uid;
    /**
     * 用户密码
     */
    private String password;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 用户昵称
     */
    private String name;
    /**
     * 用户手机号
     */
    private String telephone;
    /**
     * 用户所属云盘根目录
     */
    @TableField(value = "root_fdir_path")
    private Integer rootFdirPath;
    /**
     * 用户头像文件路径
     */
    @TableField(value = "avatar_path")
    private String avatarPath;
    /**
     * 是否为管理员
     */
    @TableField(value = "is_admin")
    private Boolean isAdmin;
    /**
     * 用户是否被冻结
     */
    @TableField(value = "is_frozen")
    private Boolean isFrozen;
}
