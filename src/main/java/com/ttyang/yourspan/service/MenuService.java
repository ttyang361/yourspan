package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {
    List<Menu> getAllMenus();
}
