package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *zhn
 * 11.1
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopById(Long id) {
        //1.redis
        String shopcach = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //blank>empty,redis存储的是string，数据库获得的直接是json数据
        if (StrUtil.isNotBlank(shopcach)){
            //Json化
            Shop shop = JSONUtil.toBean(shopcach, Shop.class);
            return Result.ok(shop);
        }
        //empty "",防止缓存穿透，在redis拒绝,不消耗数据库
        if (shopcach!=null){
            return Result.fail("商家店铺信息不存在");
        }

        //没有redis则从数据库获取，放入redis
        Shop shop=getById(id);

        if (shop==null){
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, "");
            stringRedisTemplate.expire(CACHE_SHOP_KEY+id,30, TimeUnit.MINUTES);

            return  Result.fail("没有该店铺");
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop));
        stringRedisTemplate.expire(CACHE_SHOP_KEY+id,30, TimeUnit.MINUTES);



        return Result.ok(shop);
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {

        if (true){
            Page<Shop> page=query().eq("type_id",typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //这里需要redis6.0
        int from=(current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;
        String key=SHOP_GEO_KEY+typeId;

        GeoResults<RedisGeoCommands.GeoLocation<String>> search = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeCoordinates().limit(end));
        if (search==null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = search.getContent();
        List<Long> ids=new ArrayList<>(content.size());
        Map<String,Distance> distanceMap=new HashMap<>(content.size());

        content.stream().skip(from).forEach(result->{
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            Distance distance = result.getDistance();
            distanceMap.put(shopId,distance);

        });
        String idStr = StrUtil.join("," + ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop:shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }
}
