package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.Folder;

import java.util.List;

public interface FolderService extends IService<Folder> {
    List<Folder> getAllFoldersByUid(Integer uid);
}
