package com.travelplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.entity.Flight;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlightMapper extends BaseMapper<Flight> {
}
