package com.linser.service;

import com.linser.dto.LoginFormDTO;
import com.linser.dto.Result;
import com.linser.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
public interface IUserService extends IService<User> {

    Result sendCode(String QQ, HttpSession session);

    Result login(LoginFormDTO loginFormDTO, HttpSession session);
}
