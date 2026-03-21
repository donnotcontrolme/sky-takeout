package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 动态查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> getlist(ShoppingCart shoppingCart);

    /**
     * 修改物品数量
     * @param cart
     */
    @Update("update shopping_cart set number=#{number},amount=#{amount} where id=#{id}")
    void updateNumberById(ShoppingCart cart);

    void insert(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id=#{userId}")
    void deleteAll(Long userId);


    void delete(ShoppingCart shoppingCart);
}
