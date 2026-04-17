package com.linser.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class QQCodeUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private JavaMailSender sender; // 引入Spring Mail依赖后，会自动装配到IOC容器。用来发送邮件

     public void sendCode(String qq, String code) {
         if (StrUtil.isBlank(qq)) {
             throw new RuntimeException("邮箱地址不能为空");
         }

         String email = qq.contains("@") ? qq : qq + "@qq.com";

         if (RegexUtils.isEmailInvalid(email)) {
             throw new RuntimeException("邮箱地址格式不正确: " + email);
         }

         SimpleMailMessage message = new SimpleMailMessage();
         message.setSubject("【邮信验证码】验证消息");
         message.setText("验证码："+ code + "，切勿将验证码泄露给他人，本条验证码有效期为2分钟。");
         message.setTo(email);
         message.setFrom("1811535824@qq.com");

         sender.send(message);
    }
}
