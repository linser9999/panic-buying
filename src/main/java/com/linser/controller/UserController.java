package com.linser.controller;


import com.linser.dto.Result;
import com.linser.service.IUserService;
import com.linser.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

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
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 用户申请获取验证码
     * @param phone
     * @param session
     * @return
     */
    @PostMapping("/code")
    public Result sendCode(String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }
}
