import { get, post } from './request'

export function getAchievements() {
  return get('/achievements')
}

export function checkAchievements() {
  return post('/achievements/check', {})
}

export function unlockAchievement(achievementId) {
  return post('/achievements/unlock', { achievementId })
}

export function checkIn() {
  return post('/achievements/checkin', {})
}

export const checkin = () => {
    return post('/achievements/checkin')
}

export const getCheckinStats = () => {
    return get('/achievements/checkin/stats')
}

/**
 * 获取月度学习统计（打卡日历 + 学习时长/任务数汇总）
 * @param {number} year 年份
 * @param {number} month 月份（1-12）
 * @returns {Promise} { year, month, calendar: [{date,type,duration,count}], summary: {totalDays,totalHours,completedTasks,checkinRate} }
 */
export const getMonthlyStats = (year, month) => {
    return get('/achievements/calendar/monthly-stats', { year, month })
}

/**
 * 生成成就分享内容（文案/链接/图片数据）
 * @param {string} achievementId 成就ID
 * @param {string} format 分享格式 text/image
 * @returns {Promise} { shareData, shareText, shareUrl }
 */
export const shareAchievement = (achievementId, format = 'text') => {
    return post('/achievements/share', { achievementId, format })
}