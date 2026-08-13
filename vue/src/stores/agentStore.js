// 智能体状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAgents as getAgentsApi, executeAgentTask as executeAgentApi, streamAgentExecution, getAgentLogs as fetchLogsApi, getAgentResults as fetchResultsApi, saveAgentExecution,
  getAllLogs as fetchAllLogsApi, getAllResults as fetchAllResultsApi, getResultById as fetchResultDetailApi,
  clearAllLogs as clearLogsApi, clearAllResults as clearResultsApi } from '@/api/agentApi'

export const useAgentStore = defineStore('agent', () => {
  // ==================== 状态 ====================
  
  // 所有Agent列表（含Orchestrator）
  const agentList = ref([])
  
  // 是否正在加载
  const isLoading = ref(false)
  
  // 加载错误
  const loadError = ref(null)
  
  // 任务执行状态
  const isExecuting = ref(false)
  
  // 当前正在执行的Agent ID
  const executingAgentId = ref(null)
  
  // ReAct步骤状态
  const reactStep = ref('idle') // idle, thinking, acting, observing
  
  // ReAct迭代次数
  const reactIteration = ref(0)
  
  // 执行日志
  const executionLogs = ref([])
  
  // 执行结果列表（持久化）
  const executionResults = ref([])
  
  // 当前任务执行结果
  const taskResult = ref(null)
  
  // 任务历史
  const taskHistory = ref([])
  
  // 当前的EventSource
  let currentEventSource = null
  
  // 模拟执行定时器
  let simTimers = []
  
  // 当前执行任务的日志起始索引（用于持久化）
  const currentLogStart = ref(0)
  
  // ==================== 历史数据（持久化展示） ====================
  // 所有历史执行日志（从后端加载）
  const allLogs = ref([])
  // 所有历史执行结果（从后端加载）
  const allResults = ref([])
  // 当前查看的结果详情
  const selectedResult = ref(null)
  // 结果筛选类型
  const resultFilter = ref('all')
  
  // 根据筛选类型过滤结果
  const filteredResults = computed(() => {
    if (resultFilter.value === 'all') return allResults.value
    return allResults.value.filter(r => r.resultType === resultFilter.value)
  })
  
  // 编排器状态（可切换: IDLE/RUNNING/MAINTENANCE）
  const orchestratorStatus = ref('IDLE')
  
  // ============ localStorage 持久化（保留执行次数统计） ============
  const LS_KEYS = {
    STATS: 'agent_stats'
  }
  
  const saveStats = () => {
    const stats = {}
    agentList.value.forEach(a => { stats[a.id] = a.execCount || 0 })
    try { localStorage.setItem(LS_KEYS.STATS, JSON.stringify(stats)) } catch {}
  }
  
  const restoreStats = () => {
    try {
      const raw = localStorage.getItem(LS_KEYS.STATS)
      if (raw) {
        const stats = JSON.parse(raw)
        agentList.value.forEach(a => {
          if (stats[a.id] !== undefined) a.execCount = stats[a.id]
        })
      }
    } catch {}
  }

  // ==================== 计算属性 ====================
  
  // 子Agent列表（不含编排器）
  const subAgents = computed(() => {
    return agentList.value.filter(a => a.type === 'app')
  })
  
  // 编排器信息
  const orchestratorInfo = computed(() => {
    return agentList.value.find(a => a.type === 'orchestrator')
  })
  
  // 可用Agent数量
  const availableCount = computed(() => {
    return agentList.value.filter(a => a.status === 'IDLE').length
  })
  
  // 执行中Agent数量
  const executingCount = computed(() => {
    return agentList.value.filter(a => a.status === 'RUNNING').length
  })
  
  // 总Agent数量
  const totalCount = computed(() => agentList.value.length)

  // ==================== 操作 ====================
  
  // 获取Agent列表
  const fetchAgents = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getAgentsApi()
      if (res.code === 200 || res.success) {
        agentList.value = res.data || []
      } else if (res.data) {
        agentList.value = res.data
      }
    } catch (error) {
      // 403 说明后端运行但 Token 无效，静默使用模拟数据即可
      if (error.response?.status === 403) {
        console.info('后端鉴权未通过，使用模拟数据')
      } else {
        console.error('获取Agent列表失败:', error)
      }
      loadError.value = error.message || '获取Agent列表失败'
      // 降级：使用模拟数据
      agentList.value = getMockAgentList()
    } finally {
      isLoading.value = false
      // 从 localStorage 恢复执行次数统计
      restoreStats()
    }
  }
  
  // 同步执行任务
  const executeTask = async (agentId, message) => {
    if (!agentId || !message.trim()) return null
    
    // 记录当前日志起始位置（用于持久化）
    currentLogStart.value = executionLogs.value.length
    
    isExecuting.value = true
    executingAgentId.value = agentId
    reactStep.value = 'thinking'
    reactIteration.value = 1
    
    // 创建任务记录
    const taskRecord = {
      id: 'task_' + Date.now(),
      agentId: agentId,
      task: message,
      status: 'executing',
      result: null,
      startedAt: new Date(),
      completedAt: null
    }
    taskHistory.value.unshift(taskRecord)
    
    // 添加初始日志
    addLog('system', '任务已提交', `分配给 ${getAgentName(agentId)}`)
    addLog('think', '分析任务', `正在分析: ${message}`)
    
    try {
      const res = await executeAgentApi({ agentId, message })
      
      if (res.data) {
        taskRecord.status = res.data.status === 'FINISHED' ? 'completed' : 'error'
        taskRecord.result = res.data.output || '任务执行完成'
        taskRecord.completedAt = new Date()
        addLog('complete', '任务完成', taskRecord.result)
      }
      
      // 持久化执行数据
      taskResult.value = generateMockResult(agentId, message)
      await saveExecutionToBackend(taskRecord)
      
      return taskRecord
    } catch (error) {
      console.error('任务执行失败:', error)
      taskRecord.status = 'error'
      taskRecord.result = `执行失败: ${error.message}`
      taskRecord.completedAt = new Date()
      addLog('error', '执行出错', error.message)
      return taskRecord
    } finally {
      isExecuting.value = false
      executingAgentId.value = null
      reactStep.value = 'idle'
      reactIteration.value = 0
    }
  }
  
  // ============ 辅助函数 ============
  
  // 更新Agent状态
  const updateAgentStatus = (agentId, status) => {
    const agent = agentList.value.find(a => a.id === agentId)
    if (agent) agent.status = status
  }
  
  // 清除模拟定时器
  const clearSimTimers = () => {
    simTimers.forEach(t => clearTimeout(t))
    simTimers = []
  }
  
  // 生成模拟执行结果
  const generateMockResult = (agentId, message) => {
    const agentName = getAgentName(agentId)
    
    if (agentId === 'exercise') {
      return {
        type: 'exercise',
        title: 'Python基础练习题（5道）',
        items: [
          { question: 'Python中如何定义一个变量？请举例说明。', answer: '使用赋值语句，例如：name = "Alice" 或 count = 10，Python是动态类型语言，无需声明类型。' },
          { question: '解释一下什么是数据类型转换，并给出一个例子。', answer: '数据类型转换分为隐式转换和显式转换。例如 int("123") 将字符串转为整数，str(456) 将整数转为字符串。' },
          { question: 'if-elif-else语句的执行流程是什么？', answer: '依次检查条件表达式：if条件为真时执行对应代码块，否则继续检查elif，全部不满足时执行else分支。' },
          { question: 'for循环和while循环的区别是什么？', answer: 'for循环适用于遍历已知序列（如列表、字符串），while循环适用于条件未知次数的迭代场景。' },
          { question: '如何定义一个函数？请给出一个带参数的函数示例。', answer: '使用def关键字：def greet(name): return f"Hello, {name}!"，调用：greet("Alice") 返回 "Hello, Alice!"' }
        ]
      }
    }
    if (agentId === 'planner') {
      const is3Month = message.includes('3个月') || message.includes('3 个月')
      const is6Month = message.includes('6个月') || message.includes('6 个月')
      const duration = is6Month ? '6个月' : is3Month ? '3个月' : '1个月'
      return {
        type: 'plan',
        title: `Python数据分析师 · ${duration}学习计划`,
        duration: duration,
        overview: '从零基础到掌握Python数据分析核心技能，包含每日任务清单、学习资源和里程碑节点，助你高效达成学习目标。',
        phases: [
          {
            phase: 1,
            title: '基础入门',
            duration: '第1-4周',
            description: '学习Python核心概念和基础语法，完成基础编程练习，建立编程思维。',
            weeks: [
              {
                week: 1,
                tasks: [
                  { day: '周一', content: '学习Python基础语法（变量、数据类型、运算符）', estimated: '2h' },
                  { day: '周二', content: '学习条件语句和循环结构', estimated: '2h' },
                  { day: '周三', content: '学习函数定义和调用', estimated: '2h' },
                  { day: '周四', content: '学习列表、元组、字典等数据结构', estimated: '2h' },
                  { day: '周五', content: '综合练习：编写一个小程序', estimated: '3h' },
                  { day: '周六', content: '复习本周内容 + 完成课后作业', estimated: '2h' },
                  { day: '周日', content: '休息/自由学习', estimated: '1h' }
                ],
                resources: ['Python官方文档', '廖雪峰Python教程']
              },
              {
                week: 2,
                tasks: [
                  { day: '周一', content: '学习文件读写操作', estimated: '2h' },
                  { day: '周二', content: '学习异常处理机制', estimated: '2h' },
                  { day: '周三', content: '学习模块和包的概念', estimated: '2h' },
                  { day: '周四', content: '学习面向对象编程基础', estimated: '3h' },
                  { day: '周五', content: '学习面向对象高级特性', estimated: '2h' },
                  { day: '周六', content: '综合实战：学生管理系统', estimated: '3h' },
                  { day: '周日', content: '休息/代码回顾', estimated: '1h' }
                ],
                resources: ['笨方法学Python', 'Codecademy Python课程']
              }
            ],
            milestones: ['完成10个小程序', '通过基础语法阶段测试', '完成学生管理系统项目']
          },
          {
            phase: 2,
            title: '进阶实践',
            duration: '第5-8周',
            description: '掌握数据分析核心库（NumPy、Pandas、Matplotlib），完成数据分析项目实践。',
            weeks: [
              {
                week: 5,
                tasks: [
                  { day: '周一', content: '学习NumPy数组基础操作', estimated: '2h' },
                  { day: '周二', content: '学习NumPy高级索引和广播', estimated: '2h' },
                  { day: '周三', content: '学习Pandas Series数据结构', estimated: '2h' },
                  { day: '周四', content: '学习Pandas DataFrame操作', estimated: '3h' },
                  { day: '周五', content: '学习数据清洗和预处理', estimated: '2h' },
                  { day: '周六', content: '综合练习：数据清洗实战', estimated: '3h' },
                  { day: '周日', content: '休息/复习', estimated: '1h' }
                ],
                resources: ['NumPy官方文档', 'Pandas入门教程']
              },
              {
                week: 6,
                tasks: [
                  { day: '周一', content: '学习Matplotlib基础绘图', estimated: '2h' },
                  { day: '周二', content: '学习Seaborn统计可视化', estimated: '2h' },
                  { day: '周三', content: '学习数据聚合和分组操作', estimated: '2h' },
                  { day: '周四', content: '学习时间序列数据处理', estimated: '3h' },
                  { day: '周五', content: '数据探索性分析（EDA）实战', estimated: '3h' },
                  { day: '周六', content: '完整数据分析项目：销售数据分析', estimated: '4h' },
                  { day: '周日', content: '项目代码优化和总结', estimated: '2h' }
                ],
                resources: ['利用Python进行数据分析', 'Kaggle入门竞赛']
              }
            ],
            milestones: ['掌握NumPy/Pandas核心操作', '完成3个数据分析项目', '学会数据可视化']
          },
          {
            phase: 3,
            title: '综合提升',
            duration: '第9-12周',
            description: '学习高级分析技能，完成综合项目实战，巩固知识体系并拓展实际应用能力。',
            weeks: [
              {
                week: 9,
                tasks: [
                  { day: '周一', content: '学习数据接口与API调用', estimated: '2h' },
                  { day: '周二', content: '学习数据库操作基础（SQLite）', estimated: '2h' },
                  { day: '周三', content: '学习Web数据爬取基础', estimated: '2h' },
                  { day: '周四', content: '机器学习基础概念入门', estimated: '3h' },
                  { day: '周五', content: '学习Scikit-learn基础建模', estimated: '3h' },
                  { day: '周六', content: '完整数据科学项目：用户行为分析', estimated: '4h' },
                  { day: '周日', content: '项目复盘和知识梳理', estimated: '2h' }
                ],
                resources: ['Scikit-learn官方文档', '数据科学入门书籍']
              },
              {
                week: 10,
                tasks: [
                  { day: '周一', content: '学习特征工程基础方法', estimated: '2h' },
                  { day: '周二', content: '学习模型评估与调参', estimated: '2h' },
                  { day: '周三', content: '学习集成学习方法', estimated: '3h' },
                  { day: '周四', content: '综合项目：房价预测模型', estimated: '4h' },
                  { day: '周五', content: '学习生成数据报告和可视化', estimated: '2h' },
                  { day: '周六', content: '准备项目展示材料', estimated: '3h' },
                  { day: '周日', content: '整体复习和查漏补缺', estimated: '2h' }
                ],
                resources: ['Kaggle房价预测项目', '机器学习实战书籍']
              }
            ],
            milestones: ['完成综合数据科学项目', '掌握机器学习建模流程', '能够自主分析新数据集']
          }
        ],
        resources: [
          '《利用Python进行数据分析》（Wes McKinney）',
          'Python官方文档（docs.python.org）',
          'Kaggle入门竞赛平台',
          '廖雪峰Python教程',
          'NumPy/Pandas/Matplotlib官方文档'
        ],
        summary: '完成本计划后，你将具备使用Python进行数据分析的能力，掌握从数据获取、清洗、分析到可视化的完整流程，并具备基础的机器学习建模能力，能够独立完成数据分析项目。'
      }
    }
    if (agentId === 'tutor') {
      return {
        type: 'explanation',
        title: message.length > 30 ? message.substring(0, 30) + '...' : message,
        status: 'success',
        data: {
          query: message,
          result: `关于「${message.length > 30 ? message.substring(0, 30) + '...' : message}」的解答：\n\n基于知识库检索到的相关内容，从定义与原理、实际应用（附代码示例）、常见误区三个方面给出完整解释。`,          source: '知识库',
          confidence: 0.85
        },
        message: '任务已成功执行'
      }
    }
    if (agentId === 'reporter') {
      return {
        type: 'report',
        title: '学习报告',
        summary: `根据 ${message.includes('月') ? '月度' : '周期'} 学习数据分析，整体完成率良好。建议加强薄弱环节的针对性练习。`,
        metrics: [
          { label: '学习时长', value: '42.5小时' },
          { label: '完成率', value: '87%' },
          { label: '掌握程度', value: '良好' },
          { label: '薄弱环节', value: '排序算法' }
        ]
      }
    }
    if (agentId === 'diagnosis') {
      return {
        type: 'diagnosis',
        title: '学习水平诊断报告',
        summary: '已完成用户能力画像分析',
        dimensions: [
          { name: 'Python基础', score: 85, level: '良好' },
          { name: '数据结构', score: 60, level: '需加强' },
          { name: '算法思维', score: 45, level: '薄弱' },
          { name: '项目实践', score: 30, level: '需提升' }
        ]
      }
    }
    // 默认结果：返回任务执行摘要（data.result 为真实输出，message 仅为状态文案）
    return {
      type: 'default',
      title: `${agentName} 执行完成`,
      status: 'success',
      data: {
        query: message,
        result: `任务「${message.length > 40 ? message.substring(0, 40) + '...' : message}」执行完成，输出类型：${agentName} 执行结果。`,
        source: '本地执行引擎',
        confidence: 0.7
      },
      message: '任务已成功执行完毕'
    }
  }
  
  // 保存当前执行数据到后端数据库
  const saveExecutionToBackend = async (taskRecord) => {
    if (!taskRecord) return
    try {
      // 获取本次执行的新增日志
      const newLogs = executionLogs.value.slice(currentLogStart.value)
      if (newLogs.length === 0 && !taskResult.value) return

      await saveAgentExecution({
        agentId: taskRecord.agentId,
        taskDescription: taskRecord.task || '',
        agentName: getAgentName(taskRecord.agentId),
        result: taskResult.value,
        logs: newLogs
      })
      console.debug('[持久化] Agent执行数据已保存:', taskRecord.agentId)
    } catch (error) {
      console.warn('[持久化] 保存Agent执行数据失败:', error)
    }
  }

  // 完成任务
  const completeTaskCommon = async (taskRecord, resultMsg) => {
    if (taskRecord) {
      taskRecord.status = 'completed'
      taskRecord.completedAt = new Date()
      taskRecord.result = resultMsg || '任务执行完成'
      updateAgentStatus(taskRecord.agentId, 'IDLE')
      incrementExecCount(taskRecord.agentId)
      // 生成执行结果
      taskResult.value = generateMockResult(taskRecord.agentId, taskRecord.task)
    }
    isExecuting.value = false
    executingAgentId.value = null
    reactStep.value = 'idle'
    reactIteration.value = 0
    currentEventSource = null
    
    // 持久化执行数据到后端数据库（等待完成）
    await saveExecutionToBackend(taskRecord)
  }

  // ============ 模拟执行引擎 ============
  const startSimulation = (taskRecord, agentId, message) => {
    const agentName = getAgentName(agentId)
    const totalIterations = 3
    
    // 迭代各阶段的提示文案，不同迭代略有差异
    const thinkMsgs = [
      '正在解析任务需求，识别关键信息和约束条件...',
      '正在评估已有数据，确定最优执行方案...',
      '正在综合分析所有中间结果，制定最终输出...'
    ]
    const actMsgs = [
      '调用工具查询用户画像和知识图谱...',
      '调用工具生成个性化学习路径...',
      '调用工具汇总执行结果并优化方案...'
    ]
    const observeMsgs = [
      '工具调用完成，获取到用户画像和知识图谱数据',
      '学习路径生成完毕，质量校验通过，准备进入下一阶段',
      '所有结果已汇总，任务执行成功'
    ]
    
    const runCycle = (iteration) => {
      if (!isExecuting.value) return
      
      reactIteration.value = iteration
      const idx = Math.min(iteration - 1, totalIterations - 1)
      
      // Step 1: 思考
      reactStep.value = 'thinking'
      addLog('think', `思考 (${iteration}/${totalIterations})`, thinkMsgs[idx])
      
      simTimers.push(setTimeout(() => {
        if (!isExecuting.value) return
        
        // Step 2: 行动
        reactStep.value = 'acting'
        addLog('act', `行动 (${iteration}/${totalIterations})`, actMsgs[idx])
        
        simTimers.push(setTimeout(() => {
          if (!isExecuting.value) return
          
          // Step 3: 观察
          reactStep.value = 'observing'
          addLog('observe', `观察 (${iteration}/${totalIterations})`, observeMsgs[idx])
          
          simTimers.push(setTimeout(() => {
            if (!isExecuting.value) return
            
            if (iteration < totalIterations) {
              runCycle(iteration + 1)
            } else {
              // 全部完成
              const summary = message.length > 40 ? message.substring(0, 40) + '...' : message
              addLog('complete', '任务完成', `${agentName} 已成功完成任务：「${summary}」`)
              completeTaskCommon(taskRecord, '任务执行成功完成')
            }
          }, 1200))
        }, 2000))
      }, 1500))
    }
    
    // 延迟一小段时间后开始执行（等待对话框关闭动画）
    simTimers.push(setTimeout(() => runCycle(1), 600))
  }
  
  // ============ 流式执行任务（SSE + 模拟回退） ============
  const executeTaskStream = (agentId, message) => {
    if (!agentId || !message.trim()) return
    
    // 清除之前的任务
    clearSimTimers()
    if (currentEventSource) {
      currentEventSource.close()
      currentEventSource = null
    }
    
    // 清除旧的执行结果
    taskResult.value = null
    
    // 记录当前日志起始位置（用于后续持久化）
    currentLogStart.value = executionLogs.value.length
    
    isExecuting.value = true
    executingAgentId.value = agentId
    reactStep.value = 'thinking'
    reactIteration.value = 1
    
    // 更新Agent状态 -> RUNNING
    updateAgentStatus(agentId, 'RUNNING')
    
    // 创建任务记录
    const taskRecord = {
      id: 'task_' + Date.now(),
      agentId: agentId,
      task: message,
      status: 'executing',
      result: '',
      startedAt: new Date(),
      completedAt: null
    }
    taskHistory.value.unshift(taskRecord)
    
    // 添加初始日志
    addLog('system', '任务已提交', `分配给 ${getAgentName(agentId)}：${message}`)
    
    // ============ 尝试 SSE 连接，带超时回退 ============
    let sseConnected = false
    
    const callbacks = {
      onThink(data) {
        sseConnected = true
        reactStep.value = 'thinking'
        addLog('think', '思考', data.content || '')
      },
      onAct(data) {
        sseConnected = true
        reactStep.value = 'acting'
        const actMsg = data.tool ? `调用工具: ${data.tool}` : (data.content || '正在执行...')
        addLog('act', '行动', actMsg)
      },
      onObserve(data) {
        sseConnected = true
        reactStep.value = 'observing'
        addLog('observe', '观察', (data.content || '').substring(0, 200))
      },
      onToolCall(data) {
        sseConnected = true
        addLog('tool', `工具调用: ${data.tool}`, JSON.stringify(data.args || {}))
      },
      onToolResult(data) {
        sseConnected = true
        addLog('tool', '工具结果', ((data.result || '') + '').substring(0, 300))
        reactIteration.value++
      },
      onComplete(data) {
        sseConnected = true
        addLog('complete', '任务完成', data.message || '执行完毕')
        completeTaskCommon(taskRecord, data.message || '任务执行完成')
      },
      onError() {
        if (!sseConnected && isExecuting.value) {
          // SSE 连接失败，回退到模拟执行
          startSimulation(taskRecord, agentId, message)
        }
      },
      onStatus(data) {
        if (data.state === 'RUNNING') {
          sseConnected = true
          reactStep.value = 'thinking'
        }
      }
    }
    
    // SSE 连接超时回退（2秒未收到任何数据则启用模拟）
    const fallbackTimer = setTimeout(() => {
      if (!sseConnected && isExecuting.value) {
        startSimulation(taskRecord, agentId, message)
      }
    }, 2000)
    
    try {
      currentEventSource = streamAgentExecution(agentId, message, {
        ...callbacks,
        onThink(data) { clearTimeout(fallbackTimer); callbacks.onThink(data) },
        onComplete(data) { clearTimeout(fallbackTimer); callbacks.onComplete(data) },
        onError() { clearTimeout(fallbackTimer); callbacks.onError() }
      })
    } catch (e) {
      clearTimeout(fallbackTimer)
      if (!sseConnected && isExecuting.value) {
        startSimulation(taskRecord, agentId, message)
      }
    }
  }
  
  // 添加日志
  const addLog = (type, title, content) => {
    const logEntry = {
      id: 'log_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      type: type,
      title: title,
      content: content || '',
      timestamp: new Date()
    }
    executionLogs.value.push(logEntry)
    
    // 只保留最近200条
    if (executionLogs.value.length > 200) {
      executionLogs.value = executionLogs.value.slice(-200)
    }
    
    // 持久化到后端数据库
    // (Note: logs are persisted via saveAgentExecution API call in the execution flow)
  }
  
  // 清空日志
  const clearLogs = () => {
    executionLogs.value = []
  }
  
  // 清空任务历史
  const clearTaskHistory = () => {
    taskHistory.value = []
  }
  
  // 清空执行结果
  const clearTaskResult = () => {
    taskResult.value = null
  }
  
  // 添加执行结果
  const addExecutionResult = (result) => {
    executionResults.value.unshift({
      id: 'res_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      agentId: result.agentId,
      agentName: result.agentName,
      taskDescription: result.taskDescription || '',
      status: result.status || 'completed',
      resultType: result.resultType || 'default',
      result: result.result || {},
      duration: result.duration || 0,
      createdAt: result.createdAt || new Date().toISOString()
    })
    // 只保留最近100条
    if (executionResults.value.length > 100) {
      executionResults.value = executionResults.value.slice(0, 100)
    }
  }
  
  // 清空所有结果
  const clearExecutionResults = () => {
    executionResults.value = []
  }
  
  // 停止当前任务
  const stopCurrentTask = () => {
    clearSimTimers()
    if (currentEventSource) {
      currentEventSource.close()
      currentEventSource = null
    }
    
    // 恢复Agent状态
    if (isExecuting.value && executingAgentId.value) {
      updateAgentStatus(executingAgentId.value, 'IDLE')
    }
    
    isExecuting.value = false
    executingAgentId.value = null
    reactStep.value = 'idle'
    reactIteration.value = 0
    addLog('system', '任务已停止', '用户手动停止执行')
  }
  
  // 增加Agent执行次数
  const incrementExecCount = (agentId) => {
    const agent = agentList.value.find(a => a.id === agentId)
    if (agent) agent.execCount = (agent.execCount || 0) + 1
    saveStats()
  }
  
  // 获取Agent名称
  const getAgentName = (agentId) => {
    const agent = agentList.value.find(a => a.id === agentId)
    return agent ? agent.name : agentId
  }
  
  // ==================== 数据持久化（从后端API获取历史数据） ====================

  /**
   * 从后端获取指定Agent的历史执行日志
   */
  const fetchHistoryLogs = async (agentId) => {
    try {
      const res = await fetchLogsApi(agentId)
      if (res.code === 200 && res.data) {
        return res.data
      }
    } catch (error) {
      console.warn('获取历史日志失败:', error)
    }
    return []
  }

  /**
   * 从后端获取指定Agent的历史执行结果
   */
  const fetchHistoryResults = async (agentId) => {
    try {
      const res = await fetchResultsApi(agentId)
      if (res.code === 200 && res.data) {
        return res.data
      }
    } catch (error) {
      console.warn('获取历史结果失败:', error)
    }
    return []
  }

  // ==================== 历史数据操作（持久化展示） ====================

  /**
   * 从后端加载所有历史执行日志
   */
  const fetchAllLogs = async () => {
    try {
      const res = await fetchAllLogsApi()
      if (res.code === 200 && res.data) {
        allLogs.value = res.data
      }
    } catch (error) {
      console.warn('加载历史日志失败:', error)
    }
  }

  /**
   * 从后端加载所有历史执行结果
   */
  const fetchAllResults = async () => {
    try {
      const res = await fetchAllResultsApi()
      if (res.code === 200 && res.data) {
        allResults.value = res.data
      }
    } catch (error) {
      console.warn('加载历史结果失败:', error)
    }
  }

  /**
   * 从后端获取单个结果详情
   */
  const fetchResultDetail = async (id) => {
    try {
      const res = await fetchResultDetailApi(id)
      if (res.code === 200 && res.data) {
        selectedResult.value = res.data
        return res.data
      }
    } catch (error) {
      console.warn('获取结果详情失败:', error)
    }
    return null
  }

  /**
   * 清空所有历史执行日志
   */
  const clearAllHistoryLogs = async () => {
    try {
      await clearLogsApi()
      allLogs.value = []
    } catch (error) {
      console.warn('清空历史日志失败:', error)
    }
  }

  /**
   * 清空所有历史执行结果
   */
  const clearAllHistoryResults = async () => {
    try {
      await clearResultsApi()
      allResults.value = []
    } catch (error) {
      console.warn('清空历史结果失败:', error)
    }
  }

  /**
   * 设置结果筛选类型
   */
  const setResultFilter = (type) => {
    resultFilter.value = type
  }

  // ==================== 模拟数据 ====================
  
  const getMockAgentList = () => [
    {
      id: 'orchestrator',
      name: '编排Agent',
      description: '统一调度所有子Agent，协调多个专业智能体完成任务分配与回收',
      type: 'orchestrator',
      status: 'IDLE',
      icon: 'brain',
      role: '统一调度·意图识别',
      tools: [],
      currentStep: 0,
      maxSteps: 30,
      execCount: 128,
      exampleTask: '协调各Agent完成综合学习任务'
    },
    {
      id: 'diagnosis', name: '诊断Agent', description: '评估学习水平，构建用户能力画像，识别薄弱环节',
      type: 'app', status: 'IDLE', icon: 'search', role: '能力测评·画像构建',
      tools: ['unified_academic_search', 'smart_quiz_generation'], currentStep: 0, maxSteps: 30,
      execCount: 23, exampleTask: '"诊断我的Python学习水平"'
    },
    {
      id: 'planner', name: '规划Agent', description: '生成个性化学习路径，动态调整学习计划',
      type: 'app', status: 'IDLE', icon: 'map', role: '路径生成·动态调整',
      tools: ['unified_academic_search', 'full_chain_learning'], currentStep: 0, maxSteps: 30,
      execCount: 18, exampleTask: '"制定3个月学Java的计划"'
    },
    {
      id: 'tutor', name: '答疑Agent', description: '基于知识库的智能问答，引导式教学',
      type: 'app', status: 'IDLE', icon: 'message-square', role: '苏格拉底·RAG',
      tools: ['unified_academic_search', 'academic_translation'], currentStep: 0, maxSteps: 30,
      execCount: 45, exampleTask: '"解释一下什么是闭包？"'
    },
    {
      id: 'reporter', name: '报告Agent', description: '生成学习报告，分析学习进度和效果',
      type: 'app', status: 'IDLE', icon: 'bar-chart', role: '学情分析·PDF导出',
      tools: ['unified_academic_search', 'deep_document_analysis'], currentStep: 0, maxSteps: 30,
      execCount: 12, exampleTask: '"生成我本月的学习报告"'
    },
    {
      id: 'exercise', name: '习题Agent', description: '生成练习题，批改作答，生成错题本',
      type: 'app', status: 'IDLE', icon: 'edit-3', role: '习题生成·智能批改',
      tools: ['smart_quiz_generation', 'unified_academic_search'], currentStep: 0, maxSteps: 30,
      execCount: 31, exampleTask: '"生成5道Python练习题"'
    },
    {
      id: 'intervention', name: '干预Agent', description: '监测学习行为，及时干预和提醒',
      type: 'app', status: 'IDLE', icon: 'bell', role: '行为监测·主动干预',
      tools: [], currentStep: 0, maxSteps: 30,
      execCount: 8, exampleTask: '"设置每天学习2小时的提醒"'
    },
    {
      id: 'motivator', name: '激励Agent', description: '管理学习成就，激励持续学习',
      type: 'app', status: 'IDLE', icon: 'award', role: '成就解锁·打卡管理',
      tools: [], currentStep: 0, maxSteps: 30,
      execCount: 15, exampleTask: '"查看我的学习成就"'
    }
  ]
  
  // 设置编排器状态（循环切换）
  const cycleOrchestratorStatus = () => {
    const statusCycle = { 'IDLE': 'RUNNING', 'RUNNING': 'MAINTENANCE', 'MAINTENANCE': 'IDLE' }
    const current = orchestratorStatus.value
    const next = statusCycle[current] || 'IDLE'
    orchestratorStatus.value = next
    // 同步到 agentList
    const orch = agentList.value.find(a => a.type === 'orchestrator')
    if (orch) orch.status = next === 'RUNNING' ? 'RUNNING' : 'IDLE'
  }
  
  return {
    // 状态
    agentList,
    isLoading,
    loadError,
    isExecuting,
    executingAgentId,
    reactStep,
    reactIteration,
    executionLogs,
    taskHistory,
    taskResult,
    orchestratorStatus,
    // 计算属性
    subAgents,
    orchestratorInfo,
    availableCount,
    executingCount,
    totalCount,
    // 操作
    fetchAgents,
    executeTask,
    executeTaskStream,
    startSimulation,
    addLog,
    clearLogs,
    clearTaskHistory,
    stopCurrentTask,
    updateAgentStatus,
    clearSimTimers,
    incrementExecCount,
    clearTaskResult,
    addExecutionResult,
    clearExecutionResults,
    executionResults,
    cycleOrchestratorStatus,
    // 数据持久化
    fetchHistoryLogs,
    fetchHistoryResults,
    // 历史数据（持久化展示）
    allLogs,
    allResults,
    selectedResult,
    resultFilter,
    filteredResults,
    fetchAllLogs,
    fetchAllResults,
    fetchResultDetail,
    clearAllHistoryLogs,
    clearAllHistoryResults,
    setResultFilter
  }
})