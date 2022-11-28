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
    @TableId(value = "uid",type = IdType.AUTO)
    private Integer uid;
    private String password;
    private String email;
    private String name;
    private String telephone;
    /**
     * 用户所属云盘根目录
     */
    @TableField(value = "root_fdir_path")
    private String rootFdirPath;
    /**
     *
     */
    private String avatarPath;
}
