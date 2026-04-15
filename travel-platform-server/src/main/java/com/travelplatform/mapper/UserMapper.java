package com.travelplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
