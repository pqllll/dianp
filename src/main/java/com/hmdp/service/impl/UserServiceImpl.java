package com.hmdp.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author zhn
 * @since 2023-10-26
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送验证码
     * 放入redis里
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info(code);
        //session.setAttribute("code",code);
        return Result.ok("发送成功");
    }

    /**
     * 登录
     * 通过phone获得user
     * 放入redis中
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        String phone = loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }

        //code
        String cachcode=stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code=loginForm.getCode();
        if (cachcode == null || !cachcode.equals(code)) {
            return Result.fail("验证码错误");
        }

        //user
        User user = query().eq("phone", phone).one();

        if (user==null) {
               user= createUserWithPhone(phone);
        }

        //8.1随机生成token，作为登录令牌;  生成key
        String token = UUID.randomUUID().toString(true);
        //8.2将User对象转为HashMap存储；  生成value
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString())  );


        //8.3存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);//用定义的常量拼接key值
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,30,TimeUnit.MINUTES);//设置有效时间
        //返回token
        return Result.ok(token);

    }

    @Override
    public Result qiandao() {
        //获取登录用户
        Long userId = UserHolder.getUser().getId();
        //获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = "sign:"+ userId + format;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //写入redis select key offset 1,2023.11.5,5-1=4,1000=8(0x8)
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth -1, true);//true代表 1为签到，0为未签到
        return Result.ok();
    }

    /**
     * 统计签到次数
     * @return
     */
    @Override
    public Result Countqiandao() {
        //获取登录用户
        Long userId = UserHolder.getUser().getId();
        //获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = "sign:"+ userId + format;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //获取本月截至今天为止的所有签到记录，返回的是一个十进制的数字 BITFIELD sign：5：202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()){
            //没有任何签到结果
            return  Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0){
            return Result.ok(0);
        }
        //循环遍历
        int count = 0;
        while (true){
            //让这个数字与1做与运算，得到数字的最后一个bit位   //判读这个bit位是否为0
            if ((num & 1) == 0){
                //如果为0，说明未签到，结束
                break;
            }else {
                //如果不为0，说明已签到，计数器加1
                count++;
            }
            //把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }


    private User createUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10) );
        save(user);
        return user;
    }


}
