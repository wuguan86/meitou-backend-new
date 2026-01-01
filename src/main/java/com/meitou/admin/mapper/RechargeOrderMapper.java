package com.meitou.admin.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meitou.admin.entity.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 充值订单 Mapper 接口
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
    
    /**
     * 根据订单号查询订单（忽略多租户过滤）
     * 用于支付回调等无法获取上下文的场景
     * 
     * @param orderNo 订单号
     * @return 订单
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM recharge_orders WHERE order_no = #{orderNo} AND deleted = 0 LIMIT 1")
    RechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);
}

