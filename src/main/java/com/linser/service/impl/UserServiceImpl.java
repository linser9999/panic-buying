package com.linser.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.linser.dto.LoginFormDTO;
import com.linser.dto.Result;
import com.linser.entity.User;
import com.linser.mapper.UserMapper;
import com.linser.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linser.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.linser.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private QQCodeUtil qqCodeUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户申请获取验证码
     * @param QQ
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String QQ, HttpSession session) {
        // 校验 QQ号 合法性
        if (RegexUtils.isQQInvalid(QQ)) {
            // QQ号格式错误
            return Result.fail(ErrorContants.QQ_ERROR);
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码: 基于session -> 后期改为 redis
        // session.setAttribute("code", code);
        stringRedisTemplate.opsForValue()
                .set(LOGIN_CODE_KEY + QQ, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);


        // 发送验证码
        qqCodeUtil.sendCode(QQ, code);
        log.debug("生成的验证码是：" + code);

        // 返回成功
        return Result.ok();
    }

    /**
     * 用户登录
     * @param loginFormDTO
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginFormDTO, HttpSession session) {
        String qq = loginFormDTO.getPhone();
        //校验 qq 合法性
        if (RegexUtils.isQQInvalid(qq)) {
            // QQ号格式错误
            return Result.fail(ErrorContants.QQ_ERROR);
        }

        // 校验验证码
        String code = loginFormDTO.getCode();
        // String cacheCode = (String) session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + qq);

        if (!StrUtil.equals(cacheCode, code)) {
            return Result.fail(ErrorContants.CODE_ERROR);
        }

        // 获取用户信息
        User user = query().eq("phone", qq).one();
        if (user == null) {
            // 用户不存在
            user = createUserWithQQ(qq);
        }

        // 将用户信息保存到 session
        // session.setAttribute("user", user);
        // 将用户信息保存到 redis
        String token = String.valueOf(UUID.randomUUID(true));
        stringRedisTemplate.opsForValue()
                .set(LOGIN_USER_KEY + token, JSONUtil.toJsonStr(user), LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithQQ(String qq) {
        User user = User.builder()
                // .icon(SystemConstants.DEFAULT_ICON)
                .phone(qq)
                .nickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(16))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存
        save(user);
        return user;
    }
}
