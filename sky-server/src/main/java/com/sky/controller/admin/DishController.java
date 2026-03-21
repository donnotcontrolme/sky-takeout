package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishSevice;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RequestMapping("/admin/dish")
@RestController
public class DishController {

    @Autowired
    private DishSevice dishSevice;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishSevice.save(dishDTO);
        String key="dish_"+dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();

    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询：{}",dishPageQueryDTO);
        PageResult pageResult= dishSevice.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("要删除的菜品id：{}",ids);
        dishSevice.deleteById(ids);
        cleanCache("dish_*");
        return Result.success();
    }

    @ApiOperation("根据id查询菜品，数据回显")
    @GetMapping("/{id}")
    public Result<DishVO> selectById(@PathVariable Long id){
        log.info("查询菜品id: {}",id);
        DishVO dishVO= dishSevice.selectById(id);
        return Result.success(dishVO);
    }

    @ApiOperation("修改菜品")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO){
        dishSevice.update(dishDTO);
        cleanCache("dish_*");
        return Result.success();
    }
    @ApiOperation("起售停售菜品")
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("id{}菜品启停售{}",id,status);
        dishSevice.startOrStop(status,id);
        cleanCache("dish_*");
        return Result.success();
    }

    @ApiOperation("根据分类id查询菜品")
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类id查询菜品：{}",categoryId);
         List<Dish> dishList = dishSevice.list(categoryId);
        return Result.success(dishList);
    }

    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
