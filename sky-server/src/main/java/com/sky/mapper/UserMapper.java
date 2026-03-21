package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    //小程序用户登录查询是否已注册
    @Select("select * from user where openid=#{openid}")
    User getByOpenId(String openid);


    void insert(User user1);
}
