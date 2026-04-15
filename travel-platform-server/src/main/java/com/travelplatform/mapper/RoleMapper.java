package com.travelplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            SELECT r.*
            FROM `role` r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1
            """)
    List<Role> selectRolesByUserId(Long userId);
}
