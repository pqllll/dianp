package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 * zhn
 * 11.1
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;


    private IVoucherOrderService proxy;//定义代理对象，提前定义后面会用到

    @Override
    @Transactional
    public Result setkillVoucher(Long voucherId) {

        /**
         * 三个表
         * 查询秒杀订单
         * 若没开始，数量为0，则返回
         * 三个表一致修改
         * 防止黄牛买票--一人一单
         */
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long id = UserHolder.getUser().getId();
        //intern从字符串常量池先找
        //对整个事务加锁，保证提交事务+释放锁
        synchronized (id.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.CreateVoucher(voucherId, voucher);
        }
    }


    //一人一单2,加s锁
    @Transactional
    public Result CreateVoucher(Long voucherId, SeckillVoucher voucher) {
        Long id = UserHolder.getUser().getId();

        int count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        //一人一单1：but，同时查询该if判断的都为0，并发错误率高
        if (count > 0) {
            return Result.fail("已经买过该卷");
        }

        LambdaQueryWrapper<SeckillVoucher> queryWrapper = new LambdaQueryWrapper<>();
        //加上stock>0,乐观锁
        queryWrapper.eq(SeckillVoucher::getVoucherId, voucherId).gt(SeckillVoucher::getStock, 0);

        //确定stock-1和现在的stock（数据库）的内容一致;舍弃
        int stock = voucher.getStock();
        stock -= 1;
        voucher.setStock(stock);
        boolean suc = seckillVoucherService.update(voucher, queryWrapper);

        if (!suc) {
            return Result.fail("库存不足");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        //3 id userid voucherid
        long order = redisIdWorker.nextId("order");

        voucherOrder.setId(order);
        voucherOrder.setUserId(id);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);


        return Result.ok(voucherId);
    }
}
/*  Long id = UserHolder.getUser().getId();

        //创建锁对象，尝试获取锁，判断
        RLock redislock=redissonClient.getLock("lock:order:"+id);
        boolean islock= redislock.tryLock();
        if (!islock){
            return  Result.fail("不允许重复下单");
        }
        try {
            int count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
            //一人一单1：but，同时查询该if判断的都为0，并发错误率高
            if (count > 0) {
                return Result.fail("已经买过该卷");
            }
            LambdaQueryWrapper<SeckillVoucher> queryWrapper = new LambdaQueryWrapper<>();
            //加上stock>0,乐观锁
            queryWrapper.eq(SeckillVoucher::getVoucherId, voucherId).gt(SeckillVoucher::getStock, 0);

            //确定stock-1和现在的stock（数据库）的内容一致;舍弃
            int stock = voucher.getStock();
            stock -= 1;
            voucher.setStock(stock);
            boolean suc = seckillVoucherService.update(voucher, queryWrapper);

            if (!suc) {
                return Result.fail("库存不足");
            }
            VoucherOrder voucherOrder = new VoucherOrder();
            //3 id userid voucherid
            long order = redisIdWorker.nextId("order");

            voucherOrder.setId(order);
            voucherOrder.setUserId(id);
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);


            return Result.ok(voucherId);
        }finally {
            redislock.unlock();
        }*/
