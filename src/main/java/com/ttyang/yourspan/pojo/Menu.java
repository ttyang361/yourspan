package com.ttyang.yourspan.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@TableName("menu")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Menu implements Serializable {
    /**
     * 菜单组件id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 组件url
     */
    private String url;
    /**
     * 组件路由路径
     */
    private String path;
    /**
     * 组件名
     */
    private String component;
    /**
     * 组件称谓
     */
    private String name;
    /**
     * 组件图标
     */
    @TableField(value = "iconCls")
    private String iconCls;
    /**
     * ...
     */
    @TableField(value = "keepAlive")
    private Boolean keepAlive;
    /**
     * 组件是否需要认证
     */
    @TableField(value = "requireAuth")
    private Boolean requireAuth;
    /**
     * 父组件id
     */
    @TableField(value = "parentId")
    private Integer parentId;
    /**
     * 组件是否激活
     */
    private Boolean enabled;
    /**
     * 子组件对象集合
     */
    @TableField(exist = false)
    private List<Menu> children;
}
