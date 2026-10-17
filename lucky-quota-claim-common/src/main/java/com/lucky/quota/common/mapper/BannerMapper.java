package com.lucky.quota.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.common.model.entity.Banner;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BannerMapper extends BaseMapper<Banner> {

    int insertSelective(Banner record);

    int batchInsert(@Param("list") List<Banner> banners);

    int updateByPrimaryKeySelective(Banner record);

    Banner selectByPrimaryKey(Long id);


    List<Banner> selectAllBanners();

    int deleteByPrimaryKey(Long id);

    int deleteAllBanners();
}
