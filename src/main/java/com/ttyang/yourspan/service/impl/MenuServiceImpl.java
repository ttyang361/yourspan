package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.MenuMapper;
import com.ttyang.yourspan.pojo.Menu;
import com.ttyang.yourspan.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("menuServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> getAllMenus() {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
        return baseMapper.selectList(queryWrapper);
    }
}
