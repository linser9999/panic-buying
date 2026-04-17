package com.linser.service.impl;

import com.linser.entity.BlogComments;
import com.linser.mapper.BlogCommentsMapper;
import com.linser.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
