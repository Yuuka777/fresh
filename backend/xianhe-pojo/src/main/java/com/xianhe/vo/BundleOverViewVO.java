package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 组合箱总览（《接口文档》2.5.4）
 */
@Data
public class BundleOverViewVO implements Serializable {

    //已起售数量（status = 1）
    private Integer sold;

    //已停售数量（status = 0）
    private Integer discontinued;
}
