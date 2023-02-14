package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("capacity")
public class Capacity {
    /**
     * 用户id
     */
    @TableId(value = "uid")
    private Integer uid;
    /**
     * 总容量
     */
    @TableField(value = "total")
    private Double total;
    /**
     * 已用容量
     */
    @TableField(value = "used")
    private Double used;
}
