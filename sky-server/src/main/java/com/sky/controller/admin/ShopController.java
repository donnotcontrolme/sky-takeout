package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("AdminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(value = "店铺营业接口")
public class ShopController {

    public static final String KEY="STOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("设置店铺状态")
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态为{}",status==1?"营业":"打烊");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    @ApiOperation("查看店铺状态")
    @GetMapping("status")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("店铺状态为{}",status==1?"营业中":"打烊中");
        return Result.success(status);
    }


}
