package com.xianhe.mapper;

import com.xianhe.entity.Member;
import com.xianhe.vo.DailyStatVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员持久层（表 {@code member}）
 */
public interface MemberMapper {

    /**
     * 按 openid 查会员
     * @param openid
     * @return
     */
    Member getByOpenid(String openid);

    /**
     * 按主键查会员
     * @param id
     * @return
     */
    Member getById(Long id);

    /**
     * 新增会员，回填自增主键
     * @param member
     */
    void insert(Member member);

    /**
     * 按注册时间区间统计会员数；只传一端即只加一端条件
     * @param begin
     * @param end
     * @return
     */
    Integer countByCreateTimeBetween(@Param("begin") LocalDateTime begin,
                                     @Param("end") LocalDateTime end);

    /**
     * 按注册时间区间、按天统计新增会员数（一次查回整段区间）
     * @param begin
     * @param end
     * @return
     */
    List<DailyStatVO> countGroupByDate(@Param("begin") LocalDateTime begin,
                                       @Param("end") LocalDateTime end);
}
