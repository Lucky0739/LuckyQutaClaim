package com.lucky.seckill.common.mapper;

import com.lucky.seckill.common.model.entity.Banner;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BannerMapper {

    int insertSelective(Banner record);

    int batchInsert(@Param("list") List<Banner> banners);

    int updateByPrimaryKeySelective(Banner record);

    Banner selectByPrimaryKey(Long id);


    List<Banner> selectAllBanners();

    int deleteByPrimaryKey(Long id);

    int deleteAllBanners();
}
