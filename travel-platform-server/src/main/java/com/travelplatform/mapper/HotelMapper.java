package com.travelplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.entity.Hotel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {
}
