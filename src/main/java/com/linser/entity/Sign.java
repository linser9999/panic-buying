package com.linser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.Year;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Sign implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 签到的年
     */
    private Year year;

    /**
     * 签到的月
     */
    private Integer month;

    /**
     * 签到的日期
     */
    private LocalDate date;

    /**
     * 是否补签
     */
    private Integer isBackup;


}
