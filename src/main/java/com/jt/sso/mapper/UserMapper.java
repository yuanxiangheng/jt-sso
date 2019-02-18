package com.jt.sso.mapper;

import org.apache.ibatis.annotations.Param;

import com.jt.sso.pojo.User;

public interface UserMapper {

    /**
     * 校验数据是否可用
     * 
     * @param param
     * @param string
     * @return
     */
    Integer check(@Param("param") String param, @Param("paramType") String paramType);

    /**
     * 新增数据
     * @param user
     */
    void save(User user);

    /**
     * 根据用户名登录
     * @param userName
     * @return
     */
    User login(String userName);

}
