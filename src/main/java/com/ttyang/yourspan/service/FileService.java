package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.File;

import java.util.List;

public interface FileService extends IService<File> {
    List<File> getAllFilesByUid(Integer uid);
}
