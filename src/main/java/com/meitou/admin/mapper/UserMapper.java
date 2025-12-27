package com.meitou.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meitou.admin.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * 继承 MyBatis Plus 的 BaseMapper，提供基础 CRUD 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

