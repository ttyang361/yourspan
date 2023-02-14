package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.Ratings;

import java.util.List;

public interface RatingsService extends IService<Ratings> {
    boolean addFileRate(Integer uid, Integer fileId, int rateInt);

    List<Ratings> getRatingsByUid(Integer uid);

    void deleteRatingsByFid(String fileId);
}
