package com.travelplatform.contenttrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.contenttrip.entity.Flight;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlightMapper extends BaseMapper<Flight> {
}
