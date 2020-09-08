package com.example.sharding.server.impl;

import com.example.sharding.entity.Order;
import com.example.sharding.mapper.OrderMapper;
import com.example.sharding.server.OrderServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
public class OrderServerImpl implements OrderServer {
    @Resource
    OrderMapper orderMapper;

    @Transactional
    public long confirmOrder(int sequenceId) {
        //创建订单
        Order order = new Order();
        order.setAddressId(sequenceId);
        order.setUserId(sequenceId);
        order.setStatus("创建订单");
        try {
            orderMapper.insert(order);
        }catch (Exception e){
            log.info(e.getMessage(),e.getCause());
            throw new RuntimeException("SQLException",e.getCause());
        }
        return order.getOrderId();
    }

    public List<Order> getAllOrder() {
        try {
            List<Order> orders = orderMapper.selectAll();
            return orders;
        } catch (SQLException e) {
            log.info(e.getMessage(),e.getCause());
        }
        return null;
    }

    @Transactional
    public String deleteData(long orderId) {
        try {
            orderMapper.delete(orderId);
            return "delete data success";
        } catch (SQLException e) {
            log.info(e.getMessage(),e.getCause());
        }
        return "failure";
    }

    @Transactional
    public int updateOrder(long orderId, String status) {
        try {
            return orderMapper.update(orderId,status);
        } catch (SQLException e) {
            log.info(e.getMessage(),e.getCause());
            throw new RuntimeException("exception is happenning, tx will be rollback",e.getCause());
        }
    }
}
