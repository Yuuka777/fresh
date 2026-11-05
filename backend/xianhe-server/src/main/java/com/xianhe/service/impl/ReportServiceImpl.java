package com.xianhe.service.impl;

import com.xianhe.constant.OrderStatusConstant;
import com.xianhe.dto.GoodsSalesDTO;
import com.xianhe.mapper.MemberMapper;
import com.xianhe.mapper.OrderMapper;
import com.xianhe.service.ReportService;
import com.xianhe.vo.DailyStatVO;
import com.xianhe.vo.MemberReportVO;
import com.xianhe.vo.OrderReportVO;
import com.xianhe.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表服务实现
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    //不传时间区间时的默认跨度：最近 30 天（含今天）。
    //与报表页「统计区间」卡片上的说明一致；管理端工作台的近 7 日营业额趋势取这条序列的最后 7 天
    private static final int DEFAULT_DAYS = 30;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MemberMapper memberMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        LocalDate[] range = normalizeRange(begin, end);
        List<LocalDate> dates = dateListOf(range[0], range[1]);

        //整段区间一次按天聚合查回：逐天查库在 30 天区间下就是 30 条 SQL
        Map<String, BigDecimal> amountByDate = new HashMap<>();
        List<DailyStatVO> stats = orderMapper.sumAmountGroupByDate(
                startOfDay(range[0]), endOfDay(range[1]), OrderStatusConstant.COMPLETED);
        for (DailyStatVO stat : stats) {
            amountByDate.put(stat.getStatDate(), stat.getAmount());
        }

        //区间里没有订单的日期补 0，保证两个序列长度一致、下标能对上
        List<String> amounts = new ArrayList<>();
        for (LocalDate date : dates) {
            BigDecimal amount = amountByDate.get(date.toString());
            amounts.add(amount == null ? "0" : amount.toPlainString());
        }

        TurnoverReportVO vo = new TurnoverReportVO();
        vo.setDateList(join(dates));
        vo.setTurnoverList(String.join(",", amounts));
        return vo;
    }

    /**
     * 会员统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public MemberReportVO getMemberStatistics(LocalDate begin, LocalDate end) {
        LocalDate[] range = normalizeRange(begin, end);
        List<LocalDate> dates = dateListOf(range[0], range[1]);

        Map<String, Integer> newByDate = new HashMap<>();
        List<DailyStatVO> stats = memberMapper.countGroupByDate(startOfDay(range[0]), endOfDay(range[1]));
        for (DailyStatVO stat : stats) {
            newByDate.put(stat.getStatDate(), stat.getCount());
        }

        //累计会员数＝区间开始前已有的会员数 + 区间内逐日累加。
        //这样只需 2 条 SQL（一条查期初、一条查每日新增），不必每到一天就整表数一次
        int running = memberMapper.countByCreateTimeBetween(null, endOfDay(range[0].minusDays(1)));

        List<String> newCounts = new ArrayList<>();
        List<String> totalCounts = new ArrayList<>();
        for (LocalDate date : dates) {
            int added = newByDate.getOrDefault(date.toString(), 0);
            running += added;
            newCounts.add(String.valueOf(added));
            totalCounts.add(String.valueOf(running));
        }

        MemberReportVO vo = new MemberReportVO();
        vo.setDateList(join(dates));
        vo.setNewMemberList(String.join(",", newCounts));
        vo.setTotalMemberList(String.join(",", totalCounts));
        return vo;
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        LocalDate[] range = normalizeRange(begin, end);
        List<LocalDate> dates = dateListOf(range[0], range[1]);

        //两个序列各一次聚合查询：订单总数不带状态，有效订单只算「已完成」
        Map<String, Integer> totalByDate = countMapOf(
                orderMapper.countGroupByDate(startOfDay(range[0]), endOfDay(range[1]), null));
        Map<String, Integer> validByDate = countMapOf(
                orderMapper.countGroupByDate(startOfDay(range[0]), endOfDay(range[1]), OrderStatusConstant.COMPLETED));

        List<String> orderCounts = new ArrayList<>();
        List<String> validCounts = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;
        for (LocalDate date : dates) {
            int total = totalByDate.getOrDefault(date.toString(), 0);
            int valid = validByDate.getOrDefault(date.toString(), 0);
            totalOrderCount += total;
            validOrderCount += valid;
            orderCounts.add(String.valueOf(total));
            validCounts.add(String.valueOf(valid));
        }

        OrderReportVO vo = new OrderReportVO();
        vo.setDateList(join(dates));
        vo.setOrderCountList(String.join(",", orderCounts));
        vo.setValidOrderCountList(String.join(",", validCounts));
        vo.setTotalOrderCount(totalOrderCount);
        vo.setValidOrderCount(validOrderCount);
        vo.setOrderCompletionRate(totalOrderCount == 0
                ? 0.0
                : BigDecimal.valueOf(validOrderCount)
                        .divide(BigDecimal.valueOf(totalOrderCount), 4, RoundingMode.HALF_UP)
                        .doubleValue());
        return vo;
    }

    /**
     * 销量 TOP10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public List<GoodsSalesDTO> getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDate[] range = normalizeRange(begin, end);
        return orderMapper.listSalesTop10(startOfDay(range[0]), endOfDay(range[1]));
    }

    /**
     * 时间区间补缺省值：都不传＝最近 7 天（含今天）；缺一端时，begin 按 end 往前推、end 取今天
     * @param begin
     * @param end
     * @return
     */
    private LocalDate[] normalizeRange(LocalDate begin, LocalDate end) {
        LocalDate realEnd = end == null ? LocalDate.now() : end;
        LocalDate realBegin = begin == null ? realEnd.minusDays(DEFAULT_DAYS - 1L) : begin;
        return new LocalDate[]{realBegin, realEnd};
    }

    /**
     * 生成日期序列；begin 晚于 end 时返回空序列（不报错，前端拿到空数组自然画空图）
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> dateListOf(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    /**
     * 按天聚合结果转成「日期 → 数量」的映射，便于按日期序列对齐
     * @param stats
     * @return
     */
    private Map<String, Integer> countMapOf(List<DailyStatVO> stats) {
        Map<String, Integer> map = new HashMap<>();
        for (DailyStatVO stat : stats) {
            map.put(stat.getStatDate(), stat.getCount());
        }
        return map;
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    private String join(List<LocalDate> dates) {
        return dates.stream().map(LocalDate::toString).collect(Collectors.joining(","));
    }
}
