package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<Long> selectByid(List<Long> dishIds);

    @AutoFill(value = OperationType.INSERT)
    void insertBatch(List<SetmealDish> setmealDishes);

    //根据套餐id查询菜品
    @Select("select * from setmeal_dish where setmeal_id =#{setmealId}")
    List<SetmealDish> selectBySetmealid(Long setmealId);

    void delete(Long setmealId);
}
