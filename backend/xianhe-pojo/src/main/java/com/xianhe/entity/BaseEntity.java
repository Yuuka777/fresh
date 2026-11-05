package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体公共字段基类
 */
@Data
public class BaseEntity implements Serializable {

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;

    //创建人 id
    private Long createUser;

    //修改人 id
    private Long updateUser;
}
