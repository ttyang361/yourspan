package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.FileMapper;
import com.ttyang.yourspan.pojo.File;
import com.ttyang.yourspan.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("fileServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {
    @Override
    public List<File> getAllFilesByUid(Integer uid) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("f_owner_id", uid);
        return baseMapper.selectList(queryWrapper);
    }
}
