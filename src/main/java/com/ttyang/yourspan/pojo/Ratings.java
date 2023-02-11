package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ratings")
public class Ratings {
    /**
     * 用户id
     */
    @TableField(value = "uid")
    private Integer uid;
    /**
     * 文件id
     */
    @TableField(value = "f_id")
    private Integer fid;
    /**
     * 偏好值
     */
    @TableField(value = "preference")
    private Integer preference;
}
