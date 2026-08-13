package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.CheckinResultDTO;
import com.ai.learning.planner.entity.CheckinRecord;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.repository.CheckinRecordRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打卡服务
 * 提供用户每日打卡、打卡统计等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinService {

    private final CheckinRecordRepository checkinRecordRepository;
    private final UserRepository userRepository;
    private final LearningRecordRepository learningRecordRepository;

    /**
     * 用户每日打卡
     * 检查今日是否已打卡，更新连续打卡天数和总打卡天数
     */
    @Transactional
    public CheckinResultDTO checkin(Long userId) {
        LocalDate today = LocalDate.now();

        if (checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today)) {
            throw new BusinessException("今天已打卡，明天再来吧！");
        }

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setCreatedAt(LocalDateTime.now());
        checkinRecordRepository.save(record);
        log.info("用户打卡成功: userId={}, date={}", userId, today);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDate yesterday = today.minusDays(1);
        boolean checkedYesterday = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, yesterday);

        int continuousDays = checkedYesterday ? user.getContinuousCheckinDays() + 1 : 1;
        int totalDays = user.getTotalCheckinDays() + 1;

        user.setContinuousCheckinDays(continuousDays);
        user.setTotalCheckinDays(totalDays);
        userRepository.save(user);

        CheckinResultDTO result = new CheckinResultDTO();
        result.setTodayChecked(true);
        result.setContinuousDays(continuousDays);
        result.setTotalDays(totalDays);
        result.setCheckinDate(today.toString());
        result.setMonthDays(getMonthCheckinDates(userId, today.getYear(), today.getMonthValue()));

        return result;
    }

    /**
     * 获取指定月份的打卡日期列表
     */
    public List<LocalDate> getMonthCheckinDates(Long userId, int year, int month) {
        return checkinRecordRepository.findCheckinDatesByUserIdAndMonth(userId, year, month);
    }

    /**
     * 获取用户打卡统计数据
     */
    public CheckinResultDTO getCheckinStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDate today = LocalDate.now();
        boolean todayChecked = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today);

        CheckinResultDTO result = new CheckinResultDTO();
        result.setTodayChecked(todayChecked);
        result.setContinuousDays(user.getContinuousCheckinDays());
        result.setTotalDays(user.getTotalCheckinDays());
        result.setCheckinDate(today.toString());
        result.setMonthDays(getMonthCheckinDates(userId, today.getYear(), today.getMonthValue()));

        return result;
    }

    /**
     * 获取指定年月的月度统计数据
     * 包含日历视图数据和汇总信息
     */
    public Map<String, Object> getMonthlyStats(Long userId, int year, int month) {
        log.info("[monthlyStats] 获取月度统计: userId={}, year={}, month={}", userId, year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<LocalDate> checkinDates = getMonthCheckinDates(userId, year, month);
        Set<LocalDate> checkinDateSet = new HashSet<>(checkinDates);

        // 月度真实学习记录（已完成节点），按天分组供日历展示真实时长与任务数
        List<LearningRecord> monthRecords = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
                        String.valueOf(userId),
                        "completed",
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );
        Map<LocalDate, List<LearningRecord>> recordsByDay = monthRecords.stream()
                .filter(r -> r.getCompletedAt() != null)
                .collect(Collectors.groupingBy(r -> r.getCompletedAt().toLocalDate()));

        List<Map<String, Object>> calendar = new ArrayList<>();
        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", date.toString());

            List<LearningRecord> dayRecords = recordsByDay.getOrDefault(date, Collections.emptyList());
            int dayDuration = dayRecords.stream()
                    .mapToInt(r -> r.getTimeSpent() != null ? r.getTimeSpent() : 0).sum();
            int dayCount = dayRecords.size();

            if (date.isAfter(LocalDate.now())) {
                dayInfo.put("type", "future");
                dayInfo.put("duration", 0);
                dayInfo.put("count", 0);
            } else if (checkinDateSet.contains(date) || dayCount > 0) {
                dayInfo.put("type", "learning");
                dayInfo.put("duration", dayDuration);
                dayInfo.put("count", dayCount);
            } else {
                dayInfo.put("type", "none");
                dayInfo.put("duration", 0);
                dayInfo.put("count", 0);
            }
            calendar.add(dayInfo);
        }

        int totalDays = checkinDates.size();
        int totalHours = monthRecords.stream()
                .filter(r -> r.getTimeSpent() != null)
                .mapToInt(LearningRecord::getTimeSpent)
                .sum() / 60;
        int completedTasks = (int) monthRecords.stream()
                .filter(r -> "completed".equals(r.getStatus()))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDays", totalDays);
        summary.put("totalHours", totalHours);
        summary.put("completedTasks", completedTasks);
        summary.put("checkinRate", calculateCheckinRate(year, month, totalDays));

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("calendar", calendar);
        result.put("summary", summary);

        log.info("[monthlyStats] 月度统计完成: userId={}, totalDays={}, totalHours={}", userId, totalDays, totalHours);
        return result;
    }

    /**
     * 计算指定月份的打卡率
     */
    private int calculateCheckinRate(int year, int month, int checkinDays) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int daysPassed;

        if (year == today.getYear() && month == today.getMonthValue()) {
            daysPassed = today.getDayOfMonth();
        } else if (year < today.getYear() || (year == today.getYear() && month < today.getMonthValue())) {
            daysPassed = daysInMonth;
        } else {
            daysPassed = 0;
        }

        if (daysPassed == 0) return 0;
        return Math.round((float) checkinDays / daysPassed * 100);
    }
}