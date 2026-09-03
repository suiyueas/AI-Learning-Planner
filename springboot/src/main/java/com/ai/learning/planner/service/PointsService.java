package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.*;
import com.ai.learning.planner.entity.CheckinConfig;
import com.ai.learning.planner.entity.CheckinRecord;
import com.ai.learning.planner.entity.PointTransaction;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.entity.UserPoints;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.exception.InsufficientPointsException;
import com.ai.learning.planner.repository.CheckinConfigRepository;
import com.ai.learning.planner.repository.CheckinRecordRepository;
import com.ai.learning.planner.repository.PointTransactionRepository;
import com.ai.learning.planner.repository.UserPointsRepository;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 积分服务
 * 处理签到积分、积分消耗、配置管理等核心业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointsService {

    private final UserPointsRepository userPointsRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final CheckinConfigRepository checkinConfigRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CHECKIN_LOCK_PREFIX = "checkin:lock:";
    private static final String CONSUME_LOCK_PREFIX = "consume:lock:";
    private static final long LOCK_EXPIRE_MINUTES = 5;

    /**
     * 获取用户当前积分余额
     */
    public PointsBalanceDTO getBalance(Long userId) {
        UserPoints userPoints = userPointsRepository.findByUserId(userId)
                .orElse(UserPoints.builder()
                        .userId(userId)
                        .totalEarned(0L)
                        .availablePoints(0L)
                        .frozenPoints(0L)
                        .build());

        boolean todayChecked = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, LocalDate.now());
        int continuousDays = calculateContinuousDays(userId);

        return PointsBalanceDTO.builder()
                .userId(userId)
                .availablePoints(userPoints.getAvailablePoints())
                .totalEarned(userPoints.getTotalEarned())
                .frozenPoints(userPoints.getFrozenPoints())
                .continuousDays(continuousDays)
                .todayChecked(todayChecked)
                .build();
    }

    /**
     * 获取今日签到状态
     */
    public CheckinStatusDTO getCheckinStatus(Long userId) {
        LocalDate today = LocalDate.now();
        boolean todayChecked = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today);
        int continuousDays = calculateContinuousDays(userId);
        long totalDays = checkinRecordRepository.countByUserId(userId);

        UserPoints userPoints = userPointsRepository.findByUserId(userId)
                .orElse(UserPoints.builder()
                        .userId(userId)
                        .availablePoints(0L)
                        .build());

        long todayPoints = getDailyCheckinPoints();
        if (!todayChecked && continuousDays > 0 && continuousDays % getConsecutiveDays() == 0) {
            todayPoints += getConsecutiveBonusPoints();
        }

        YearMonth yearMonth = YearMonth.now();
        List<LocalDate> monthDays = checkinRecordRepository.findCheckinDatesByUserIdAndMonth(
                userId, yearMonth.getYear(), yearMonth.getMonthValue());

        return CheckinStatusDTO.builder()
                .todayChecked(todayChecked)
                .continuousDays(continuousDays)
                .totalDays((int) totalDays)
                .availablePoints(userPoints.getAvailablePoints())
                .todayPoints(todayPoints)
                .checkinDate(today)
                .monthDays(monthDays)
                .build();
    }

    /**
     * 获取月度签到日历
     */
    public List<LocalDate> getMonthCalendar(Long userId, int year, int month) {
        return checkinRecordRepository.findCheckinDatesByUserIdAndMonth(userId, year, month);
    }

    /**
     * 用户签到
     */
    @Transactional
    public CheckinResultDTO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        String lockKey = CHECKIN_LOCK_PREFIX + userId + ":" + today;

        // 分布式锁防止重复签到
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("签到操作进行中，请稍后重试");
        }

        try {
            // 检查今日是否已签到
            if (checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today)) {
                throw new BusinessException("今日已签到，请勿重复签到");
            }

            // 计算连续签到天数
            int continuousDays = calculateContinuousDays(userId);

            // 检查昨天是否签到，如果昨天签到了则连续天数+1
            LocalDate yesterday = today.minusDays(1);
            boolean yesterdayChecked = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, yesterday);
            if (yesterdayChecked) {
                continuousDays++;
            } else {
                continuousDays = 1; // 断签后重新开始
            }

            // 计算本次签到获得的积分
            long basePoints = getDailyCheckinPoints();
            long bonusPoints = 0;
            boolean bonusTriggered = false;

            // 检查是否触发连续奖励
            int consecutiveDays = getConsecutiveDays();
            if (continuousDays > 0 && continuousDays % consecutiveDays == 0) {
                bonusPoints = getConsecutiveBonusPoints();
                bonusTriggered = true;
            }

            long totalPoints = basePoints + bonusPoints;

            // 插入签到记录
            CheckinRecord checkinRecord = new CheckinRecord();
            checkinRecord.setUserId(userId);
            checkinRecord.setCheckinDate(today);
            checkinRecord.setCreatedAt(LocalDateTime.now());
            checkinRecordRepository.save(checkinRecord);

            // 更新用户积分
            UserPoints userPoints = userPointsRepository.findByUserId(userId)
                    .orElse(UserPoints.builder()
                            .userId(userId)
                            .totalEarned(0L)
                            .availablePoints(0L)
                            .frozenPoints(0L)
                            .version(0L)
                            .build());

            long balanceBefore = userPoints.getAvailablePoints();
            userPoints.setTotalEarned(userPoints.getTotalEarned() + totalPoints);
            userPoints.setAvailablePoints(userPoints.getAvailablePoints() + totalPoints);
            userPointsRepository.save(userPoints);

            // 记录积分流水
            PointTransaction baseTransaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionType("CHECKIN")
                    .points(basePoints)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceBefore + basePoints)
                    .source("CHECKIN")
                    .referenceId(checkinRecord.getId())
                    .description("每日签到奖励")
                    .build();
            pointTransactionRepository.save(baseTransaction);

            if (bonusTriggered) {
                PointTransaction bonusTransaction = PointTransaction.builder()
                        .userId(userId)
                        .transactionType("CHECKIN_BONUS")
                        .points(bonusPoints)
                        .balanceBefore(balanceBefore + basePoints)
                        .balanceAfter(balanceBefore + totalPoints)
                        .source("CHECKIN")
                        .referenceId(checkinRecord.getId())
                        .description("连续签到" + continuousDays + "天奖励")
                        .build();
                pointTransactionRepository.save(bonusTransaction);
            }

            // 获取本月签到日期
            YearMonth yearMonth = YearMonth.now();
            List<LocalDate> monthDays = checkinRecordRepository.findCheckinDatesByUserIdAndMonth(
                    userId, yearMonth.getYear(), yearMonth.getMonthValue());

            String message = "签到成功！获得" + totalPoints + "积分";
            if (bonusTriggered) {
                message += "（含连续奖励" + bonusPoints + "积分）";
            }

            return CheckinResultDTO.builder()
                    .basePoints(basePoints)
                    .bonusPoints(bonusPoints)
                    .totalPoints(totalPoints)
                    .continuousDays(continuousDays)
                    .totalDays(monthDays.size())
                    .availablePoints(userPoints.getAvailablePoints())
                    .bonusTriggered(bonusTriggered)
                    .checkinDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .message(message)
                    .build();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 消耗积分
     */
    @Transactional
    public void consumePoints(Long userId, Long points, String source) {
        // 检查用户是否为管理员
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if ("ADMIN".equals(user.getRole())) {
            log.info("管理员用户 {} 免积分使用功能: {}", userId, source);
            return;
        }

        String lockKey = CONSUME_LOCK_PREFIX + userId + ":" + source;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("操作进行中，请稍后重试");
        }

        try {
            UserPoints userPoints = userPointsRepository.findByUserId(userId)
                    .orElse(UserPoints.builder()
                            .userId(userId)
                            .totalEarned(0L)
                            .availablePoints(0L)
                            .frozenPoints(0L)
                            .version(0L)
                            .build());

            if (userPoints.getAvailablePoints() < points) {
                throw new InsufficientPointsException(userPoints.getAvailablePoints(), points, source);
            }

            long balanceBefore = userPoints.getAvailablePoints();
            userPoints.setAvailablePoints(userPoints.getAvailablePoints() - points);
            userPointsRepository.save(userPoints);

            PointTransaction transaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionType("CONSUME")
                    .points(-points)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(userPoints.getAvailablePoints())
                    .source(source)
                    .description("消耗积分使用" + source)
                    .build();
            pointTransactionRepository.save(transaction);

            log.info("用户 {} 消耗 {} 积分，来源: {}，剩余: {}", userId, points, source, userPoints.getAvailablePoints());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 管理员发放积分
     */
    @Transactional
    public void grantPoints(Long userId, Long points, String description) {
        UserPoints userPoints = userPointsRepository.findByUserId(userId)
                .orElse(UserPoints.builder()
                        .userId(userId)
                        .totalEarned(0L)
                        .availablePoints(0L)
                        .frozenPoints(0L)
                        .version(0L)
                        .build());

        long balanceBefore = userPoints.getAvailablePoints();
        userPoints.setTotalEarned(userPoints.getTotalEarned() + points);
        userPoints.setAvailablePoints(userPoints.getAvailablePoints() + points);
        userPointsRepository.save(userPoints);

        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .transactionType("ADMIN_GRANT")
                .points(points)
                .balanceBefore(balanceBefore)
                .balanceAfter(userPoints.getAvailablePoints())
                .source("ADMIN")
                .description(description)
                .build();
        pointTransactionRepository.save(transaction);

        log.info("管理员发放 {} 积分给用户 {}，原因: {}", points, userId, description);
    }

    /**
     * 管理员扣除积分
     */
    @Transactional
    public void revokePoints(Long userId, Long points, String description) {
        UserPoints userPoints = userPointsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("用户积分记录不存在"));

        if (userPoints.getAvailablePoints() < points) {
            throw new BusinessException("用户积分不足，当前积分: " + userPoints.getAvailablePoints());
        }

        long balanceBefore = userPoints.getAvailablePoints();
        userPoints.setAvailablePoints(userPoints.getAvailablePoints() - points);
        userPointsRepository.save(userPoints);

        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .transactionType("ADMIN_REVOKE")
                .points(-points)
                .balanceBefore(balanceBefore)
                .balanceAfter(userPoints.getAvailablePoints())
                .source("ADMIN")
                .description(description)
                .build();
        pointTransactionRepository.save(transaction);

        log.info("管理员扣除用户 {} 的 {} 积分，原因: {}", userId, points, description);
    }

    /**
     * 获取积分配置
     */
    public PointsConfigDTO getConfig() {
        return PointsConfigDTO.builder()
                .dailyCheckinPoints(getConfigValue(CheckinConfig.KEY_DAILY_CHECKIN_POINTS, CheckinConfig.DEFAULT_DAILY_CHECKIN_POINTS))
                .consecutiveDays(getConfigValueAsInt(CheckinConfig.KEY_CONSECUTIVE_DAYS, CheckinConfig.DEFAULT_CONSECUTIVE_DAYS))
                .consecutiveBonusPoints(getConfigValue(CheckinConfig.KEY_CONSECUTIVE_BONUS_POINTS, CheckinConfig.DEFAULT_CONSECUTIVE_BONUS_POINTS))
                .chatConsumePoints(getConfigValue(CheckinConfig.KEY_CHAT_CONSUME_POINTS, CheckinConfig.DEFAULT_CHAT_CONSUME_POINTS))
                .agentConsumePoints(getConfigValue(CheckinConfig.KEY_AGENT_CONSUME_POINTS, CheckinConfig.DEFAULT_AGENT_CONSUME_POINTS))
                .learningPathConsumePoints(getConfigValue(CheckinConfig.KEY_LEARNING_PATH_CONSUME_POINTS, CheckinConfig.DEFAULT_LEARNING_PATH_CONSUME_POINTS))
                .build();
    }

    /**
     * 更新积分配置
     */
    @Transactional
    public void updateConfig(PointsConfigRequest request) {
        if (request.getDailyCheckinPoints() != null) {
            updateConfigValue(CheckinConfig.KEY_DAILY_CHECKIN_POINTS, String.valueOf(request.getDailyCheckinPoints()));
        }
        if (request.getConsecutiveDays() != null) {
            updateConfigValue(CheckinConfig.KEY_CONSECUTIVE_DAYS, String.valueOf(request.getConsecutiveDays()));
        }
        if (request.getConsecutiveBonusPoints() != null) {
            updateConfigValue(CheckinConfig.KEY_CONSECUTIVE_BONUS_POINTS, String.valueOf(request.getConsecutiveBonusPoints()));
        }
        if (request.getChatConsumePoints() != null) {
            updateConfigValue(CheckinConfig.KEY_CHAT_CONSUME_POINTS, String.valueOf(request.getChatConsumePoints()));
        }
        if (request.getAgentConsumePoints() != null) {
            updateConfigValue(CheckinConfig.KEY_AGENT_CONSUME_POINTS, String.valueOf(request.getAgentConsumePoints()));
        }
        if (request.getLearningPathConsumePoints() != null) {
            updateConfigValue(CheckinConfig.KEY_LEARNING_PATH_CONSUME_POINTS, String.valueOf(request.getLearningPathConsumePoints()));
        }
        log.info("积分配置已更新");
    }

    /**
     * 获取用户积分流水
     */
    public List<PointTransactionDTO> getTransactions(Long userId) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return transactions.stream()
                .map(this::convertToTransactionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }

    // ========== 私有方法 ==========

    /**
     * 计算连续签到天数
     * 从昨天开始往前推，直到断签
     */
    private int calculateContinuousDays(Long userId) {
        LocalDate today = LocalDate.now();
        int continuousDays = 0;
        LocalDate checkDate = today.minusDays(1); // 从昨天开始

        while (checkinRecordRepository.existsByUserIdAndCheckinDate(userId, checkDate)) {
            continuousDays++;
            checkDate = checkDate.minusDays(1);
        }

        return continuousDays;
    }

    /**
     * 获取每日签到基础积分
     */
    private long getDailyCheckinPoints() {
        return getConfigValue(CheckinConfig.KEY_DAILY_CHECKIN_POINTS, CheckinConfig.DEFAULT_DAILY_CHECKIN_POINTS);
    }

    /**
     * 获取连续签到奖励周期
     */
    private int getConsecutiveDays() {
        return getConfigValueAsInt(CheckinConfig.KEY_CONSECUTIVE_DAYS, CheckinConfig.DEFAULT_CONSECUTIVE_DAYS);
    }

    /**
     * 获取连续签到奖励积分
     */
    private long getConsecutiveBonusPoints() {
        return getConfigValue(CheckinConfig.KEY_CONSECUTIVE_BONUS_POINTS, CheckinConfig.DEFAULT_CONSECUTIVE_BONUS_POINTS);
    }

    /**
     * 获取配置值（Long）
     */
    private long getConfigValue(String key, String defaultValue) {
        return checkinConfigRepository.findConfigValue(key)
                .map(Long::parseLong)
                .orElse(Long.parseLong(defaultValue));
    }

    /**
     * 获取配置值（Integer）
     */
    private int getConfigValueAsInt(String key, String defaultValue) {
        return checkinConfigRepository.findConfigValue(key)
                .map(Integer::parseInt)
                .orElse(Integer.parseInt(defaultValue));
    }

    /**
     * 更新配置值
     */
    private void updateConfigValue(String key, String value) {
        Optional<CheckinConfig> existing = checkinConfigRepository.findByConfigKey(key);
        if (existing.isPresent()) {
            checkinConfigRepository.updateConfigValue(key, value);
        } else {
            CheckinConfig config = CheckinConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(key)
                    .build();
            checkinConfigRepository.save(config);
        }
    }

    /**
     * 将 PointTransaction 转换为 DTO
     */
    private PointTransactionDTO convertToTransactionDTO(PointTransaction transaction) {
        return PointTransactionDTO.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .points(transaction.getPoints())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .source(transaction.getSource())
                .referenceId(transaction.getReferenceId())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
