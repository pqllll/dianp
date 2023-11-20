package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isfollow}")
    public Result follow(@PathVariable("id") Long userFollowId,@PathVariable("isfollow") boolean isFollow) {
        return followService.follow(userFollowId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result Isfollow(@PathVariable("id") Long userFollowId){
        return followService.Isfollow(userFollowId);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }

}
