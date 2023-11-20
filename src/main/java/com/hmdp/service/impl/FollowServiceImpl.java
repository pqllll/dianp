package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_USER_ID;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override //关注取关功能
    public Result follow(Long userFollowId, boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key=FOLLOW_USER_ID+userId;
        if (isFollow){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(userFollowId);
            boolean save = save(follow);
            if (save){
                stringRedisTemplate.opsForSet().add(key,userFollowId.toString());
            }
        }else {//取关，删除  delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", userFollowId));
            if (remove){
                stringRedisTemplate.opsForSet().remove(key,userFollowId.toString());
            }
        }
        return Result.ok();
    }


    @Override
    public Result Isfollow(Long userFollowId) {
        Long userId = UserHolder.getUser().getId();
        //查询是否关注  select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", userFollowId).count();
        return Result.ok(count>0);
    }

    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key1 = FOLLOW_USER_ID + userId;  //当前登录用户的关注列表集合
        String key2 = FOLLOW_USER_ID + id;  //点击查看的用户的关注列表集合
        //求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()){
            //无交集
            return Result.ok(Collections.emptyList());
        }
        //解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //批量查询用户并转换为userDTO对象
        List<UserDTO> userDTOList = userService.listByIds(ids).stream().map(user ->
                BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }


}
