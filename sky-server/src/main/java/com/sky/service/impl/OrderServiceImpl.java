package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.*;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.LoginException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO sub(OrdersSubmitDTO ordersSubmitDTO) {

        Long userId = BaseContext.getCurrentId();
        //判断是否有地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook.getDetail()==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //TODO:
        //记得打开
        /*//判断是否超出配送距离
        String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
        checkOutOfRange(address);*/
        //判断是否购物车为空
        ShoppingCart cartList= new ShoppingCart();
        cartList.setUserId(userId);
        List<ShoppingCart> getlist = shoppingCartMapper.getlist(cartList);
        if(getlist==null|| getlist.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setUserId(userId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setAddress(addressBook.getDetail());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPhone(addressBook.getPhone());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setConsignee(addressBook.getConsignee());
        //插入订单数据
        orderMapper.insert(orders);


        //插入订单详情表
        List<OrderDetail>orderDetailList=new ArrayList<>();
        getlist.forEach(shoppingCart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        //批量插入
        orderDetailMapper.inertBatch(orderDetailList);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();

        //清空购物车
        shoppingCartMapper.deleteAll(userId);

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        /*// 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));*/
        log.info("跳过微信支付，支付成功");
        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();

        orderMapper.update(orders);

        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",ordersDB.getId());
        map.put("type",1);
        map.put("content","订单号："+outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }


    /**
     * 用户历史订单分页查询
     *
     * @param
     * @return
     */
    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setStatus(ordersPageQueryDTO.getStatus());
        ordersPageQueryDTO.setUserId(userId);
        Page<Orders>lists= orderMapper.getPage(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        lists.forEach(list->{
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(list.getId());
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(list,orderVO);
            orderVO.setOrderDetailList(orderDetails);
            orderVOList.add(orderVO);
        });
        return new PageResult(lists.getTotal(),orderVOList);



    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id){
        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;

    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Override
    public void cancel(Long id) {

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        Long currentId = BaseContext.getCurrentId();
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> shoppingCarts = detailList.stream().map(od -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(od, cart);
            cart.setUserId(currentId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 商家条件搜索订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        ordersPageQueryDTO.setStatus(ordersPageQueryDTO.getStatus());
        Page<Orders>lists= orderMapper.getPage(ordersPageQueryDTO);

        if (lists.isEmpty()){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderVO> orderVOList = new ArrayList<>();
        lists.forEach(list->{
            String orderDetailStr = getOrderDetailStr(list);
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(list,orderVO);
            orderVO.setOrderDishes(orderDetailStr);
            orderVOList.add(orderVO);
        });
        return new PageResult(lists.getTotal(),orderVOList);
    }


    /**
     * orderDetail转String
     * @param orders
     * @return
     */
    public String getOrderDetailStr(Orders orders){
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> detailStr = detailList.stream().map(orderDetail -> {
            return orderDetail.getName() + "*" + orderDetail.getNumber() + "；";
        }).collect(Collectors.toList());
        return String.join(" ", detailStr);

    }

    /**
     * 订单统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED)); orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        orderStatisticsVO.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancelByAdm(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单派送
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 检查地址是否超出配送范围
     * @param address
     */
    public void checkOutOfRange(String address){
        HashMap<String, String> map = new HashMap<>();
        // 获取店铺经纬度
        map.put("address",shopAddress);
        map.put("ak",ak);
        map.put("output","json");
        String doGet = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(doGet);
        if (!jsonObject.getString("status").equals("0")){
            throw new AdmLocationException(MessageConstant.ADDRESS_NOT_FOUND_ADMIN);
        }
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lng = location.get("lng").toString();
        String lat = location.get("lat").toString();
        String shopAddress = lat+","+lng;

        //获取用户经纬度
        map.put("address",address);
        String doGet2 = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject2 = JSON.parseObject(doGet2);
        if (!jsonObject2.getString("status").equals("0")) {
            throw new UserNotLoginException(MessageConstant.ADDRESS_NOT_FOUND_USER);
        }
        JSONObject userLocation = jsonObject2.getJSONObject("result").getJSONObject("location");
        String userLat = userLocation.get("lat").toString();
        String userLng = userLocation.get("lng").toString();
        String userAddress=userLat+','+userLng;

        map.put("origin",shopAddress);
        map.put("destination",userAddress);
        map.put("step_info","0");
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/direction/v2/driving", map);
        JSONObject route = JSON.parseObject(json);
        if(!route.getString("status").equals("0")){
            throw new DistanceException(MessageConstant.DISTANCE_NOT_FOUND);
        }
        JSONObject result = route.getJSONObject("result");
        JSONArray jsonArray = result.getJSONArray("routes");
        Integer distance = jsonArray.getJSONObject(0).getInteger("distance");
        if(distance > 5000){
            throw new DistanceException(MessageConstant.DISTANCE_TOO_LONG);
        }


    }


}
