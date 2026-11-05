package com.xianhe.service;

import com.xianhe.dto.GoodsSalesDTO;
import com.xianhe.vo.MemberReportVO;
import com.xianhe.vo.OrderReportVO;
import com.xianhe.vo.TurnoverReportVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务
 */
public interface ReportService {

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 会员统计
     * @param begin
     * @param end
     * @return
     */
    MemberReportVO getMemberStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量 TOP10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDate begin, LocalDate end);
}
