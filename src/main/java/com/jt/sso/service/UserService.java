package com.jt.sso.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jt.common.service.RedisService;
import com.jt.common.vo.SysResult;
import com.jt.sso.mapper.UserMapper;
import com.jt.sso.pojo.User;

@Service
public class UserService {

    private static final Map<Integer, String> PARAM_TYPE = new HashMap<Integer, String>();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    private static final String TICKET = "TICKET";

    private static final Integer TICKET_TIME = 60 * 60;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        PARAM_TYPE.put(1, "username");
        PARAM_TYPE.put(2, "phone");
        PARAM_TYPE.put(3, "email");
    }

    public SysResult check(String param, Integer type) {
        // 校验类型是否合法
        if (!PARAM_TYPE.containsKey(type)) {
            return SysResult.build(201, "校验类型不合法，只能是1、2、3.");
        }

        // 从数据库查询
        Integer count = this.userMapper.check(param, PARAM_TYPE.get(type));
        if (count == 0) {
            // 数据可用
            return SysResult.ok(false);
        }

        // 返回
        return SysResult.ok(true);
    }

    public boolean register(User user) {
        // 检测用户名是否已经注册
        Integer count = this.userMapper.check(user.getUsername(), PARAM_TYPE.get(1));
        if (count > 0) {
            // 已经注册，此处注册失败
            return false;
        }

        try {
            this.userMapper.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String login(String userName, String passwd) {
        // 根据用户名从数据库中查询
        User user = this.userMapper.login(userName);
        // 比对密码是否正确
        if (null == user || !StringUtils.equals(user.getPassword(), DigestUtils.md5Hex(passwd))) {
            return null;
        }

        // 生存ticket，将用户数据保存到redis中
        String ticket = DigestUtils.md5Hex(System.currentTimeMillis() + userName + user.getId());
        try {
            this.redisService.set(TICKET + "_" + ticket, MAPPER.writeValueAsString(user), TICKET_TIME);
            return ticket;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 查询用户数据
     * @param ticket
     * @return
     */
    public String queryUserByTicket(String ticket) {
        String key = TICKET + "_" + ticket;
        String data = this.redisService.get(key);
        if (data == null) {
            return null;
        }
        // 刷新数据的生存时间
        this.redisService.expire(key, TICKET_TIME);
        return data;
    }

}
