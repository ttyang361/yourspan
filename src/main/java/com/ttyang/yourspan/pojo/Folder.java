package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("folder")
public class Folder {
    /**
     * 文件夹id
     */
    @TableId(value = "fl_id", type = IdType.AUTO)
    private Integer flid;
    /**
     * 文件夹名称（注意，表内应该以fl_name和fl_owner_id作为联合唯一索引来使表内不能存在这两个属性相同的行，以确保一个文件夹内不能有重名文件夹）
     */
    @TableField(value = "fl_name")
    private String flName;
    /**
     * 父级文件夹id，当一个用户新注册时，应当创建一个初始的文件夹名为“我的资源”，该文件夹父级文件夹id为1（该父级文件夹仅作为所有初始文件夹的父级文件夹用）
     */
    @TableField(value = "fl_parent_id")
    private Integer flParentId;
    /**
     * 所属者id
     */
    @TableField(value = "fl_owner_id")
    private Integer flOwnerId;
    /**
     * 文件夹创建时间
     */
    @TableField(value = "fl_create_time")
    private Date flCreateTime;
    /**
     * 文件夹最后一次修改时间
     */
    @TableField(value = "fl_last_modified_time")
    private Date flLastModifiedTime;
    /**
     * 用于保存该文件夹拥有的所有文件对象
     */
    @TableField(exist = false)
    private List<File> files;
    /**
     * 用于保存该文件夹拥有的所有子级文件夹对象
     */
    @TableField(exist = false)
    private List<Folder> childrenFolders;

    /**
     * @param folderList 待处理的文件夹列表
     * @return 合并后的根文件夹对象
     */
    public Folder mergeFoldersFromList(List<Folder> folderList) {
        // 找出folderList中flParentId为1的文件夹，并将其作为根文件夹返回
        Folder result = null;
        for (Folder folder : folderList) {
            if (folder.flParentId.equals(1)) {
                result = folder;
                break;
            }
        }
        if (result == null) {
            throw new RuntimeException("合并文件夹失败，找不到用户根文件夹！");
        }
        // 使用广度优先算法，处理文件夹之间的父子级关系
        Queue<Folder> queue = new LinkedList<>();
        queue.offer(result);
        while (!queue.isEmpty()) {
            Folder parent = queue.poll();
            for (Folder folder : folderList) {
                if (folder.flParentId.equals(parent.flid)) {
                    parent.childrenFolders.add(folder);
                    queue.offer(folder);
                }
            }
        }
        return result;
    }
}
