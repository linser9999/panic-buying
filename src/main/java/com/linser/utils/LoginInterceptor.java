package com.linser.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.linser.dto.UserDTO;
import com.linser.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.concurrent.TimeUnit;

import static com.linser.utils.RedisConstants.LOGIN_USER_KEY;

public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // // 获取 session
        // HttpSession session = request.getSession();
        // // 获取 用户信息
        // User user = (User) session.getAttribute("user");

        // 获取 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }

        // 从 redis 中获取 用户信息
        String userJson = stringRedisTemplate.opsForValue().get(LOGIN_USER_KEY + token);

        if (userJson.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        User user = JSONUtil.toBean(userJson, User.class);

        // 保存用户信息
        UserHolder.saveUser(BeanUtil.copyProperties(user, UserDTO.class));
        // 刷新 token 时长
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remove();
    }
}
