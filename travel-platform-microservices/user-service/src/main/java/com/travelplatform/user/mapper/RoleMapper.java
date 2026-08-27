package com.travelplatform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.user.entity.Role;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
