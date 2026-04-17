package com.linser.service.impl;

import com.linser.entity.Blog;
import com.linser.mapper.BlogMapper;
import com.linser.service.IBlogService;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
