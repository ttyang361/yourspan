package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.FileMapper;
import com.ttyang.yourspan.pojo.File;
import com.ttyang.yourspan.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service("fileServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {
    @Override
    public List<File> getAllFilesByUid(Integer uid) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("f_owner_id", uid);
        queryWrapper.eq("f_delete", false);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean uploadFileInfo(String prefixName, Integer uid, String group, String path, String folderId, Date createTime, Date modifiedTime, Boolean isPublic, Long fileSize) {
        return save(new File(null, prefixName, uid, group, path, Integer.valueOf(folderId), createTime, modifiedTime, isPublic, fileSize, false, null, null));
    }

    @Override
    public File getFileByFid(String fileId) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("f_id", Integer.valueOf(fileId));
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean modifyFileName(String fileId, String newFileName) {
        File file = getFileByFid(fileId);
        file.setFName(newFileName);
        file.setFLastModifiedTime(Date.valueOf(LocalDate.now()));
        return updateById(file);
    }

    @Override
    public String deleteFileByFid(String fileId) {
        File file = getFileByFid(fileId);
        String fullPath = file.getFGroup() + "/" + file.getFPath();
        int result = baseMapper.delete(new QueryWrapper<File>().eq("f_id", Integer.valueOf(fileId)));
        if (result == 1) {
            return fullPath;
        }
        return null;
    }

    @Override
    public boolean modifyFolderOfFile(String fileId, String targetFolderId) {
        File file = getFileByFid(fileId);
        file.setFVirtualFolder(Integer.valueOf(targetFolderId));
        file.setFLastModifiedTime(Date.valueOf(LocalDate.now()));
        return updateById(file);
    }

    @Override
    public boolean setFileAuthority(String fileId, Boolean authority) {
        File file = getFileByFid(fileId);
        file.setAuthority(authority);
        file.setFLastModifiedTime(Date.valueOf(LocalDate.now()));
        return updateById(file);
    }

    @Override
    public List<File> getPublicFiles() {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("authority", true);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean moveToRecycleBin(String fileId) {
        File file = getFileByFid(fileId);
        file.setFDelete(true);
        return updateById(file);
    }

    @Override
    public boolean restoreFromRecycleBin(String fileId) {
        File file = getFileByFid(fileId);
        file.setFDelete(false);
        return updateById(file);
    }

    @Override
    public List<File> getRecycleBinFileList(Integer uid) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("f_owner_id", uid);
        queryWrapper.eq("f_delete", true);
        return baseMapper.selectList(queryWrapper);
    }
}
