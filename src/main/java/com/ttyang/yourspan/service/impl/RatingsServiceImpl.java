package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.RatingsMapper;
import com.ttyang.yourspan.pojo.Ratings;
import com.ttyang.yourspan.service.RatingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("ratingsServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class RatingsServiceImpl extends ServiceImpl<RatingsMapper, Ratings> implements RatingsService {
    @Override
    public boolean addFileRate(Integer uid, Integer fileId, int rateInt) {
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        queryWrapper.eq("f_id", fileId);

        Ratings ratings = new Ratings();
        ratings.setPreference(rateInt);
        int updateResult = baseMapper.update(ratings, queryWrapper);

        if (updateResult == 0) {
            ratings.setUid(uid);
            ratings.setFid(fileId);
            return save(ratings);
        } else {
            return true;
        }
    }

    @Override
    public List<Ratings> getRatingsByUid(Integer uid) {
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void deleteRatingsByFid(String fileId) {
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("f_id", Integer.valueOf(fileId));
        baseMapper.delete(queryWrapper);
    }
}
