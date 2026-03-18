package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

   @AutoFill(value = OperationType.INSERT)
    void add(Setmeal setmeal);


    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    Setmeal getById(Long id);

    void deletByIds(List<Long> ids);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);

    @Select("select s.* from setmeal s left join setmeal_dish d on s.id = d.setmeal_id where d.dish_id=#{dishId}")
    List<Setmeal> getByDishId(Long dishId);
}
