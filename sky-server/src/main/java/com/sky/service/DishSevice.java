package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishSevice {
    void save(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteById(List<Long> ids);

    DishVO selectById(Long id);

    void update(DishDTO dishDTO);

    List<DishVO> list(Long id);

    void startOrStop(Integer status, Long id);
}
