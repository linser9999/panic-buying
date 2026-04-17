package com.linser.controller;


import com.linser.dto.LoginFormDTO;
import com.linser.dto.Result;
import com.linser.dto.UserDTO;
import com.linser.entity.User;
import com.linser.service.IUserService;
import com.linser.service.impl.UserServiceImpl;
import com.linser.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.linser.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户申请获取验证码
     * @param phone
     * @param session
     * @return
     */
    @PostMapping("/code")
    public Result sendCode(String phone, HttpSession session) {
        log.debug("用户申请获取验证码:" + phone);
        return userService.sendCode(phone, session);
    }

    /**
     * 用户登录
     * @param loginFormDTO
     * @param session
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginFormDTO, HttpSession session) {
        log.debug("用户：" + loginFormDTO.getPhone() + "登录");
        return userService.login(loginFormDTO, session);
    }

    /**
     * 获取当前登录的用户并返回
     * @return
     */
    @GetMapping("/me")
    public Result me() {
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 用户退出登录
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        // 获取 token
        String token = request.getHeader("authorization");
        // 移除 redis 中的用户信息
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok();
    }
}
