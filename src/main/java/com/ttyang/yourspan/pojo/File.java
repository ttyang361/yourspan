package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("file")
public class File implements Serializable {
    /**
     * 文件id
     */
    @TableId(value = "f_id", type = IdType.AUTO)
    private Integer fid;
    /**
     * 文件名
     */
    @TableField(value = "f_name")
    private String fName;
    /**
     * 所属者id
     */
    @TableField(value = "f_owner_id")
    private Integer fOwnerId;
    /**
     * 文件所属卷名
     */
    @TableField(value = "f_group")
    private String fGroup;
    /**
     * 文件路径（文件路径应作为表内唯一索引来确保不能存在相同文件）
     */
    @TableField(value = "f_path")
    private String fPath;
    /**
     * 文件所属文件夹id
     */
    @TableField(value = "f_virtual_folder")
    private Integer fVirtualFolder;
    /**
     * 文件创建时间
     */
    @TableField(value = "f_create_time")
    private Date fCreateTime;
    /**
     * 最后一次修改时间
     */
    @TableField(value = "f_last_modified_time")
    private Date fLastModifiedTime;
    /**
     * 文件是否公开
     */
    @TableField(value = "authority")
    private Boolean authority;
    /**
     * 文件大小
     */
    @TableField(value = "f_size")
    private Long fSize;
    /**
     * 文件是否被删除
     */
    @TableField(value = "f_delete")
    private Boolean fDelete;
    /**
     * 当前用户对该文件的评分
     */
    @TableField(exist = false)
    private Integer rate;
    /**
     * 该文件的平均评分
     */
    @TableField(exist = false)
    private Double avgRate;
}
