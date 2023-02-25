package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.FolderMapper;
import com.ttyang.yourspan.pojo.Folder;
import com.ttyang.yourspan.service.FolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
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

    @Override
    public boolean createNewFolder(String folderName, String parentFolderId, Integer uid, Date createTime, Date modifiedTime) {
        return save(new Folder(null, folderName, Integer.valueOf(parentFolderId), uid, createTime, modifiedTime, null, null));
    }

    @Override
    public boolean modifyFolderName(String currentFolderId, String newFolderName) {
        Folder folder = baseMapper.selectOne(new QueryWrapper<Folder>().eq("fl_id", Integer.valueOf(currentFolderId)));
        folder.setFlName(newFolderName);
        folder.setFlLastModifiedTime(Date.valueOf(LocalDate.now()));
        return updateById(folder);
    }

    @Override
    public Folder getFolderByUidAndParentFolderId(Integer uid, int parentFolderId) {
        return baseMapper.selectOne(new QueryWrapper<Folder>().eq("fl_owner_id", uid).eq("fl_parent_id", parentFolderId));
    }

    @Override
    public void deleteFolderByFlid(Integer flid) {
        baseMapper.delete(new QueryWrapper<Folder>().eq("fl_id", flid));
    }
}
