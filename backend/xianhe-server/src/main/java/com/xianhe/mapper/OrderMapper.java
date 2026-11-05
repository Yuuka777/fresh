package com.xianhe.mapper;

import com.xianhe.dto.GoodsSalesDTO;
import com.xianhe.dto.OrderPageQueryDTO;
import com.xianhe.entity.OrderMain;
import com.xianhe.vo.DailyStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主订单持久层
 */
@Mapper
public interface OrderMapper {

    /**
     * 新增订单，回填自增主键（紧接着要写入订单明细）
     * @param order
     */
    void insert(OrderMain order);

    /**
     * 按订单号查询
     * @param number
     * @return
     */
    OrderMain getByNumber(String number);

    /**
     * 按主键查询
     * @param id
     * @return
     */
    OrderMain getById(Long id);

    /**
     * 修改订单，动态更新（只写提交的字段）；支付与后续各类状态流转都走这一条
     * @param order
     */
    void update(OrderMain order);

    /**
     * 分页查询，配合 PageHelper 使用（调用方在此之前调用 startPage）
     * @param query
     * @return
     */
    List<OrderMain> pageQuery(OrderPageQueryDTO query);

    /**
     * 按「原状态」条件更新，返回影响行数（0 表示状态已被并发修改，本次不生效）
     * @param order
     * @param fromStatus
     * @return
     */
    int updateByIdAndStatus(@Param("order") OrderMain order, @Param("fromStatus") Integer fromStatus);

    /**
     * 批量取消超时未支付订单，返回实际取消条数
     * @param fromStatus
     * @param toStatus
     * @param deadline
     * @param reason
     * @param cancelTime
     * @return
     */
    int cancelExpired(@Param("fromStatus") Integer fromStatus,
                      @Param("toStatus") Integer toStatus,
                      @Param("deadline") LocalDateTime deadline,
                      @Param("reason") String reason,
                      @Param("cancelTime") LocalDateTime cancelTime);

    /**
     * 按状态分组统计数量：一次查回，四个角标数字来自同一快照
     * @param statuses
     * @return
     */
    List<Map<String, Object>> countGroupByStatus(@Param("statuses") List<Integer> statuses);

    /**
     * 按下单时间区间统计订单数量；区间与状态都可为空（为空即不加该条件）
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Integer countByCondition(@Param("begin") LocalDateTime begin,
                             @Param("end") LocalDateTime end,
                             @Param("status") Integer status);

    /**
     * 按下单时间区间求订单金额合计；没查到记录时返回 0
     * @param begin
     * @param end
     * @param status
     * @return
     */
    BigDecimal sumAmountByCondition(@Param("begin") LocalDateTime begin,
                                    @Param("end") LocalDateTime end,
                                    @Param("status") Integer status);

    /**
     * 按下单时间区间、按天统计订单数量（一次查回整段区间，避免逐天查库）
     * @param begin
     * @param end
     * @param status
     * @return
     */
    List<DailyStatVO> countGroupByDate(@Param("begin") LocalDateTime begin,
                                       @Param("end") LocalDateTime end,
                                       @Param("status") Integer status);

    /**
     * 按下单时间区间、按天统计营业额
     * @param begin
     * @param end
     * @param status
     * @return
     */
    List<DailyStatVO> sumAmountGroupByDate(@Param("begin") LocalDateTime begin,
                                           @Param("end") LocalDateTime end,
                                           @Param("status") Integer status);

    /**
     * 按已完成的订单聚合商品销量，取前 10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> listSalesTop10(@Param("begin") LocalDateTime begin,
                                       @Param("end") LocalDateTime end);
}
