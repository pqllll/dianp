package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate=stringRedisTemplate;
    }

    /**
     * 11.1
     * 针对所有页面
     * 有token就将user放入UserHold中
     * 为保证信息安全，封装为userdto
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");//前端页面定义的请求头
        if (StrUtil.isBlank(token)){

            return true;
        }
        //2.基于token获取redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().
                entries(RedisConstants.LOGIN_USER_KEY + token);
        //3.判读用户是否存在
        if (userMap.isEmpty()){
            //4.不存在拦截到登录页面,返回401状态码，未授权
            return true;
        }
        //5.将查询到的Hash数据转为UserDTO对象
        //将map中的数据自动填充到对象中，false是忽视错误，有异常直接往外抛就行了
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6.存在，保存用户信息到TreadLocal中
        UserHolder.saveUser(userDTO);
        //7.刷新token的有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,30, TimeUnit.MINUTES);
        //8.放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
