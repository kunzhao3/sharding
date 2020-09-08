package com.example.sharding.controller;

import com.example.sharding.entity.Order;
import com.example.sharding.server.impl.OrderServerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
public class ShardingController {

    @Resource
    OrderServerImpl orderService;

    @GetMapping("/confirm_order")
    public String confirmOrder(int sequenceId){
        long id = orderService.confirmOrder(sequenceId);
        return "创建订单成功:订单ID = " + id;
    }

    @GetMapping("/order_all_list")
    public List<Order> orderHistoryList(){
        return orderService.getAllOrder();
    }

    /**
     * 删除历史订单
     * @param orderId
     * @return
     */
    @GetMapping("/delete_histroy_order")
    public String deleteHistroyOrder(long orderId){
        return orderService.deleteData(orderId);
    }

}
