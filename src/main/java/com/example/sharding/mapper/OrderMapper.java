package com.example.sharding.mapper;

import com.example.sharding.entity.Order;
import org.apache.ibatis.annotations.*;

import java.sql.SQLException;
import java.util.List;

@Mapper
public interface OrderMapper {
    @Options(useGeneratedKeys = true,keyProperty = "orderId",keyColumn = "order_id")
    @Insert("INSERT INTO t_order (user_id, address_id, status) VALUES (#{userId,jdbcType=INTEGER}," +
            " #{addressId,jdbcType=BIGINT}, #{status,jdbcType=VARCHAR})")
    long insert(Order order) throws SQLException;

    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    void delete(long orderId) throws SQLException;

    @Select("SELECT order_id orderId,user_id userId,address_id addressId,status FROM t_order")
    List<Order> selectAll() throws SQLException;

    @Select("SELECT * FROM t_order WHERE order_id BETWEEN #{start} AND #{end}")
    List<Order> selectRange(@Param("start") long start,@Param("end") long end) throws SQLException;

    @Select("SELECT * FROM t_order order by order_id limit #{offset},#{size}")
    List<Order> selectPageList(@Param("offset") long offset,@Param("size") long size) throws SQLException;

    @Update("update t_order set status = #{status} where order_id = #{orderId}")
    int update(@Param("orderId") long orderId, @Param("status") String status) throws SQLException;
}
