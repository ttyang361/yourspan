package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.FolderMapper;
import com.ttyang.yourspan.pojo.Folder;
import com.ttyang.yourspan.service.FolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("folderServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder> implements FolderService {
    @Override
    public List<Folder> getAllFoldersByUid(Integer uid) {
        QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fl_owner_id", uid);
        return baseMapper.selectList(queryWrapper);
    }
}
