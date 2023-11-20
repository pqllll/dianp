package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IFollowService extends IService<Follow> {

    public Result follow( Long userFollowId, boolean isFollow);

    Result Isfollow(Long userFollowId);

    Result followCommons(Long id);
}
