package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 1.2023.11.1
 * 2.用来拦截
 * 所有页面都拦截，若有token，则更新时间
 * 登录拦截，检查UserHol是否有userdto
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //刚刚拦截器是编写好了，但是还没有注册

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(//排除哪些不被拦截,放行
                        "/user/code",
                        "/user/login",
                        "/voucher/**",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**"

                ).order(1);
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);
    }
}

