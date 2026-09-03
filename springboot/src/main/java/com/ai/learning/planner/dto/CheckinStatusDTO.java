package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * 签到状态 DTO
 * 返回签到状态、连续天数、积分信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinStatusDTO {
    /** 今日是否已签到 */
    private Boolean todayChecked;
    /** 当前连续签到天数 */
    private Integer continuousDays;
    /** 总签到天数 */
    private Integer totalDays;
    /** 当前可用积分 */
    private Long availablePoints;
    /** 今日签到可获得积分 */
    private Long todayPoints;
    /** 签到日期 */
    private LocalDate checkinDate;
    /** 当月已签到日期列表 */
    private List<LocalDate> monthDays;
}
