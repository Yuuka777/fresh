package com.xianhe.service;

import com.xianhe.vo.BundleOverViewVO;
import com.xianhe.vo.BusinessDataVO;
import com.xianhe.vo.GoodsOverViewVO;
import com.xianhe.vo.OrderOverViewVO;

import java.time.LocalDateTime;

/**
 * 工作台服务
 */
public interface WorkspaceService {

    /**
     * 按时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 订单总览
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 商品总览
     * @return
     */
    GoodsOverViewVO getGoodsOverView();

    /**
     * 组合箱总览
     * @return
     */
    BundleOverViewVO getBundleOverView();
}
