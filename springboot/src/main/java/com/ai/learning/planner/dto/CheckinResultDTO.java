package com.ai.learning.planner.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 打卡结果 DTO
 * 返回当日打卡状态、连续/总打卡天数与当月打卡日期集合
 */
@Data
public class CheckinResultDTO {
    private boolean todayChecked;
    private int continuousDays;
    private int totalDays;
    private String checkinDate;
    private List<LocalDate> monthDays;
}