package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.StatusModifyNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishSevice;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Properties;

@Service
public class DishSeviceImpl implements DishSevice {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private Properties pageHelperProperties;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null&&flavors.size()!=0){
            flavors.forEach(flavor->
                    flavor.setDishId(dishId));

            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO>dishVOS= dishMapper.pageQuery(dishPageQueryDTO);
        long total = dishVOS.getTotal();
        List<DishVO> result = dishVOS.getResult();

        return new PageResult(total,result);
    }

    @Override
    @Transactional
    public void deleteById(List<Long> ids) {
        //查询是否起售
        for (Long id : ids) {
            Dish dish= dishMapper.getById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //查询是否在套餐中
        List<Long>setmealIds= setmealDishMapper.selectByid(ids);
        if(setmealIds!=null && setmealIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

       /* for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }*/

        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 回显菜品
     * @param id
     * @return
     */
    @Override
    @Transactional
    public DishVO selectById(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish= dishMapper.selectById(id);
        BeanUtils.copyProperties(dish,dishVO);
        List<DishFlavor> dishFlavors=dishFlavorMapper.selectById(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品：dish表和dish_flavors表
     * @param dishDTO
     */
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        Long dishId = dish.getId();

        /**
         * 可以直接调用已经写好的insertBatch，只要在此之前删除口味数据就行
         */
        //删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishId);
        //新增口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null && flavors.size()!=0){
            flavors.forEach(flavor->{
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .build();
        List<DishVO> dishVOList = dishMapper.list(dish);

        return dishVOList;

    }

    /**
     * 起售停售菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //查询是否在起售套餐中
        if(status==StatusConstant.DISABLE){
            List<Setmeal> setmeals = setmealMapper.getByDishId(id);
            setmeals.forEach(setmeal -> {
                if(setmeal.getStatus()==StatusConstant.ENABLE){
                    throw new StatusModifyNotAllowedException(MessageConstant.DISH_ON_STEMEAL_SALE);
                }
            });
        }
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
}
