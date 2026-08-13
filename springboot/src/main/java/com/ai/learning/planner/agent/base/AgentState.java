package com.ai.learning.planner.agent.base;

/**
 * 智能体状态枚举
 * IDLE: 空闲待命
 * RUNNING: 执行中
 * FINISHED: 执行完成
 * ERROR: 执行出错
 */
public enum AgentState {
    IDLE,
    RUNNING,
    FINISHED,
    ERROR
}
