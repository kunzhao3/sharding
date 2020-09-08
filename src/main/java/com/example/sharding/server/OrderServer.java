package com.example.sharding.server;


import com.example.sharding.entity.Order;

import java.util.List;

public interface OrderServer {
    public long confirmOrder(int sequenceId);

    public List<Order> getAllOrder();

    public String deleteData(long orderId);

    public int updateOrder(long orderId,String status);
}
