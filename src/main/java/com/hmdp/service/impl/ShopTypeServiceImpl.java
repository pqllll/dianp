package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *11.1
 * zhn
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private IShopTypeService shopTypeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {

        //redis

        String shopcache = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);

        if (StrUtil.isNotBlank(shopcache)){
            //string->list<shoptype>

            List<ShopType> shopTypeList= JSONUtil.toList(shopcache,ShopType.class);

            return Result.ok(shopTypeList);
        }



        //放进redis,从数据库获取数据
                List<ShopType> typeList = shopTypeService
                .query().orderByAsc("sort").list();
        if (typeList==null) {
            //一般数据库不会为空
            return Result.fail("分类不存在");
        }

        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY,JSONUtil.toJsonStr(typeList));
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY,30, TimeUnit.MINUTES);
        return Result.ok(typeList);

    }
}
