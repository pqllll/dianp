package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhn
 * @since 2023-10-26
 */

public interface IUserService extends IService<User> {
    public Result sendCode( String phone, HttpSession session);
    public Result login( LoginFormDTO loginForm, HttpSession session);
    Result qiandao();
    Result Countqiandao();

}
