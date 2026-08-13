// 全局单例确认弹窗工具
// 解决 ElMessageBox 命令式弹窗堆叠问题：
// 1. 同一时刻只允许一个确认弹窗存在，新弹窗出现时自动销毁旧弹窗（防止重复弹窗）
// 2. 组件卸载时调用 cleanupDialogs() 清理残留弹窗（防止路由切换后弹窗累积堆叠）
import { ElMessageBox, ElMessage } from 'element-plus'

let activeInstance = null

const destroyActive = () => {
  if (activeInstance) {
    try { activeInstance.close() } catch { /* 弹窗已关闭则忽略 */ }
    activeInstance = null
  }
}

/**
 * 单例确认弹窗（替代直接调用 ElMessageBox.confirm）
 * @param {string} message 提示内容
 * @param {string} title 标题
 * @param {object} options ElMessageBox 选项（type / confirmButtonText / cancelButtonText / distinguishCancelAndClose 等）
 * @returns {Promise} 确认时 resolve；取消 / 关闭时 reject（'cancel' / 'close'），与原 ElMessageBox 行为一致
 */
export const confirmAction = (message, title, options = {}) => {
  // 已有弹窗时先关闭，保证同一时刻只有一个确认弹窗
  destroyActive()
  const instance = ElMessageBox.confirm(message, title, {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    ...options
  })
  activeInstance = instance
  // 防止 cancel / close 产生 unhandled rejection（不影响调用方自身的 catch）
  instance.catch(() => {})
  instance.finally(() => { if (activeInstance === instance) activeInstance = null })
  return instance
}

/**
 * 关闭全部确认弹窗与轻提示（组件卸载时调用，防止弹窗残留堆叠）
 */
export const cleanupDialogs = () => {
  destroyActive()
  try { ElMessageBox.close() } catch { /* 忽略 */ }
  try { ElMessage.closeAll() } catch { /* 忽略 */ }
}

export default { confirmAction, cleanupDialogs }
