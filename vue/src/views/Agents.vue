<template>
  <div ref="pageRef" class="agents-page">

    <!-- ===== 顶部栏（纯净化）===== -->
    <header class="top-bar">
      <div class="top-bar-left">
        <h1 class="tb-title">🧠 智能体中心</h1>
      </div>
      <div class="top-bar-center" v-if="selectedAgent">
        <div class="tb-breadcrumb">
          <span class="tbb-item">{{ selectedAgent.icon }} {{ selectedAgent.name }}</span>
          <span class="tbb-sep">/</span>
          <span class="tbb-role">{{ selectedAgent.role }}</span>
        </div>
      </div>
      <div class="top-bar-right">
        <button class="tb-route-btn" @click="showRouterModal = true">
          ✨ 智能匹配
        </button>
        <span v-if="selectedAgent" class="tb-status" :class="selectedAgent.status">
          <span class="tb-dot"></span>
          {{ selectedAgent.status === 'available' ? '在线' : selectedAgent.status === 'executing' ? '执行中' : '离线' }}
        </span>
        <span class="tb-right-spacer"></span>
        <button class="tb-history-btn" @click="showHistoryDrawer = true">
          📋 历史
          <span v-if="successLogCount > 0" class="tb-badge">{{ successLogCount }}</span>
        </button>
      </div>
    </header>

    <!-- ===== 智能匹配弹窗 ===== -->
    <Transition name="fade">
      <div v-if="showRouterModal" class="router-modal-overlay" @click.self="showRouterModal = false">
        <div class="router-modal" @click.stop>
          <div class="router-modal-header">
            <h3>✨ 智能匹配</h3>
            <p>描述你想做的事，AI 自动为你分配最合适的智能体</p>
            <button class="router-modal-close" @click="showRouterModal = false">✕</button>
          </div>
          <div class="router-modal-body">
            <div class="rm-search">
              <input
                v-model="routerInput"
                class="rm-search-input"
                placeholder="例如：诊断我的 Python 水平 / 制定三个月学习计划..."
                @keydown.enter="handleSmartRouter"
              />
              <button class="rm-search-btn" :disabled="!routerInput.trim() || isRouterLoading" @click="handleSmartRouter">
                <span v-if="isRouterLoading" class="btn-spinner-sm"></span>
                <span v-else>🤖 匹配</span>
              </button>
            </div>
            <Transition name="fade-slide">
              <div v-if="routedAgent" class="rm-result">
                <div class="rmr-card">
                  <span class="rmr-icon">{{ routedAgent.icon }}</span>
                  <div class="rmr-info">
                    <span class="rmr-name">{{ routedAgent.name }}</span>
                    <span class="rmr-role">{{ routedAgent.role }}</span>
                  </div>
                  <button class="rmr-go" @click="goToAgent(routedAgent)">进入对话</button>
                </div>
              </div>
            </Transition>
            <div v-if="routerError" class="rm-error">{{ routerError }}</div>
            <div class="rm-quick">
              <div class="rmq-label">💡 快捷示例：</div>
              <div class="rmq-chips">
                <span class="rmq-chip" @click="routerInput = '诊断我的 Python 学习水平'">诊断我的 Python 水平</span>
                <span class="rmq-chip" @click="routerInput = '制定三个月 Java 学习计划'">制定三个月学习计划</span>
                <span class="rmq-chip" @click="routerInput = '解释一下什么是闭包'">解释闭包概念</span>
                <span class="rmq-chip" @click="routerInput = '生成本周学习报告'">生成学习报告</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ===== 主布局 ===== -->
    <div class="main-layout">
      <!-- 左侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-search">
          <input v-model="sidebarSearch" placeholder="搜索智能体..." />
        </div>
        <div class="sidebar-cats">
          <span class="sc-tab" :class="{ active: activeCategory === 'all' }" @click="activeCategory = 'all'">全部</span>
          <span class="sc-tab" :class="{ active: activeCategory === 'core' }" @click="activeCategory = 'core'">核心</span>
          <span class="sc-tab" :class="{ active: activeCategory === 'support' }" @click="activeCategory = 'support'">辅助</span>
        </div>
        <div class="sidebar-list">
          <div
            v-for="agent in filteredAgents" :key="agent.id"
            class="sli-item"
            :class="{ active: isAgentOpenAndActive(agent.id), executing: getAgentStatus(agent.id) === 'executing' }"
            @click="openOrSwitchTab(agent)"
          >
            <span class="sli-icon">{{ agent.icon }}</span>
            <div class="sli-info">
              <span class="sli-name">{{ agent.name }}</span>
              <span class="sli-role">{{ agent.role }}</span>
            </div>
            <span class="sli-dot" :class="getAgentStatus(agent.id)"></span>
          </div>
        </div>
      </aside>

      <!-- 右侧工作区（多标签页 + 沉浸式对话） -->
      <main class="workspace">

        <!-- ===== 标签页栏 ===== -->
        <div v-if="openTabs.length > 0" class="tab-bar">
          <div class="tab-list">
            <div
              v-for="tab in openTabs"
              :key="tab.agentId"
              class="tab-item"
              :class="{ active: activeTabId === tab.agentId, executing: getAgentStatus(tab.agentId) === 'executing' }"
              @click="activeTabId = tab.agentId"
            >
              <span class="tab-icon">{{ getAgentIcon(tab.agentId) }}</span>
              <span class="tab-name">{{ getAgentName(tab.agentId) }}</span>
              <span v-if="getAgentStatus(tab.agentId) === 'executing'" class="tab-dot"></span>
              <button class="tab-close" @click.stop="closeTab(tab.agentId)">✕</button>
            </div>
          </div>
          <div class="tab-add" @click="showRouterModal = true" title="打开新智能体">
            <span>+</span>
          </div>
        </div>

        <!-- ===== 无标签 → 欢迎页 ===== -->
        <div v-if="openTabs.length === 0" class="welcome">
          <div class="welcome-bg">
            <div class="welcome-orb"></div>
          </div>
          <div class="welcome-content">
            <div class="welcome-icon">🤖</div>
            <h2 class="welcome-title">智能体工作台</h2>
            <p class="welcome-desc">点击左侧打开智能体，支持多标签并行对话</p>
            <div class="welcome-cards">
              <div class="wl-card" @click="openTabByRecommend('diagnosis')">
                <span class="wl-card-icon">🔍</span>
                <div class="wl-card-text">
                  <span class="wl-card-title">诊断我的学习水平</span>
                  <span class="wl-card-desc">全面评估能力画像，发现薄弱点</span>
                </div>
              </div>
              <div class="wl-card" @click="openTabByRecommend('planner')">
                <span class="wl-card-icon">🗺️</span>
                <div class="wl-card-text">
                  <span class="wl-card-title">制定学习计划</span>
                  <span class="wl-card-desc">生成个性化学习路径与资源推荐</span>
                </div>
              </div>
              <div class="wl-card" @click="openTabByRecommend('tutor')">
                <span class="wl-card-icon">💬</span>
                <div class="wl-card-text">
                  <span class="wl-card-title">概念答疑解惑</span>
                  <span class="wl-card-desc">苏格拉底式引导，深入理解知识</span>
                </div>
              </div>
              <div class="wl-card" @click="openTabByRecommend('reporter')">
                <span class="wl-card-icon">📊</span>
                <div class="wl-card-text">
                  <span class="wl-card-title">生成学习报告</span>
                  <span class="wl-card-desc">分析学习进度，提供数据洞察</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ===== 当前激活标签 → 沉浸式对话 ===== -->
        <div v-else class="chat" :key="activeTabId">
          <div v-if="currentTab" class="chat-content">
            <!-- 对话头部（精简） -->
            <div class="chat-header">
              <div class="ch-left">
                <span class="ch-icon">{{ currentTab.agent.icon }}</span>
                <div>
                  <h2 class="ch-name">{{ currentTab.agent.name }}</h2>
                  <p class="ch-role">{{ currentTab.agent.role }}</p>
                </div>
              </div>
              <div class="ch-right">
                <button class="ch-tool-btn" @click="openTaskDialog(currentTab.agent)">
                  🚀 开始任务
                </button>
              </div>
            </div>

            <!-- 工具链（紧凑） -->
            <div class="chat-tools">
              <span class="cht-label">工具链</span>
              <span v-for="tool in currentTab.agent.tools" :key="tool" class="cht-tag">{{ tool }}</span>
            </div>

            <!-- 对话消息区（占据最大空间） -->
            <div class="chat-messages" ref="chatRef">
              <div v-if="currentTab.messages.length" class="cmsg-list">
                <div v-for="(msg, mi) in currentTab.messages" :key="mi" class="cmsg" :class="msg.role">
                  <span class="cmsg-avatar">{{ msg.role === 'user' ? '👤' : currentTab.agent.icon }}</span>
                  <div class="cmsg-body">
                    <div class="cmsg-bubble">
                      <p class="cmsg-text">{{ msg.content }}</p>
                    </div>
                    <span class="cmsg-time">{{ msg.time }}</span>
                  </div>
                </div>
                <!-- 执行中打字指示器 -->
                <div v-if="isAgentExecuting(activeTabId)" class="cmsg typing">
                  <span class="cmsg-avatar">{{ currentTab.agent.icon }}</span>
                  <div class="cmsg-body">
                    <div class="cmsg-bubble typing-bubble">
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="cmsg-empty">
                <div class="cmsg-empty-icon">💬</div>
                <p>开始与 <strong>{{ currentTab.agent.name }}</strong> 的对话</p>
                <span>选择一个快捷指令或输入问题</span>
              </div>
            </div>

            <!-- 快捷指令 -->
            <div v-if="quickCommandsForAgent.length" class="chat-quick">
              <span
                v-for="cmd in quickCommandsForAgent"
                :key="cmd"
                class="cq-chip"
                @click="sendQuickCommandToCurrentTab(cmd)"
              >{{ cmd }}</span>
            </div>

            <!-- 输入区（固定底部） -->
            <div class="chat-input-area">
              <div class="chat-input">
                <input
                  v-model="chatInput"
                  class="ci-field"
                  :placeholder="'向 ' + currentTab.agent.name + ' 提问...'"
                  @keydown.enter="sendChatMessageToCurrentTab"
                />
                <button class="ci-btn" :disabled="!chatInput.trim()" @click="sendChatMessageToCurrentTab">
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- ===== 右侧: Orchestrator 编排面板 ===== -->
      <aside class="orchestrator-panel" :class="{ 'orchestrator--open': orchestratorPanelOpen }">
        <div class="orch-header">
          <div class="orch-title">
            <span class="orch-icon">🧠</span>
            <span>智能编排</span>
          </div>
          <button class="orch-toggle" @click="orchestratorPanelOpen = !orchestratorPanelOpen" title="收起编排面板">
            <ChevronRight />
          </button>
        </div>
        <div class="orch-messages" ref="orchestratorMessagesRef">
          <div v-if="orchestrationMessages.length === 0 && !isOrchestrating" class="orch-empty">
            <p>在这里输入复杂任务，Orchestrator 会自动拆解任务，调用多个 Agent <strong>并行执行</strong>，最后聚合结果</p>
            <div class="orch-hints">
              <span class="hint">示例：帮我规划一个 30 天的 Java 学习路线，并评估我的当前水平，最后生成练习题</span>
            </div>
          </div>
          <div v-for="msg in orchestrationMessages" :key="msg.id" class="orch-message" :class="msg.type">
            <div class="orch-msg-header">
              <span class="orch-msg-icon">{{ getOrchestrationIcon(msg.type) }}</span>
              <span class="orch-msg-title">{{ getOrchestrationTitle(msg) }}</span>
              <span v-if="msg.agentName" class="orch-msg-agent">{{ msg.agentName }}</span>
            </div>
            <div v-if="msg.content" class="orch-msg-content" v-html="formatOrchestrationContent(msg.content)"></div>
            <div v-if="msg.subTasks" class="orch-subtasks">
              <div v-for="task in msg.subTasks" :key="task.agentId" class="orch-subtask" :class="getSubTaskClass(task.agentId)">
                <span class="st-agent">{{ task.agentName }}</span>
                <span class="st-desc">{{ task.description }}</span>
                <span class="st-status" :class="subTaskStatusMap[task.agentId] || 'pending'">
                  {{ getSubTaskStatusLabel(subTaskStatusMap[task.agentId]) }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="isOrchestrating" class="orch-loading">
            <div class="spinner"></div>
            <span>编排执行中...</span>
          </div>
        </div>
        <div class="orch-input-area">
          <input
            v-model="orchestratorInput"
            class="orch-input"
            placeholder="输入复杂任务，Orchestrator 会自动多Agent调度..."
            @keydown.enter="sendOrchestration"
            :disabled="isOrchestrating"
          />
          <button class="orch-send" @click="sendOrchestration" :disabled="isOrchestrating || !orchestratorInput.trim()">
            {{ isOrchestrating ? '执行中...' : '发送' }}
          </button>
        </div>
      </aside>
    </div>

    <!-- 编排面板伸缩标签（独立于面板，始终可见） -->
    <button class="orch-tab" :class="{ 'orch-tab--open': orchestratorPanelOpen }" @click="orchestratorPanelOpen = !orchestratorPanelOpen" :title="orchestratorPanelOpen ? '收起编排面板' : '打开编排面板'">
      <ChevronLeft v-if="!orchestratorPanelOpen" :size="16" />
      <ChevronRight v-else :size="16" />
      <span v-if="!orchestratorPanelOpen" class="orch-tab-label">编排</span>
    </button>

    <!-- ===== 底部状态栏 ===== -->
    <footer class="status-bar">
      <span class="sb-item">
        <span class="sb-dot online"></span>
        {{ toolsStore.tools.length }} 个 MCP 工具在线
      </span>
      <span class="sb-divider"></span>
      <span class="sb-item">● {{ availableCount }} 个智能体就绪</span>
      <span class="sb-divider"></span>
      <span class="sb-item">📋 总执行 {{ totalExecCount }} 次</span>
      <span v-if="isAnyExecuting" class="sb-item sb-live">
        <span class="live-dot-pulse"></span>
        实时
      </span>
    </footer>

    <!-- ===== 历史抽屉 ===== -->
    <Transition name="drawer-slide">
      <div v-if="showHistoryDrawer" class="drawer-overlay" @click.self="showHistoryDrawer = false">
        <div class="history-drawer">
          <div class="hd-header">
            <h3 class="hd-title">📋 执行历史</h3>
            <button class="hd-close" @click="showHistoryDrawer = false">✕</button>
          </div>
          <div class="hd-toolbar">
            <div class="hd-tabs">
              <span class="hd-tab" :class="{ active: flowView === 'list' }" @click="switchFlowView('list')">全部</span>
              <span class="hd-tab" :class="{ active: flowView === 'trash' }" @click="switchFlowView('trash')">回收站</span>
            </div>
            <div class="hd-actions">
              <input v-model="flowKeyword" class="hd-search" placeholder="搜索..." />
              <button v-if="selectedLogIds.size > 0" class="hd-btn batch" @click.stop="batchDeleteSelected">🗑️ {{ selectedLogIds.size }}</button>
              <button class="hd-btn clear" :disabled="executionLogs.length === 0" @click.stop="handleClearLogs">清空</button>
            </div>
          </div>
          <div class="hd-body">
            <!-- 回收站视图 -->
            <div v-if="flowView === 'trash'" class="hd-trash">
              <div v-if="trashLoading" class="hd-line muted">⏳ 加载中...</div>
              <div v-else-if="trashError" class="hd-line error">加载失败 <button class="retry-btn" @click="loadTrashLogs">重试</button></div>
              <template v-else>
                <div v-for="log in trashLogs" :key="log.id" class="hd-log">
                  <span class="hdt time">{{ log.time }}</span>
                  <span class="hdt agent">{{ log.agentName }}</span>
                  <span class="hdt desc">{{ log.description }}</span>
                  <span class="hdt actions">
                    <button class="hda" @click="restoreTrashLog(log)">↩️</button>
                    <button class="hda danger" @click="hardDeleteTrashLog(log)">🔥</button>
                  </span>
                </div>
                <div v-if="trashLogs.length === 0" class="hd-empty">🗑️ 回收站是空的</div>
              </template>
            </div>
            <!-- 全部视图 -->
            <div v-else class="hd-logs">
              <div v-if="flowLoading" class="hd-line muted">⏳ 加载执行历史...</div>
              <div v-else-if="flowError" class="hd-line error">加载失败 <button class="retry-btn" @click="retryLoadHistory">🔄 重试</button></div>
              <template v-else>
                <div v-if="isAnyExecuting" class="hd-line live">
                  <span class="live-dot-pulse"></span>
                  <span>Agent 正在执行中...</span>
                </div>
                <div v-for="log in filteredLogs" :key="log.id" class="hd-log" :class="log.status" @click="selectLog(log)">
                  <label class="hdt check" @click.stop>
                    <input type="checkbox" :checked="selectedLogIds.has(log.id)" @change="toggleSelectLog(log)" />
                  </label>
                  <span class="hdt time">{{ log.time }}</span>
                  <span class="hdt agent" :style="{ color: agentColor(log.agentId) }">
                    {{ log.agentIcon || '🤖' }} {{ log.agentName }}
                  </span>
                  <span class="hdt badge" :class="log.stepType">{{ log.stepLabel }}</span>
                  <span class="hdt desc">{{ log.description }}</span>
                  <span v-if="log.status === 'executing'" class="hdt cursor">▌</span>
                </div>
                <div v-if="filteredLogs.length === 0 && executionLogs.length > 0" class="hd-empty">🔍 没有匹配的记录</div>
                <div v-if="executionLogs.length === 0" class="hd-empty">📭 暂无执行记录</div>
              </template>
              <div v-if="filteredLogs.length > 0" class="hd-footer">
                <span class="hd-count">共 {{ successLogCount }} 条记录</span>
                <label class="hd-check-all">
                  <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
                  全选
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ===== 执行详情弹窗 ===== -->
    <ExecutionDetailModal
      v-if="selectedLog"
      :log="selectedLog"
      :list="filteredLogs"
      :index="currentLogIndex"
      :fallback-generator="regenerateResultForLog"
      @close="closeDetailPanel"
      @navigate="navigateLog"
      @delete="confirmDeleteLog"
    />

    <!-- ===== 工具输入弹窗 ===== -->
    <div v-if="showToolDialog" class="dialog-overlay" @click="closeToolDialog">
      <div class="task-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-header-left">
            <span class="dialog-icon">{{ toolDialogConfig.icon }}</span>
            <div>
              <h3 class="dialog-name">{{ toolDialogConfig.title }}</h3>
              <p class="dialog-role">{{ toolDialogConfig.placeholder }}</p>
            </div>
          </div>
          <button class="dialog-close" @click="closeToolDialog">✕</button>
        </div>
        <div class="dialog-body">
          <div class="task-input-section">
            <label class="input-label">{{ toolDialogConfig.label }}</label>
            <textarea
              v-model="toolDialogValue"
              class="task-textarea"
              :placeholder="toolDialogConfig.placeholder"
              rows="3"
              ref="toolDialogInput"
              @keydown.enter.ctrl="confirmToolDialog"
            ></textarea>
            <div v-if="toolDialogError" class="tool-dialog-error">{{ toolDialogError }}</div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn cancel" @click="closeToolDialog">取消</button>
          <button class="dialog-btn primary" @click="confirmToolDialog">
            {{ toolDialogConfig.confirmText }}
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 删除确认对话框 ===== -->
    <DeleteConfirmDialog
      :visible="showDeleteDialog"
      :title="deleteDialogConfig.title"
      :message="deleteDialogConfig.message"
      :type="deleteDialogConfig.type"
      :show-soft-delete="deleteDialogConfig.showSoftDelete"
      :details="deleteDialogConfig.details"
      @cancel="handleDeleteCancel"
      @soft-delete="handleDeleteSoft"
      @hard-delete="handleDeleteHard"
    />

    <!-- ===== 任务分配对话框 ===== -->
    <div v-if="showTaskDialog" class="dialog-overlay" @click="closeTaskDialog">
      <div class="task-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-header-left">
            <span class="dialog-icon">{{ currentAgent?.icon }}</span>
            <div>
              <h3 class="dialog-name">{{ currentAgent?.name }}</h3>
              <p class="dialog-role">{{ currentAgent?.role }}</p>
            </div>
          </div>
          <button class="dialog-close" :disabled="isTaskExecuting" @click="closeTaskDialog">✕</button>
        </div>

        <div class="dialog-body">
          <div class="example-section">
            <div class="example-label">💡 示例任务 <span class="hint">点击自动填充</span></div>
            <div class="example-list">
              <div
                v-for="(ex, ei) in currentAgent?.examples || []" :key="ei"
                class="example-item"
                :class="{ active: taskInput === ex.text }"
                @click="taskInput = ex.text"
              >
                <span class="ex-cat">{{ ex.cat }}</span>
                <span class="ex-text">{{ ex.text }}</span>
              </div>
            </div>
          </div>

          <!-- Agent 专属选项 -->
          <div v-if="currentAgent?.id === 'diagnosis'" class="agent-opts">
            <button class="opt-btn" :disabled="isTaskExecuting" @click="quickDiagnosis">⚡ 快速诊断</button>
          </div>
          <div v-if="currentAgent?.id === 'planner'" class="agent-opts">
            <div class="opt-label">学习时长：</div>
            <div class="opt-group">
              <button v-for="opt in plannerOptions" :key="opt.value" class="opt-btn" :class="{ active: plannerDuration === opt.value }" :disabled="isTaskExecuting" @click="plannerDuration = opt.value; taskInput = opt.label">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'tutor'" class="agent-opts">
            <label class="toggle-row"><span>🔍 知识检索增强</span>
              <div class="toggle" :class="{ on: tutorRagEnabled }" @click="tutorRagEnabled = !tutorRagEnabled">
                <div class="toggle-knob"></div>
              </div>
            </label>
          </div>
          <div v-if="currentAgent?.id === 'reporter'" class="agent-opts">
            <div class="opt-label">报告类型：</div>
            <div class="opt-group">
              <button v-for="opt in reporterOptions" :key="opt.value" class="opt-btn" :class="{ active: reporterType === opt.value }" :disabled="isTaskExecuting" @click="reporterType = opt.value; taskInput = opt.label">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'exercise'" class="agent-opts">
            <div class="opt-label">难度：</div>
            <div class="opt-group">
              <button v-for="opt in difficultyOptions" :key="opt.value" class="opt-btn" :class="{ active: exerciseDifficulty === opt.value }" :disabled="isTaskExecuting" @click="exerciseDifficulty = opt.value">{{ opt.label }}</button>
            </div>
            <div class="opt-label">数量：</div>
            <div class="count-group">
              <button class="count-btn" :disabled="isTaskExecuting || exerciseCount <= 1" @click="exerciseCount--">−</button>
              <span class="count-val">{{ exerciseCount }}</span>
              <button class="count-btn" :disabled="isTaskExecuting || exerciseCount >= 20" @click="exerciseCount++">+</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'search'" class="agent-opts">
            <div class="opt-label">搜索范围：</div>
            <div class="opt-group">
              <button v-for="opt in searchRangeOptions" :key="opt.value" class="opt-btn" :class="{ active: searchRange === opt.value }" :disabled="isTaskExecuting" @click="searchRange = opt.value">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'knowledge'" class="agent-opts">
            <div class="opt-label">文档筛选：</div>
            <div class="opt-group">
              <button v-for="opt in docFilterOptions" :key="opt.value" class="opt-btn" :class="{ active: docFilter === opt.value }" :disabled="isTaskExecuting" @click="docFilter = opt.value">{{ opt.label }}</button>
            </div>
          </div>

          <div class="task-input-section">
            <label class="input-label">任务描述</label>
            <textarea v-model="taskInput" class="task-textarea" :placeholder="'为 ' + (currentAgent?.name || '') + ' 分配任务...'" rows="3" :disabled="isTaskExecuting"></textarea>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="dialog-btn cancel" :disabled="isTaskExecuting" @click="closeTaskDialog">取消</button>
          <button class="dialog-btn primary" :disabled="isTaskExecuting || !taskInput.trim()" @click="submitTask">
            <span v-if="isTaskExecuting" class="btn-spinner-sm"></span>
            <span v-else>🚀</span>
            {{ isTaskExecuting ? '执行中...' : '分配任务' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, onActivated, onDeactivated, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { cleanupDialogs } from '@/utils/modalHelper'
import { saveAgentExecution, getAllResults, clearAllResults, deleteResultById, deleteResultsBatch, getTrashResults, restoreResult, postStreamExecution, postOrchestrateExecution } from '@/api/agentApi'
import { getToolExecutionHistory, deleteToolExecution } from '@/api/toolsApi'
import { parseResultContent } from '@/utils/markdown'
import { securityFilter } from '@/utils/securityUtils'
import ExecutionDetailModal from '@/components/agent/ExecutionDetailModal.vue'
import DeleteConfirmDialog from '@/components/common/DeleteConfirmDialog.vue'
import { useToolsStore } from '@/stores/toolsStore'

const route = useRoute()
const router = useRouter()
const pageRef = ref(null)
const toolsStore = useToolsStore()

// ===== 新布局状态 =====
const selectedAgent = ref(null)
const sidebarSearch = ref('')
const activeCategory = ref('all')
const showHistoryDrawer = ref(false)
const showRouterModal = ref(false)
const chatInput = ref('')
const chatRef = ref(null)
const conversations = reactive({})

// ===== Orchestrator 编排面板状态 =====
const orchestratorPanelOpen = ref(false)
const orchestratorInput = ref('')
const isOrchestrating = ref(false)
const orchestrationMessages = ref([])
const orchestratorMessagesRef = ref(null)
const subTaskStatusMap = ref({})
let messageIdCounter = 0

// ===== 多标签页 - 支持多 Agent 并行对话 =====
const openTabs = ref([])
const activeTabId = ref(null)
const agentExecuting = ref({})
const agentAbortControllers = ref({})

const currentTab = computed(() => {
  if (!activeTabId.value) return null
  return openTabs.value.find(t => t.agentId === activeTabId.value) || null
})

const isAgentExecuting = (agentId) => !!agentExecuting.value[agentId]
const getAgentStatus = (agentId) => {
  if (agentExecuting.value[agentId]) return 'executing'
  const agent = agents.value.find(a => a.id === agentId)
  return agent?.status || 'offline'
}
const getAgentIcon = (agentId) => {
  if (agentId && agentId.startsWith('tool:')) return '🔧'
  return agents.value.find(a => a.id === agentId)?.icon || '🤖'
}
const getAgentName = (agentId) => agents.value.find(a => a.id === agentId)?.name || 'Unknown'

const isAgentOpenAndActive = (agentId) => {
  return openTabs.value.some(t => t.agentId === agentId) && activeTabId.value === agentId
}

const openOrSwitchTab = (agent) => {
  const existing = openTabs.value.find(t => t.agentId === agent.id)
  if (existing) {
    activeTabId.value = agent.id
    existing.lastActiveAt = Date.now()
    return
  }
  if (openTabs.value.length >= 8) {
    const nonExecuting = openTabs.value.filter(t => !agentExecuting.value[t.agentId])
    if (nonExecuting.length > 0) {
      const oldest = nonExecuting.sort((a, b) => a.lastActiveAt - b.lastActiveAt)[0]
      closeTab(oldest.agentId)
    }
  }
  openTabs.value.push({
    agentId: agent.id,
    agent: agent,
    messages: [],
    lastActiveAt: Date.now()
  })
  activeTabId.value = agent.id
  agentExecuting.value[agent.id] = false
  agentAbortControllers.value[agent.id] = null
  // 兼容 selectedAgent
  selectedAgent.value = agent
}

const openTabByRecommend = (type) => {
  const map = {
    diagnosis: 'diagnosis', planner: 'planner', tutor: 'tutor', reporter: 'reporter'
  }
  const agent = agents.value.find(a => a.id === map[type])
  if (agent) openOrSwitchTab(agent)
}

const closeTab = (agentId) => {
  if (agentExecuting.value[agentId] && agentAbortControllers.value[agentId]) {
    agentAbortControllers.value[agentId]?.abort()
  }
  const idx = openTabs.value.findIndex(t => t.agentId === agentId)
  if (idx === -1) return
  openTabs.value.splice(idx, 1)
  delete agentExecuting.value[agentId]
  delete agentAbortControllers.value[agentId]
  if (activeTabId.value === agentId) {
    activeTabId.value = openTabs.value.length > 0
      ? openTabs.value[Math.max(0, idx - 1)].agentId
      : null
    selectedAgent.value = activeTabId.value
      ? openTabs.value.find(t => t.agentId === activeTabId.value)?.agent || null
      : null
  }
}

const selectAgent = (agent) => {
  openOrSwitchTab(agent)
  showRouterModal.value = false
  routerInput.value = ''
  routedAgent.value = null
  routerError.value = null
}

// 欢迎页推荐卡片 → 打开对应 Agent 标签
const handleRecommend = (agentId) => {
  openTabByRecommend(agentId)
  showRouterModal.value = false
}

// 快捷指令计算属性
const quickCommandsForAgent = computed(() => {
  if (!currentTab.value) return []
  return currentTab.value.agent.quickCommands || []
})

// 发送消息到当前标签
const sendChatMessageToCurrentTab = () => {
  if (!currentTab.value || !chatInput.value.trim()) return
  sendMessageToTab(currentTab.value.agentId, chatInput.value.trim())
  chatInput.value = ''
}

const sendQuickCommandToCurrentTab = (cmd) => {
  if (!currentTab.value) return
  sendMessageToTab(currentTab.value.agentId, cmd)
}

const sendMessageToTab = async (agentId, content) => {
  const tab = openTabs.value.find(t => t.agentId === agentId)
  if (!tab) return
  tab.messages.push({
    role: 'user',
    content: content,
    time: formatTime()
  })
  // 自动滚动到底部
  nextTick(() => {
    if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
  })

  // 如果当前是 supervisor 智能体，走编排流程
  if (tab.agent.id === 'supervisor') {
    agentExecuting.value[agentId] = true
    try {
      await postOrchestrateExecution(content, {
        onOrchestrationStart: (data) => {
          tab.messages.push({ role: 'assistant', content: data.content, time: formatTime() })
        },
        onDecomposition: (data) => {
          const subTasks = data.subTasks || []
          const taskList = subTasks.map((t, i) => 
            `  ${i + 1}. **${t.agentName}**: ${t.description}`
          ).join('\n')
          tab.messages.push({
            role: 'assistant',
            content: `📋 **任务拆解完成**\n${taskList}`,
            time: formatTime()
          })
        },
        onSubtaskStart: (data) => {
          tab.messages.push({
            role: 'assistant',
            content: `⏳ **${data.agentName}** 开始执行: ${data.description}`,
            time: formatTime()
          })
        },
        onSubtaskDone: (data) => {
          tab.messages.push({
            role: 'assistant',
            content: `✅ **${data.agentName}** 完成\n${data.outputPreview || ''}`,
            time: formatTime()
          })
        },
        onSubtaskError: (data) => {
          tab.messages.push({
            role: 'assistant',
            content: `❌ **${data.agentName}** 执行失败: ${data.error}`,
            time: formatTime()
          })
        },
        onAggregating: (data) => {
          tab.messages.push({
            role: 'assistant', content: data.content, time: formatTime()
          })
        },
        onOrchestrationDone: (data) => {
          tab.messages.push({
            role: 'assistant',
            content: `📊 **最终综合报告**\n\n${data.content}`,
            time: formatTime()
          })
        },
        onError: (data) => {
          tab.messages.push({
            role: 'assistant',
            content: `❌ 编排失败: ${data.message}`,
            time: formatTime()
          })
        }
      })
    } catch (e) {
      tab.messages.push({
        role: 'assistant',
        content: `❌ 编排异常: ${e.message}`,
        time: formatTime()
      })
    } finally {
      agentExecuting.value[agentId] = false
      // 自动滚动到底部
      nextTick(() => {
        if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
      })
    }
  } else {
    // 原有逻辑：直接提交执行，不弹窗
    currentAgent.value = tab.agent
    taskInput.value = content
    setTimeout(() => { submitTask() }, 50)
  }
}

// 兼容原来的函数
const sendChatMessage = () => sendChatMessageToCurrentTab()
const sendQuickCommand = (cmd) => sendQuickCommandToCurrentTab(cmd)

const filteredAgents = computed(() => {
  let list = subAgents.value
  if (activeCategory.value === 'core') list = list.filter(a => isCoreAgent(a.id))
  else if (activeCategory.value === 'support') list = list.filter(a => !isCoreAgent(a.id))
  const kw = sidebarSearch.value.trim().toLowerCase()
  if (kw) list = list.filter(a => a.name.toLowerCase().includes(kw) || a.role.toLowerCase().includes(kw))
  return list
})

// ===== 折叠控制 =====
const toolsExpanded = ref(true)
const flowExpanded = ref(true)

// ===== 智能路由 =====
const routerInput = ref('')
const isRouterLoading = ref(false)
const routedAgent = ref(null)
const routerError = ref(null)

const handleSmartRouter = async () => {
  const input = routerInput.value.trim()
  if (!input) return
  isRouterLoading.value = true
  routerError.value = null
  routedAgent.value = null

  try {
    const { post } = await import('@/api/request')
    const result = await post('/agents/route', { input })
    if (result.success && result.data && result.data.agentId) {
      routedAgent.value = subAgents.value.find(a => a.id === result.data.agentId) || null
      if (!routedAgent.value) routerError.value = '未找到匹配的 Agent'
    } else {
      routerError.value = result.message || '路由失败'
    }
  } catch (e) {
    // 降级前端关键词匹配
    const lower = input.toLowerCase()
    const keywordMap = {
      diagnosis: ['诊断', '水平', '能力', '薄弱点', '测评', '评估'],
      planner: ['规划', '计划', '学习路径', '路线', '目标'],
      tutor: ['答疑', '解释', '教我', '问题', '问', '不懂'],
      reporter: ['报告', '总结', '分析', '导出'],
      exercise: ['练习', '习题', '题目', '作业', '批改'],
      search: ['搜索', '查找', '资源', '教程'],
      knowledge: ['知识', '文档', '检索', '知识库']
    }
    let found = false
    for (const [id, keywords] of Object.entries(keywordMap)) {
      if (keywords.some(k => lower.includes(k))) {
        routedAgent.value = subAgents.value.find(a => a.id === id) || null
        found = true
        break
      }
    }
    if (!found) routedAgent.value = subAgents.value[Math.floor(Math.random() * subAgents.value.length)]
  } finally {
    isRouterLoading.value = false
  }
}

const goToAgent = (agent) => {
  openOrSwitchTab(agent)
  showRouterModal.value = false
  routerInput.value = ''
  routedAgent.value = null
  routerError.value = null
  // 打开任务对话框
  openTaskDialog(agent)
}

// ===== 工具视图 =====
const REAL_TOOL_ICONS = {
  unified_academic_search: '🌐', deep_document_analysis: '📄',
  smart_quiz_generation: '✏️', academic_translation: '🌍', full_chain_learning: '🔗'
}

const toolCategories = computed(() => {
  const tools = toolsStore.tools
  if (!tools || tools.length === 0) return []
  const categories = {}
  const catNames = { input_search: '搜索工具', understanding_output: '理解工具', assessment_loop: '评估工具', system_debug: '系统工具', other: '其他工具' }
  const catIcons = { input_search: '🔍', understanding_output: '📖', assessment_loop: '📊', system_debug: '⚙️', other: '🔧' }
  for (const tool of tools) {
    const cat = tool.category || 'other'
    if (!categories[cat]) categories[cat] = { id: cat, name: catNames[cat] || cat, icon: catIcons[cat] || '🔧', count: 0, tools: [] }
    categories[cat].tools.push({
      id: tool.id, name: tool.name, icon: REAL_TOOL_ICONS[tool.id] || '🔧',
      status: tool.status === 'available' ? 'available' : 'unavailable',
      statusText: tool.status === 'available' ? '可用' : '不可用'
    })
    categories[cat].count++
  }
  return Object.values(categories)
})

const handleToolClick = async (tool) => {
  if (tool.status !== 'available') { ElMessage.warning(`工具「${tool.name}」当前不可用`); return }
  let toolParams = null
  try {
    if (tool.id === 'unified_academic_search') {
      const value = await openToolDialog({
        icon: '🌐',
        title: tool.name,
        placeholder: '例如：机器学习入门教程',
        label: '搜索关键词',
        confirmText: '搜索',
        validator: (val) => val && val.trim() ? true : '关键词不能为空'
      })
      if (!value) return
      toolParams = { query: value.trim(), searchInternal: true, searchWeb: true }
    } else if (tool.id === 'academic_translation') {
      const value = await openToolDialog({
        icon: '🔤',
        title: tool.name,
        placeholder: '请输入英文或中文文本',
        label: '翻译文本',
        confirmText: '翻译',
        validator: (val) => val && val.trim() ? true : '文本不能为空'
      })
      if (!value) return
      toolParams = { text: value.trim() }
    } else if (tool.id === 'smart_quiz_generation') {
      const value = await openToolDialog({
        icon: '📝',
        title: tool.name,
        placeholder: '例如：线性代数',
        label: '知识点或主题',
        confirmText: '生成',
        validator: (val) => val && val.trim() ? true : '主题不能为空'
      })
      if (!value) return
      toolParams = { topic: value.trim(), count: 5 }
    } else {
      toolParams = {}
    }
  } catch { return }

  try {
    const { toolsAPI } = await import('@/api/toolsApi')
    const result = await toolsAPI.executeTool(tool.id, toolParams)
    if (result.success) {
      ElMessage.success(`工具「${tool.name}」执行成功`)
      const content = typeof result.data === 'string' ? result.data : JSON.stringify(result.data, null, 2)
      ElMessageBox.alert(
        `<pre style="max-height:400px;overflow:auto;font-size:12px;white-space:pre-wrap;">${content}</pre>`,
        `${tool.icon || '🔧'} ${tool.name} - 执行结果`,
        { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
      ).catch(() => {})
    } else {
      ElMessage.error(result.message || `工具执行失败`)
    }
  } catch (e) {
    ElMessage.error(`工具执行异常: ${e.message || '请稍后重试'}`)
  }
}

// ===== Agent 配置 =====
const agentConfigs = [
  { id: 'diagnosis', name: '诊断Agent', icon: '🔍', role: '能力测评 · 画像构建 · 薄弱点挖掘', tools: ['智能测评出题', '全域学术检索'], avgTime: '2.3s', example: '💡 诊断我的 Python 学习水平', quickCommands: ['诊断我的Python水平', '评估我的能力画像'], examples: [{ cat: '能力测评', text: '诊断我的 Python 学习水平，找出薄弱点' }, { cat: '画像构建', text: '生成完整的用户画像报告，包含学习风格分析' }, { cat: '薄弱分析', text: '分析我当前的知识盲区，给出提升优先级' }, { cat: '能力测评', text: '对我进行前端开发能力综合测评' }] },
  { id: 'planner', name: '规划Agent', icon: '🗺️', role: '路径生成 · 动态调整 · 资源推荐', tools: ['全域学术检索', '全链路学习助手'], avgTime: '3.1s', example: '💡 制定3个月的Java学习计划', quickCommands: ['制定Python学习计划', '调整我的学习路径'], examples: [{ cat: '路径规划', text: '制定3个月的Java学习计划，从入门到进阶' }, { cat: '路径规划', text: '规划Python数据分析完整学习路径' }, { cat: '动态调整', text: '根据我当前的进度，动态调整学习计划' }, { cat: '资源推荐', text: '推荐适合初学者的机器学习入门资源' }] },
  { id: 'tutor', name: '答疑Agent', icon: '💬', role: '苏格拉底引导 · RAG检索 · 知识解答', tools: ['全域学术检索', '学术翻译'], avgTime: '2.8s', example: '💡 解释一下什么是闭包？', quickCommands: ['解释Python装饰器', '教我理解排序算法'], examples: [{ cat: '概念解释', text: '解释一下什么是闭包？用通俗的语言说明' }, { cat: '概念解释', text: '帮我理解Python装饰器的工作原理' }, { cat: '引导式教学', text: '用苏格拉底式引导法教我理解排序算法' }, { cat: '代码答疑', text: '这段代码为什么会报错？帮我排查问题' }] },
  { id: 'reporter', name: '报告Agent', icon: '📊', role: '学情分析 · 报告生成 · PDF导出', tools: ['深度文献解析', '全域学术检索'], avgTime: '4.2s', example: '💡 生成我本月的学习报告', quickCommands: ['生成本周学习报告', '分析我的学习趋势'], examples: [{ cat: '周报', text: '生成本周学习报告，包含时长和完成率' }, { cat: '月报', text: '生成我本月的学习报告，分析进步趋势' }, { cat: '分析建议', text: '分析本周学习进度和效果，给出改进建议' }, { cat: '月报', text: '对比上个月和这个月的学习数据变化' }] },
  { id: 'exercise', name: '习题Agent', icon: '✏️', role: '习题生成 · 智能批改 · 错题本', tools: ['智能测评出题'], avgTime: '2.5s', example: '💡 生成5道Python基础练习题', quickCommands: ['生成专项练习题', '批改我的作业'], examples: [{ cat: '习题生成', text: '生成5道Python基础练习题，包含难度标注' }, { cat: '习题生成', text: '出10道Java面试高频算法题' }, { cat: '智能批改', text: '批改我提交的代码作业，指出优化方向' }, { cat: '错题巩固', text: '根据我的错题记录，生成针对性练习' }] },
  { id: 'search', name: '搜索Agent', icon: '🌐', role: '联网搜索 · 资料检索 · 资源发现', tools: ['全域学术检索'], avgTime: '1.8s', example: '💡 搜索最新的Python教程', quickCommands: ['搜索机器学习教程', '查找相关学习资源'], examples: [{ cat: '全网搜索', text: '搜索2026年最新的Python学习教程推荐' }, { cat: '学术搜索', text: '搜索Java面试常见问题及解答思路' }, { cat: '教程搜索', text: '寻找适合初学者的数据结构与算法视频教程' }] },
  { id: 'knowledge', name: '知识检索Agent', icon: '📚', role: '文档检索 · 语义搜索 · 知识问答', tools: ['全域学术检索', '深度文献解析'], avgTime: '1.5s', example: '💡 查找Python基础相关内容', quickCommands: [], examples: [{ cat: '文档检索', text: '从知识库中查找Python基础语法相关内容' }, { cat: '语义搜索', text: '检索「如何优化SQL查询性能」的相关知识' }, { cat: '知识问答', text: '根据知识库回答：什么是数据库事务？' }] },
  { id: 'supervisor', name: '🧠 学习主管', icon: '🧠', role: '协调所有智能体协作完成复杂任务', tools: ['任务拆解', '智能派单', '并行执行', '结果聚合'], avgTime: '15s', example: '💡 从零学Java，3个月达到就业水平', quickCommands: ['从零学Java，3个月达到就业水平，帮我出完整方案', '系统学习Python数据分析，从入门到项目实战', '备战前端面试，1个月冲刺计划'], examples: [{ cat: '完整方案', text: '从零学Java，3个月达到就业水平，帮我出完整方案' }, { cat: '完整方案', text: '系统学习Python数据分析，从入门到项目实战' }, { cat: '面试冲刺', text: '备战前端面试，1个月冲刺计划' }, { cat: '完整方案', text: '我想学全栈开发，请安排从零到就业的完整路径' }] }
]

// ===== localStorage 持久化 =====
const LS_KEYS = { STATS: 'agent_stats' }
const loadStats = () => { try { const r = localStorage.getItem(LS_KEYS.STATS); return r ? JSON.parse(r) : {} } catch { return {} } }
const saveStats = (s) => { try { localStorage.setItem(LS_KEYS.STATS, JSON.stringify(s)) } catch {} }

const agentStats = reactive(loadStats())
const executionLogs = ref([])
const agents = ref(agentConfigs.map(cfg => ({ ...cfg, status: 'available', execCount: agentStats[cfg.id] || 0 })))
const subAgents = computed(() => agents.value)

// ===== 判断是否核心智能体 - 核心智能体占 2 格宽度 =====
const isCoreAgent = (agentId) => {
  return ['diagnosis', 'planner', 'tutor'].includes(agentId)
}

// ===== 计算属性 =====
const availableCount = computed(() => agents.value.filter(a => a.status === 'available').length)
const executingCount = computed(() => agents.value.filter(a => a.status === 'executing').length)
const isAnyExecuting = computed(() => executingCount.value > 0)
const totalExecCount = computed(() => executionLogs.value.filter(l => l.stepType === 'success' || l.stepType === 'error').length)

// ===== 执行历史 =====
const flowView = ref('list')
const flowKeyword = ref('')
const flowFilterStatus = ref('all')
const flowFilterAgent = ref('all')
const filteredLogs = ref([])
const selectedLogId = ref(null)
const selectedLog = computed(() => filteredLogs.value.find(log => log.id === selectedLogId.value) || null)
const currentLogIndex = computed(() => filteredLogs.value.findIndex(log => log.id === selectedLogId.value))
const flowLoading = ref(false)
const flowError = ref(false)
const successLogCount = computed(() => executionLogs.value.filter(l => l.stepType === 'success' || l.stepType === 'error').length)
const selectedLogIds = ref(new Set())
const deletableLogs = computed(() => filteredLogs.value.filter(l => l.resultId || l.sourceType === 'tool'))
const allSelected = computed(() => deletableLogs.value.length > 0 && deletableLogs.value.every(l => selectedLogIds.value.has(l.id)))

// 回收站
const trashLogs = ref([])
const trashLoading = ref(false)
const trashError = ref(false)

// 删除确认
const showDeleteDialog = ref(false)
const deleteDialogConfig = ref({ title: '删除执行记录', message: '', type: 'warning', showSoftDelete: true, details: [] })
let deleteDialogResolve = null
let deleteDialogLog = null
let deleteDialogAction = ''

// ===== 工具输入弹窗 =====
const showToolDialog = ref(false)
const toolDialogValue = ref('')
const toolDialogError = ref('')
const toolDialogConfig = ref({ icon: '🔧', title: '', placeholder: '', label: '', confirmText: '确认', validator: null })
let toolDialogResolve = null

const toolDialogInput = ref(null)

const openToolDialog = (config) => {
  return new Promise((resolve) => {
    toolDialogResolve = resolve
    toolDialogConfig.value = { icon: '🔧', title: '', placeholder: '', label: '', confirmText: '确认', validator: null, ...config }
    toolDialogValue.value = ''
    toolDialogError.value = ''
    showToolDialog.value = true
    nextTick(() => toolDialogInput.value?.focus())
  })
}

const confirmToolDialog = () => {
  const val = toolDialogValue.value.trim()
  const err = toolDialogConfig.value.validator?.(val)
  if (err !== true) { toolDialogError.value = err || '输入不能为空'; return }
  toolDialogError.value = ''
  showToolDialog.value = false
  toolDialogResolve?.(val)
  toolDialogResolve = null
}

const closeToolDialog = () => {
  showToolDialog.value = false
  toolDialogResolve?.(null)
  toolDialogResolve = null
}

// ===== 任务对话框 =====
const showTaskDialog = ref(false)
const currentAgent = ref(null)
const taskInput = ref('')
const isTaskExecuting = computed(() => currentAgent.value ? !!agentExecuting.value[currentAgent.value.id] : false)
const plannerDuration = ref('1个月')
const tutorRagEnabled = ref(true)
const reporterType = ref('周报')
const exerciseDifficulty = ref('中等')
const exerciseCount = ref(5)
const searchRange = ref('全网')
const docFilter = ref('全部')

const plannerOptions = [{ label: '1个月学习计划', value: '1个月' }, { label: '3个月学习计划', value: '3个月' }, { label: '6个月学习计划', value: '6个月' }]
const reporterOptions = [{ label: '周报', value: '周报' }, { label: '月报', value: '月报' }]
const difficultyOptions = [{ label: '简单', value: '简单' }, { label: '中等', value: '中等' }, { label: '困难', value: '困难' }]
const searchRangeOptions = [{ label: '全网', value: '全网' }, { label: '学术', value: '学术' }, { label: '教程', value: '教程' }]
const docFilterOptions = [{ label: '全部', value: '全部' }, { label: '最近上传', value: '最近上传' }]

const openTaskDialog = (agent) => {
  currentAgent.value = agent
  taskInput.value = ''
  plannerDuration.value = '1个月'
  tutorRagEnabled.value = true
  reporterType.value = '周报'
  exerciseDifficulty.value = '中等'
  exerciseCount.value = 5
  searchRange.value = '全网'
  docFilter.value = '全部'
  showTaskDialog.value = true
}

const closeTaskDialog = () => {
  if (isTaskExecuting.value) return
  showTaskDialog.value = false
  currentAgent.value = null
  taskInput.value = ''
}

const executeQuickCommand = (agent, cmd) => { currentAgent.value = agent; taskInput.value = cmd; showTaskDialog.value = true }
const quickDiagnosis = () => { taskInput.value = '快速诊断我的学习水平，生成完整的能力画像报告' }

// ===== 颜色映射 =====
const agentColorMap = { diagnosis: '#10b981', planner: '#14b8a6', tutor: '#3b82f6', reporter: '#f59e0b', exercise: '#ef4444', search: '#10b981', knowledge: '#a78bfa' }
const agentColor = (id) => agentColorMap[id] || '#71717a'

// ===== 格式化工具 =====
const formatTime = (dateStr) => {
  const d = dateStr ? new Date(dateStr) : new Date()
  if (isNaN(d.getTime())) return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const mapBackendStatus = (status) => {
  const s = String(status || 'completed').toLowerCase()
  if (['failed', 'error', 'failure'].includes(s)) return { status: 'error', stepType: 'error', stepLabel: '❌ 失败' }
  if (['running', 'pending', 'executing', 'processing'].includes(s)) return { status: 'executing', stepType: 'observe', stepLabel: '⏳ 执行中' }
  return { status: 'success', stepType: 'success', stepLabel: '✅ 任务完成' }
}

// ===== 提交任务 =====
const submitTask = async () => {
  if (!currentAgent.value || !taskInput.value.trim()) return
  const safetyResult = securityFilter.sanitize(taskInput.value)
  if (safetyResult.action === 'BLOCK') { ElMessage.error(safetyResult.message || '输入包含不允许的内容'); return }
  if (safetyResult.riskLevel === 'MEDIUM') console.warn('[Security] 潜在风险输入:', safetyResult.detectedTypes)

  const agent = agents.value.find(a => a.id === currentAgent.value.id)
  if (!agent) return

  // 获取当前 Agent
  const agentId = agent.id
  if (agentExecuting.value[agentId]) return

  agentExecuting.value[agentId] = true
  agent.status = 'executing'
  showTaskDialog.value = false
  flowExpanded.value = true

  let fullDescription = taskInput.value.trim()
  if (agent.id === 'exercise') fullDescription = `[${exerciseDifficulty.value}] ${fullDescription} (${exerciseCount.value}题)`
  else if (agent.id === 'search') fullDescription = `[${searchRange.value}] ${fullDescription}`
  else if (agent.id === 'knowledge') fullDescription = `[${docFilter.value}] ${fullDescription}`

  addFlowLog(agent, 'task', '📋 任务已提交', `→ ${fullDescription}`)
  const taskLogs = []
  await simulateExecution(agent, fullDescription, taskLogs)

  let savedResultId = null
  const resultPayload = generateResultPayload(agent, fullDescription)
  const normalizedOutput = normalizeOutput(resultPayload)
  const singleExecId = 'exec_' + Date.now() + '_' + agent.id
  const singleSessionId = 'session_' + Date.now()
  const phaseLogs = taskLogs.map((t) => ({ phase: t.type || 'info', content: t.content || '' }))

  latestResultsCache[agent.id] = { agentId: agent.id, agentName: agent.name, taskDescription: fullDescription, status: 'completed', resultType: resultPayload.type || 'default', result: resultPayload, duration: null, createdAt: new Date().toISOString(), id: savedResultId }

  try {
    const res = await saveAgentExecution({ agentId: agent.id, agentName: agent.name, taskDescription: fullDescription, sessionId: singleSessionId, executionId: singleExecId, result: resultPayload, duration: null, logs: phaseLogs })
    savedResultId = res?.data?.resultId || null
    latestResultsCache[agent.id].id = savedResultId
  } catch (err) { console.warn('[持久化] 保存失败:', err) }

  agent.execCount = (agent.execCount || 0) + 1
  agentStats[agent.id] = agent.execCount
  saveStats({ ...agentStats })

  addFlowLog(agent, 'success', '✅ 任务完成', `${agent.name} 成功执行：${fullDescription.length > 40 ? fullDescription.substring(0, 40) + '...' : fullDescription}`, null, { resultId: savedResultId }, normalizedOutput)

  // 将结果添加到对应标签的消息列表
  const tab = openTabs.value.find(t => t.agentId === agentId)
  if (tab && resultPayload.outputText) {
    tab.messages.push({
      role: 'assistant',
      content: resultPayload.outputText,
      time: formatTime()
    })
  }

  ElMessage.success(`${agent.name} 任务执行成功`)
  agent.status = 'available'
  agentExecuting.value[agentId] = false
}

// ===== Orchestrator 编排逻辑 =====
const getOrchestrationIcon = (type) => {
  switch (type) {
    case 'user': return '👤'
    case 'orchestration_start': return '🧠'
    case 'decomposition': return '📋'
    case 'subtask_start': return '⚡'
    case 'subtask_done': return '✅'
    case 'subtask_error': return '❌'
    case 'aggregating': return '🔄'
    case 'orchestration_done': return '🎯'
    case 'error': return '⚠️'
    default: return '💬'
  }
}

const getOrchestrationTitle = (msg) => {
  switch (msg.type) {
    case 'user': return '你的任务'
    case 'orchestration_start': return '🧠 Orchestrator 启动'
    case 'decomposition': return '📋 任务拆解'
    case 'subtask_start': return '⚡ 子任务执行'
    case 'subtask_done': return '✅ 子任务完成'
    case 'subtask_error': return '❌ 子任务失败'
    case 'aggregating': return '🔄 结果聚合'
    case 'orchestration_done': return '🎯 编排结果'
    case 'error': return '⚠️ 错误'
    default: return '💬 消息'
  }
}

const getSubTaskClass = (agentId) => {
  const status = subTaskStatusMap.value[agentId]
  return status === 'running' ? 'st-running' : status === 'done' ? 'st-done' : status === 'error' ? 'st-error' : ''
}

const getSubTaskStatusLabel = (status) => {
  switch (status) {
    case 'pending': return '⏳ 等待中'
    case 'running': return '🔄 执行中...'
    case 'done': return '✅ 完成'
    case 'error': return '❌ 失败'
    default: return '⏳ 等待中'
  }
}

const formatOrchestrationContent = (content) => {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
  return html
}

const addOrchestrationMessage = (type, data) => {
  const msg = { id: ++messageIdCounter, type, ...data, time: new Date().toLocaleTimeString() }
  orchestrationMessages.value.push(msg)
  scrollOrchestrationToBottom()
}

const scrollOrchestrationToBottom = () => {
  nextTick(() => {
    if (orchestratorMessagesRef.value) {
      orchestratorMessagesRef.value.scrollTop = orchestratorMessagesRef.value.scrollHeight
    }
  })
}

const sendOrchestration = async () => {
  const input = orchestratorInput.value.trim()
  if (!input || isOrchestrating.value) return

  addOrchestrationMessage('user', { content: input })
  orchestratorInput.value = ''

  if (!orchestratorPanelOpen.value) {
    orchestratorPanelOpen.value = true
  }

  isOrchestrating.value = true
  subTaskStatusMap.value = {}
  addOrchestrationMessage('orchestration_start', { content: `正在分析任务并拆解...` })

  try {
    await postOrchestrateExecution(input, {
      onDecomposition: (data) => {
        const subTasks = data.subTasks || []
        subTasks.forEach(t => { subTaskStatusMap.value[t.agentId] = 'pending' })
        addOrchestrationMessage('decomposition', {
          content: data.content || `已拆解为 ${subTasks.length} 个子任务`,
          subTasks
        })
      },
      onSubtaskStart: (data) => {
        subTaskStatusMap.value[data.agentId] = 'running'
        addOrchestrationMessage('subtask_start', {
          content: data.description || '',
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onSubtaskDone: (data) => {
        subTaskStatusMap.value[data.agentId] = 'done'
        addOrchestrationMessage('subtask_done', {
          content: data.outputPreview || `${data.agentName} 执行完成`,
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onSubtaskError: (data) => {
        subTaskStatusMap.value[data.agentId] = 'error'
        addOrchestrationMessage('subtask_error', {
          content: data.error || '执行失败',
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onAggregating: (data) => {
        addOrchestrationMessage('aggregating', {
          content: data.content || '正在聚合多个 Agent 的结果...'
        })
      },
      onOrchestrationDone: (data) => {
        addOrchestrationMessage('orchestration_done', {
          content: data.content || '编排完成',
          stats: { total: data.subTaskCount, success: data.successCount, error: data.errorCount }
        })
        isOrchestrating.value = false
      },
      onError: (data) => {
        addOrchestrationMessage('error', { content: data.message || data.error || '编排执行失败' })
        isOrchestrating.value = false
      }
    })
  } catch (e) {
    addOrchestrationMessage('error', { content: `编排执行异常: ${e.message}` })
    isOrchestrating.value = false
  }
}

// ===== SSE 流式执行 =====
const simulateExecution = async (agent, task, logCollector) => {
  const desc = task.length > 35 ? task.substring(0, 35) + '...' : task
  let stepCount = 0
  let isFinished = false
  const agentId = agent.id

  // 为当前任务创建独立的 AbortController
  const controller = new AbortController()
  agentAbortControllers.value[agentId] = controller
  const signal = controller.signal

  try {
    await postStreamExecution(agent.id, task, {
      onStart: () => { addFlowLog(agent, 'task', '🚀 启动', `任务已启动: ${desc}`, logCollector) },
      onThink: (data) => { stepCount = data.step || stepCount; addFlowLog(agent, 'think', '🤔 思考', data.content || data.thought || JSON.stringify(data), logCollector) },
      onAct: (data) => { const toolName = data.tool || data.toolName || '关联工具'; const argsStr = Object.keys(data.args || data.params || {}).length > 0 ? ` 参数: ${JSON.stringify(data.args || data.params || {})}` : ''; addFlowLog(agent, 'act', '⚡ 行动', `调用「${toolName}」${argsStr}`, logCollector) },
      onObserve: (data) => { const content = data.content || data.observation || ''; addFlowLog(agent, 'observe', '👁️ 观察', content || '执行成功', logCollector) },
      onReflect: (data) => { addFlowLog(agent, 'reflect', '🔄 反思', data.content || data.reflection || JSON.stringify(data), logCollector) },
      onReplan: (data) => { addFlowLog(agent, 'replan', '📝 重规划', `原因: ${data.reason || ''}\n新计划: ${data.alternative || ''}`, logCollector) },
      onComplete: (data) => { isFinished = true; addFlowLog(agent, 'success', '✅ 任务完成', data.output || data.message || '任务执行完成', logCollector, null, { type: 'default', displayTitle: `${agent.name} 执行完成`, summary: (data.output || '').substring(0, 80), outputText: data.output || data.message || '任务执行完成', generatedAt: new Date().toISOString(), status: 'success' }) },
      onError: (data) => { isFinished = true; addFlowLog(agent, 'error', '❌ 错误', data.message || data.error || '执行失败', logCollector) }
    }, signal)
  } catch (error) {
    if (error.name === 'AbortError') return
    addFlowLog(agent, 'error', '❌ 错误', error.message || '执行失败', logCollector)
  } finally { agentAbortControllers.value[agentId] = null }
}

// ===== 执行流日志 =====
const addFlowLog = (agent, stepType, stepLabel, description, logCollector, extraData, output) => {
  const log = {
    id: 'log_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    time: formatTime(), timestamp: new Date().toISOString(),
    agentName: agent.name, agentId: agent.id, agentIcon: agent.icon,
    stepType, stepLabel, description,
    status: stepType === 'success' ? 'success' : stepType === 'error' ? 'error' : 'executing',
    output: output || null, ...(extraData || {})
  }
  executionLogs.value.push(log)
  applyFlowFilter()
  if (logCollector) logCollector.push({ type: stepType, title: stepLabel, content: description, createdAt: log.timestamp })
  if (executionLogs.value.length > 100) executionLogs.value = executionLogs.value.slice(0, 100)
  return log
}

// ===== 执行历史操作 =====
const applyFlowFilter = () => {
  filteredLogs.value = executionLogs.value.filter(log => {
    const statusMatch = flowFilterStatus.value === 'all' || log.status === flowFilterStatus.value
    const agentMatch = flowFilterAgent.value === 'all' ? true : flowFilterAgent.value === 'tool' ? log.sourceType === 'tool' : log.agentId === flowFilterAgent.value
    const kw = (flowKeyword.value || '').trim().toLowerCase()
    const kwMatch = !kw || [log.agentName, log.description, log.taskDescription].some(t => (t || '').toLowerCase().includes(kw))
    return statusMatch && agentMatch && kwMatch
  })
}

const selectLog = (log) => { selectedLogId.value = selectedLogId.value === log.id ? null : log.id }
const closeDetailPanel = () => { selectedLogId.value = null }
const navigateLog = (direction) => {
  const idx = currentLogIndex.value
  if (direction === 'prev' && idx > 0) selectedLogId.value = filteredLogs.value[idx - 1].id
  else if (direction === 'next' && idx < filteredLogs.value.length - 1) selectedLogId.value = filteredLogs.value[idx + 1].id
}

const toggleSelectLog = (log) => { if (selectedLogIds.value.has(log.id)) selectedLogIds.value.delete(log.id); else selectedLogIds.value.add(log.id) }
const toggleSelectAll = () => { if (allSelected.value) selectedLogIds.value.clear(); else deletableLogs.value.forEach(l => selectedLogIds.value.add(l.id)) }

// ===== 删除操作 =====
const handleClearLogs = () => {
  if (executionLogs.value.length === 0) return
  deleteDialogAction = 'clearLogs'
  deleteDialogConfig.value = { title: '清空执行记录', message: '清空后可在「回收站」中恢复；也可以选择永久删除。', type: 'warning', showSoftDelete: true, details: [{ icon: '📋', text: `共 ${executionLogs.value.length} 条记录` }] }
  showDeleteDialog.value = true
}

const confirmDeleteLog = (log) => {
  if (!log) return
  if (!log.resultId && !log.toolRecordId) { ElMessage.warning('该记录尚未持久化，无法删除'); return }
  deleteDialogLog = log
  const isTool = log.sourceType === 'tool'
  deleteDialogConfig.value = {
    title: isTool ? '删除工具执行记录' : '删除执行记录',
    message: isTool ? '工具执行记录仅支持永久删除。' : '选择删除方式，「移至回收站」可恢复。',
    type: isTool ? 'danger' : 'warning',
    showSoftDelete: !isTool,
    details: [
      { icon: isTool ? '🔧' : '🤖', text: `${isTool ? '工具' : 'Agent'}：${log.agentName || '未知'}` },
      { icon: '🕐', text: `时间：${log.time || '未知'}` }
    ]
  }
  showDeleteDialog.value = true
}

const handleDeleteCancel = () => { showDeleteDialog.value = false; deleteDialogLog = null }

const doDeleteLog = async (log, mode) => {
  try {
    if (log.sourceType === 'tool') await deleteToolExecution(log.toolRecordId)
    else await deleteResultById(log.resultId, mode)
    executionLogs.value = executionLogs.value.filter(l => l.id !== log.id)
    selectedLogIds.value.delete(log.id)
    if (selectedLogId.value === log.id) selectedLogId.value = null
    applyFlowFilter()
    ElMessage.success(mode === 'hard' || log.sourceType === 'tool' ? '已永久删除' : '已移至回收站')
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || '请稍后重试'))
  }
}

let batchDeleteList = []
const batchDeleteSelected = () => {
  const list = filteredLogs.value.filter(l => selectedLogIds.value.has(l.id))
  if (list.length === 0) return
  batchDeleteList = list
  const agentCount = list.filter(l => l.sourceType !== 'tool').length
  const toolCount = list.filter(l => l.sourceType === 'tool').length
  const details = []
  if (agentCount > 0) details.push({ icon: '🤖', text: `Agent 记录：${agentCount} 条` })
  if (toolCount > 0) details.push({ icon: '🔧', text: `工具记录：${toolCount} 条` })
  deleteDialogConfig.value = { title: '批量删除', message: `已选择 ${list.length} 条记录。`, type: 'warning', showSoftDelete: true, details }
  showDeleteDialog.value = true
}

const handleDeleteSoft = async () => {
  showDeleteDialog.value = false
  const action = deleteDialogAction; deleteDialogAction = ''
  if (action === 'clearLogs') {
    try { await clearAllResults('soft'); executionLogs.value = []; filteredLogs.value = []; selectedLogId.value = null; selectedLogIds.value.clear(); ElMessage.success('已清空（可在回收站恢复）') } catch (e) { ElMessage.error('清空失败') }
  } else if (deleteDialogLog) { await doDeleteLog(deleteDialogLog, 'soft'); deleteDialogLog = null }
  else if (batchDeleteList.length > 0) { await doBatchDelete(batchDeleteList, 'soft'); batchDeleteList = [] }
}

const handleDeleteHard = async () => {
  showDeleteDialog.value = false
  const action = deleteDialogAction; const log = deleteDialogLog; deleteDialogAction = ''; deleteDialogLog = null
  if (action === 'clearLogs') {
    try { await clearAllResults('hard'); executionLogs.value = []; filteredLogs.value = []; selectedLogId.value = null; selectedLogIds.value.clear(); ElMessage.success('已永久清空') } catch (e) { ElMessage.error('清空失败') }
  } else if (action === 'hardDeleteTrash') {
    try { await deleteResultById(log.resultId, 'hard'); trashLogs.value = trashLogs.value.filter(l => l.id !== log.id); ElMessage.success('已彻底删除') } catch (e) { ElMessage.error('删除失败') }
  } else if (action === 'emptyTrash') {
    try { await deleteResultsBatch(trashLogs.value.map(l => l.resultId), 'hard'); trashLogs.value = []; ElMessage.success('回收站已清空') } catch (e) { ElMessage.error('清空失败') }
  } else if (log) { await doDeleteLog(log, 'hard') }
  else if (batchDeleteList.length > 0) { await doBatchDelete(batchDeleteList, 'hard'); batchDeleteList = [] }
}

const doBatchDelete = async (list, mode) => {
  const agentLogs = list.filter(l => l.sourceType !== 'tool' && l.resultId)
  const toolLogs = list.filter(l => l.sourceType === 'tool' && l.toolRecordId)
  try {
    if (agentLogs.length > 0) await deleteResultsBatch(agentLogs.map(l => l.resultId), mode)
    for (const t of toolLogs) await deleteToolExecution(t.toolRecordId)
    const ids = new Set(list.map(l => l.id))
    executionLogs.value = executionLogs.value.filter(l => !ids.has(l.id))
    if (selectedLogId.value && ids.has(selectedLogId.value)) selectedLogId.value = null
    selectedLogIds.value.clear()
    applyFlowFilter()
    ElMessage.success(mode === 'hard' ? `已永久删除 ${list.length} 条` : `已移至回收站 ${list.length} 条`)
  } catch (e) { ElMessage.error('批量删除失败') }
}

// ===== 回收站 =====
const switchFlowView = (view) => { flowView.value = view; selectedLogId.value = null; if (view === 'trash') loadTrashLogs() }

const loadTrashLogs = async () => {
  trashLoading.value = true; trashError.value = false
  try {
    const res = await getTrashResults()
    const data = Array.isArray(res?.data) ? res.data : (res?.data?.records || [])
    trashLogs.value = data.map(r => {
      const cfg = agentConfigs.find(a => a.id === r.agentId)
      const desc = r.taskDescription || ''
      return { id: r.id, time: formatTime(r.createdAt), timestamp: r.createdAt, agentName: r.agentName || r.agentId, agentId: r.agentId, agentIcon: cfg?.icon || '🤖', stepType: 'task', stepLabel: '🗑️ 已删除', description: `${r.agentName || r.agentId}：${desc.length > 40 ? desc.substring(0, 40) + '...' : desc || '(无描述)'}`, status: 'error', resultId: r.id }
    })
  } catch { trashLogs.value = []; trashError = true } finally { trashLoading.value = false }
}

const restoreTrashLog = async (log) => {
  try { await restoreResult(log.resultId); trashLogs.value = trashLogs.value.filter(l => l.id !== log.id); ElMessage.success('已恢复'); loadAllResultsFromBackend() } catch (e) { ElMessage.error('恢复失败') }
}

const hardDeleteTrashLog = (log) => {
  deleteDialogAction = 'hardDeleteTrash'; deleteDialogLog = log
  deleteDialogConfig.value = { title: '彻底删除', message: `彻底删除「${log.agentName}」的这条记录后不可恢复，确定？`, type: 'danger', showSoftDelete: false, details: [] }
  showDeleteDialog.value = true
}

const emptyTrash = () => {
  if (trashLogs.value.length === 0) return
  deleteDialogAction = 'emptyTrash'
  deleteDialogConfig.value = { title: '清空回收站', message: `将彻底删除 ${trashLogs.value.length} 条记录，不可恢复。`, type: 'danger', showSoftDelete: false, details: [] }
  showDeleteDialog.value = true
}

// ===== 结果缓存与加载 =====
const latestResultsCache = reactive({})
const allResultsFromBackend = ref([])

const retryLoadHistory = () => { loadAllResultsFromBackend() }

const regenerateResultForLog = (agentId, taskDescription) => {
  try {
    const agent = agents.value.find(a => a.id === agentId)
    if (!agent) return null
    const payload = generateResultPayload(agent, taskDescription || '')
    if (!payload || payload.type === 'default' || !payload.outputText) return null
    return payload
  } catch { return null }
}

const normalizeOutput = (payload) => {
  if (!payload) return null
  if (typeof payload === 'string') return { content: payload, fullData: null, raw: payload }
  if ('content' in payload && 'fullData' in payload) return payload
  const result = payload.data?.result || payload.result || payload.output || payload.summary || payload.content || ''
  return { content: result || payload.message || '', fullData: payload.data || payload, raw: payload }
}

const toToolLog = (r) => {
  const failed = r.status === 'error' || r.status === 'failed'
  const running = r.status === 'running' || r.status === 'pending'
  const status = failed ? 'error' : running ? 'executing' : 'success'
  const stepType = failed ? 'error' : running ? 'observe' : 'success'
  const stepLabel = failed ? '❌ 失败' : running ? '⏳ 执行中' : '✅ 完成'
  let paramText = ''
  if (r.params && typeof r.params === 'object' && Object.keys(r.params).length > 0) {
    const first = Object.entries(r.params).slice(0, 2).map(([k, v]) => `${k}=${typeof v === 'string' ? v : JSON.stringify(v)}`).join(', ')
    paramText = first.length > 30 ? first.substring(0, 30) + '...' : first
  }
  return { id: 'tool_' + r.id, time: formatTime(r.createdAt), timestamp: r.createdAt, agentName: r.toolName || r.toolId || '工具', agentId: 'tool:' + (r.toolId || 'unknown'), agentIcon: '🔧', stepType, stepLabel, description: `${r.toolName || r.toolId} 执行${paramText ? '：' + paramText : ''}`, status, output: r.result ? normalizeOutput(r.result) : null, resultId: null, sourceType: 'tool', toolRecordId: r.id }
}

const loadAllResultsFromBackend = async () => {
  flowLoading.value = true; flowError.value = false
  try {
    const res = await getAllResults()
    const list = Array.isArray(res?.data) ? res.data : (res?.data?.content || res?.data?.records || [])
    if (res && list.length >= 0) {
      const formatted = list.map(r => ({ agentId: r.agentId, agentName: r.agentName || r.agentId, taskDescription: r.taskDescription || '', status: r.status || 'completed', resultType: r.resultType || r.type || 'default', result: parseResultContent(r.resultContent || r.result, r.resultType || r.type), duration: r.duration || 0, createdAt: r.createdAt || r.created_at }))
      allResultsFromBackend.value = formatted
      formatted.forEach(r => { if (!latestResultsCache[r.agentId]) latestResultsCache[r.agentId] = r })

      const execCountByAgent = {}
      list.forEach(r => { execCountByAgent[r.agentId] = (execCountByAgent[r.agentId] || 0) + 1 })
      agents.value.forEach(a => { a.execCount = execCountByAgent[a.id] || 0; agentStats[a.id] = a.execCount })
      saveStats(agentStats)

      const historyLogs = list.slice().reverse().map(r => {
        let output = null
        const rawContent = r.output || r.resultContent
        if (rawContent) { try { output = normalizeOutput(JSON.parse(rawContent)) } catch { output = normalizeOutput(rawContent) } }
        const cfg = agentConfigs.find(a => a.id === r.agentId)
        const desc = r.taskDescription || ''
        const st = mapBackendStatus(r.status)
        return { id: r.id, time: formatTime(r.createdAt), timestamp: r.createdAt, agentName: r.agentName || r.agentId, agentId: r.agentId, agentIcon: cfg?.icon || '🤖', stepType: st.stepType, stepLabel: st.stepLabel, description: `${r.agentName || r.agentId} ${st.status === 'success' ? '成功执行' : st.status === 'error' ? '执行失败' : '执行中'}：${desc.length > 40 ? desc.substring(0, 40) + '...' : desc}`, taskDescription: desc, status: st.status, output, outputText: output?.content || (typeof rawContent === 'string' ? rawContent : ''), resultId: r.id }
      })

      let toolLogs = []
      try { const toolRes = await getToolExecutionHistory(0, 200); toolLogs = (toolRes?.data?.records || []).map(toToolLog) } catch {}

      const mergedHistory = [...historyLogs, ...toolLogs].sort((a, b) => {
        if (a.status === 'executing' && b.status !== 'executing') return -1
        if (a.status !== 'executing' && b.status === 'executing') return 1
        return String(b.timestamp || '').localeCompare(String(a.timestamp || ''))
      })

      executionLogs.value = executionLogs.value.filter(l => !l.resultId && !l.toolRecordId)
      executionLogs.value = [...mergedHistory, ...executionLogs.value]
      applyFlowFilter()
      return formatted
    }
  } catch (e) { console.warn('从后端加载结果失败:', e); flowError.value = true } finally { flowLoading.value = false }
  return allResultsFromBackend.value
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// ===== 轮询 =====
let pollInterval = null
const startPolling = () => { if (pollInterval) return; pollInterval = setInterval(() => { if (isAnyExecuting.value) loadAllResultsFromBackend() }, 3000) }
const stopPolling = () => { if (pollInterval) { clearInterval(pollInterval); pollInterval = null } }

// ===== 生成结果载荷（简化版，保留完整业务逻辑） =====
const generateResultPayload = (agent, task) => {
  const now = new Date().toISOString()
  const summary = (text) => text.length > 50 ? text.substring(0, 50) + '...' : text
  const trimQuery = (t) => t.replace(/^\[[^\]]*\]\s*/, '')
  const query = task

  if (agent.id === 'knowledge') {
    const q = trimQuery(task)
    const blocks = [
      { source: 'Python入门指南.md', title: '变量与数据类型', score: 95, content: 'Python 变量不需要显式声明类型。\n• 整数：a = 10\n• 浮点数：b = 3.14\n• 字符串：c = "Hello"' },
      { source: 'Python核心语法.md', title: '条件判断语句', score: 88, content: 'if-elif-else 依次检查条件。' }
    ]
    const result = `关于「${q}」的知识库检索结果：\n\n${blocks.map((b, i) => `${i + 1}. 📄 来源：${b.source}\n${b.content}`).join('\n\n')}`
    return { type: 'knowledge', displayTitle: '知识库检索结果', summary: `检索到 ${blocks.length} 个相关知识块`, outputText: result, outputJson: { query: q, total: blocks.length, blocks }, generatedAt: now, status: 'success', data: { query: q, result, source: '知识库', confidence: 0.95 }, message: '任务已成功执行' }
  }

  if (agent.id === 'search') {
    const q = trimQuery(task)
    const results = [{ title: 'Python 3 官方教程', url: 'docs.python.org', snippet: 'Python 官方文档。', relevance: 0.92 }, { title: '廖雪峰Python教程', url: 'liaoxuefeng.com', snippet: '通俗易懂的 Python 入门教程。', relevance: 0.75 }]
    const result = `「${q}」联网搜索结果：\n\n${results.map((r, i) => `${i + 1}. 📄 ${r.title}\n   💡 ${r.snippet}`).join('\n\n')}`
    return { type: 'search', displayTitle: '联网搜索结果', summary: `共找到 ${results.length} 条相关结果`, outputText: result, outputJson: { query: q, total: results.length, results }, generatedAt: now, status: 'success', data: { query: q, result, source: '搜索引擎', confidence: 0.88 }, message: '任务已成功执行' }
  }

  if (agent.id === 'diagnosis') {
    const dimensions = [{ name: 'Python基础', score: 85, level: '良好' }, { name: '数据结构', score: 60, level: '需加强' }, { name: '算法思维', score: 45, level: '薄弱' }, { name: '项目实践', score: 30, level: '薄弱' }]
    const weaknesses = [{ name: '面向对象编程', priority: '高' }, { name: '算法与数据结构', priority: '高' }]
    const result = `📊 诊断报告 - Python学习水平\n\n📈 整体水平：中级（62分）\n\n⚠️ 薄弱环节：\n${weaknesses.map(w => `  • ${w.name}`).join('\n')}`
    return { type: 'diagnosis', displayTitle: '学习水平诊断报告', summary: `整体水平：中级（62分），发现 ${weaknesses.length} 个薄弱环节`, outputText: result, outputJson: { level: '中级', score: 62, dimensions, weaknesses }, generatedAt: now, status: 'success', data: { query, result, source: '能力测评题库', confidence: 0.92 }, message: '任务已成功执行' }
  }

  if (agent.id === 'planner') {
    const phases = [{ phase: 1, title: '基础入门', duration: '第1-4周', goals: ['掌握Java核心语法'], tasks: ['完成 10 个基础编程练习'] }, { phase: 2, title: '进阶实践', duration: '第5-8周', goals: ['具备独立开发能力'], tasks: ['开发一个图书管理系统'] }]
    const result = `学习计划（${plannerDuration.value}）：\n\n${phases.map(p => `📌 Phase ${p.phase}：${p.title}（${p.duration}）\n  🎯 目标：${p.goals.join('；')}\n  📝 任务：${p.tasks.join('；')}`).join('\n\n')}`
    return { type: 'plan', displayTitle: '学习计划', summary: `${plannerDuration.value}（${phases.length} 个阶段）`, outputText: result, outputJson: { duration: plannerDuration.value, phases }, generatedAt: now, status: 'success', data: { query, result, source: '学习路径算法', confidence: 0.9 }, message: '任务已成功执行' }
  }

  if (agent.id === 'tutor') {
    const topic = trimQuery(task)
    const result = `💡 问题：${summary(topic)}\n\n📖 概念解释：\n\n该概念属于编程核心知识。\n\n🔍 核心要点：\n  1. 定义与原理\n  2. 实际应用\n  3. 常见误区\n\n📝 代码示例：\n\`\`\`python\ndef example():\n    return "Hello, World!"\n\`\`\``
    return { type: 'qa', displayTitle: '答疑解惑', summary: summary(topic), outputText: result, outputJson: null, generatedAt: now, status: 'success', data: { query: task, result, source: '知识库', confidence: 0.85 }, message: '任务已成功执行' }
  }

  if (agent.id === 'reporter') {
    const metrics = [{ label: '总学习时长', value: '42.5h', change: '+15%' }, { label: '完成率', value: '87%' }]
    const result = `${reporterType.value}学习报告：\n\n📈 学习概览\n${metrics.map(m => `  • ${m.label}：${m.value}${m.change ? `（较上周 ${m.change}）` : ''}`).join('\n')}`
    return { type: 'report', displayTitle: `${reporterType.value}学习报告`, summary: `${reporterType.value}：${metrics[0].value}学习时长`, outputText: result, outputJson: { title: `${reporterType.value}学习报告`, metrics }, generatedAt: now, status: 'success', data: { query, result, source: '学习行为数据', confidence: 0.93 }, message: '任务已成功执行' }
  }

  if (agent.id === 'exercise') {
    const questions = [{ question: '在Python中，哪个关键字用于定义函数？', answer: 'B', explanation: 'def 是定义函数的关键字' }]
    const result = `已生成 ${questions.length} 道${exerciseDifficulty.value}难度练习题：\n\n${questions.map((q, i) => `#${i + 1} ${q.question}\n  🔍 答案：${q.answer}（${q.explanation}）`).join('\n\n')}`
    return { type: 'exercise', displayTitle: `练习题（${questions.length}道）`, summary: `已生成 ${questions.length} 道${exerciseDifficulty.value}难度题`, outputText: result, outputJson: { questions }, generatedAt: now, status: 'success', data: { query, result, source: '智能出题引擎', confidence: 0.9 }, message: '任务已成功执行' }
  }

  return { type: 'default', displayTitle: `${agent.name} 执行完成`, summary: `任务「${summary(task)}」已完成`, outputText: `任务「${summary(task)}」已完成执行。`, outputJson: null, generatedAt: now, status: 'success', data: { query, result: `任务「${summary(task)}」已完成`, source: '本地执行引擎', confidence: 0.7 }, message: '任务已成功执行' }
}

// ===== 生命周期 =====
onMounted(() => {
  loadAllResultsFromBackend()
  startPolling()
  toolsStore.fetchTools().catch(() => {})
  toolsStore.fetchToolStats().catch(() => {})
  if (route.query.flow === 'history') { flowExpanded.value = true }
  if (route.query.tab === 'trash') switchFlowView('trash')
})

onUnmounted(() => { cleanupDialogs(); stopPolling() })
onDeactivated(() => { stopPolling() })
onActivated(() => { loadAllResultsFromBackend(); startPolling(); if (flowView.value === 'trash') loadTrashLogs() })
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

/* ===== 页面容器 ===== */
.agents-page {
  position: relative; height: 100vh;
  color: $text-primary; overflow-x: clip;
  display: flex; flex-direction: column;
  background: transparent;
}

/* ===== 顶部栏（纯净化）===== */
.top-bar {
  position: relative; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 10px 20px;
  background: rgba($bg-surface, 0.6);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba($accent-indigo, 0.15);
  flex-shrink: 0;
}
.top-bar-left { flex-shrink: 0; }
.top-bar-center { flex: 1; display: flex; justify-content: center; }
.top-bar-right { display: flex; align-items: center; gap: 10px; flex: 1; justify-content: flex-end; }
.tb-right-spacer { flex: 1; }

.tb-title {
  font-size: 1rem; font-weight: 700; margin: 0;
  white-space: nowrap;
}
.tb-breadcrumb {
  display: flex; align-items: center; gap: 8px;
}
.tbb-item {
  font-size: 0.88rem; font-weight: 600; color: $text-primary;
}
.tbb-sep {
  font-size: 0.8rem; color: $text-muted; opacity: 0.5;
}
.tbb-role {
  font-size: 0.72rem; color: $text-muted;
}
.tb-route-btn {
  padding: 6px 14px; background: rgba($accent-teal, 0.35);
  border: 1px solid rgba($accent-teal, 0.5); border-radius: 8px;
  color: $accent-teal; font-size: 0.78rem; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans; backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba($accent-teal, 0.15);
  &:hover { border-color: rgba($accent-teal, 0.7); background: rgba($accent-teal, 0.5); box-shadow: 0 4px 16px rgba($accent-teal, 0.25); transform: translateY(-1px); }
}
.tb-status {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.72rem; padding: 4px 10px; border-radius: 6px;
  background: rgba($bg-elevated, 0.3);
  &.available { color: $color-success; .tb-dot { background: $color-success; } }
  &.executing { color: $color-warning; .tb-dot { background: $color-warning; } }
  &.offline { color: $text-muted; .tb-dot { background: $text-muted; } }
}
.tb-dot { width: 6px; height: 6px; border-radius: 50%; }
.tb-history-btn {
  position: relative; display: flex; align-items: center; gap: 4px;
  padding: 6px 14px; background: rgba($accent-teal, 0.35);
  border: 1px solid rgba($accent-teal, 0.5); border-radius: 8px;
  color: $accent-teal; font-size: 0.8rem; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans; backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba($accent-teal, 0.15);
  &:hover { border-color: rgba($accent-teal, 0.7); background: rgba($accent-teal, 0.5); box-shadow: 0 4px 16px rgba($accent-teal, 0.25); transform: translateY(-1px); }
}
.tb-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 16px; height: 16px; padding: 0 4px;
  display: flex; align-items: center; justify-content: center;
  background: $accent-teal; color: #ffffff;
  font-size: 0.6rem; font-weight: 700; border-radius: 8px;
}

/* ===== 智能匹配弹窗 ===== */
.router-modal-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.router-modal {
  width: 520px; max-width: 90vw;
  background: $bg-surface;
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: modalEnter 0.2s ease;
}
.router-modal-header {
  padding: 18px 20px; border-bottom: 1px solid rgba($accent-indigo, 0.12);
  h3 { margin: 0 0 4px; font-size: 1rem; font-weight: 700; }
  p { margin: 0; font-size: 0.78rem; color: $text-muted; }
}
.router-modal-close {
  position: absolute; top: 16px; right: 16px;
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid rgba($accent-indigo, 0.1); border-radius: 6px;
  color: $text-muted; cursor: pointer; font-size: 0.8rem;
  &:hover { background: $bg-elevated; }
}
.router-modal-body {
  padding: 16px 20px;
}
.rm-search {
  display: flex; gap: 8px; margin-bottom: 12px;
}
.rm-search-input {
  flex: 1; padding: 10px 14px;
  background: rgba($bg-elevated, 0.3); border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 10px; color: $text-primary; font-size: 0.82rem; outline: none;
  font-family: $font-sans;
  &::placeholder { color: $text-placeholder; }
  &:focus { border-color: rgba($accent-indigo, 0.25); }
}
.rm-search-btn {
  padding: 10px 20px; background: rgba($accent-indigo, 0.1);
  border: none; border-radius: 10px; color: $accent-indigo;
  font-size: 0.8rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans; white-space: nowrap;
  &:hover:not(:disabled) { background: rgba($accent-indigo, 0.2); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.rm-result { margin: 12px 0; }
.rmr-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.15); border-radius: 10px;
}
.rmr-icon { font-size: 1.8rem; }
.rmr-info { flex: 1; }
.rmr-name { display: block; font-size: 0.9rem; font-weight: 700; color: $text-primary; }
.rmr-role { display: block; font-size: 0.72rem; color: $text-muted; }
.rmr-go {
  padding: 6px 16px; background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.25); border-radius: 8px;
  color: $accent-indigo; font-size: 0.78rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans;
  &:hover { background: rgba($accent-indigo, 0.25); }
}
.rm-error {
  margin: 8px 0; padding: 8px 12px;
  background: rgba($color-danger, 0.08); border: 1px solid rgba($color-danger, 0.15);
  border-radius: 8px; color: $color-danger; font-size: 0.78rem;
}
.rm-quick {
  margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba($accent-indigo, 0.1);
}
.rmq-label { font-size: 0.75rem; color: $text-muted; margin-bottom: 8px; }
.rmq-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.rmq-chip {
  padding: 4px 10px; background: rgba($bg-elevated, 0.3);
  border: 1px solid rgba($accent-indigo, 0.1); border-radius: 6px;
  font-size: 0.72rem; color: $text-muted; cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: rgba($accent-indigo, 0.2); color: $text-secondary; background: $bg-elevated; }
}

/* ===== fade 动画 ===== */
.fade-enter-active, .fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.fade-leave-from, .fade-enter-to { opacity: 1; }

/* ===== 主布局 ===== */
.main-layout {
  position: relative; z-index: 1;
  display: flex; flex: 1; overflow: hidden;
  min-height: 0;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 240px; flex-shrink: 0;
  display: flex; flex-direction: column;
  background: rgba($bg-surface, 0.3);
  border-right: 1px solid rgba($accent-indigo, 0.12);
  padding: 12px 0;
}
.sidebar-search {
  padding: 0 12px 10px;
  input {
    width: 100%; padding: 8px 12px;
    background: rgba($bg-elevated, 0.4);
    border: 1px solid rgba($accent-indigo, 0.1); border-radius: 8px;
    color: $text-primary; font-size: 0.78rem; outline: none;
    font-family: $font-sans;
    &::placeholder { color: $text-placeholder; }
    &:focus { border-color: rgba($accent-indigo, 0.25); }
  }
}
.sidebar-cats {
  display: flex; gap: 4px; padding: 0 12px 10px;
}
.sc-tab {
  padding: 4px 10px; border-radius: 6px;
  font-size: 0.72rem; color: $text-muted; cursor: pointer;
  transition: all 0.2s; font-weight: 500;
  &:hover { color: $text-secondary; background: rgba($bg-elevated, 0.3); }
  &.active { color: $accent-indigo; background: rgba($accent-indigo, 0.08); }
}
.sidebar-list {
  flex: 1; overflow-y: auto; padding: 0 6px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba($text-muted, 0.15); border-radius: 3px; }
}
.sli-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; margin-bottom: 2px;
  border-radius: 8px; cursor: pointer;
  transition: all 0.15s;
  &:hover { background: rgba($bg-elevated, 0.4); }
  &.active {
    background: rgba($accent-indigo, 0.06);
    border: 1px solid rgba($accent-indigo, 0.12);
    margin: -1px 0 1px; padding: 9px 11px;
  }
  &.executing { .sli-dot { animation: pulse 1.5s ease-in-out infinite; } }
}
.sli-icon { font-size: 1.1rem; width: 30px; text-align: center; flex-shrink: 0; }
.sli-info { flex: 1; min-width: 0; }
.sli-name { display: block; font-size: 0.82rem; font-weight: 600; color: $text-primary; line-height: 1.3; }
.sli-role { display: block; font-size: 0.65rem; color: $text-muted; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sli-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
  &.available { background: $color-success; }
  &.executing { background: $color-warning; }
  &.offline { background: $text-muted; }
}

/* ===== 右侧工作区（沉浸式对话）===== */
.workspace {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden;
  position: relative;
  .chat { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
  .chat-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
}

/* ===== 标签页栏 ===== */
.tab-bar {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 12px;
  background: rgba($bg-surface, 0.6);
  border-bottom: 1px solid rgba($accent-indigo, 0.12);
  flex-shrink: 0;
  overflow-x: auto;
  &::-webkit-scrollbar { height: 2px; }
  &::-webkit-scrollbar-thumb { background: rgba($text-muted, 0.15); border-radius: 2px; }
}
.tab-list {
  flex: 1; display: flex; align-items: center; gap: 4px;
  min-width: 0;
}
.tab-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px 5px 8px;
  background: rgba($bg-elevated, 0.2);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  min-width: 0; cursor: pointer; transition: all 0.15s;
  &.active {
    background: $bg-surface;
    border-color: rgba($accent-indigo, 0.2);
    margin-bottom: -1px;
    box-shadow: 0 -1px 6px rgba($accent-indigo, 0.06);
  }
  &.executing { .tab-name { padding-right: 4px; } }
  &:hover:not(.active) { background: rgba($bg-elevated, 0.35); }
}
.tab-icon { font-size: 0.8rem; flex-shrink: 0; }
.tab-name {
  font-size: 0.72rem; color: $text-secondary;
  max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  .tab-item.active & { color: $text-primary; font-weight: 500; }
}
.tab-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: $color-warning;
  animation: pulseDot 1.5s ease-in-out infinite;
}
.tab-close {
  width: 16px; height: 16px; display: flex; align-items: center; justify-content: center;
  border-radius: 3px; background: transparent; border: none;
  font-size: 0.6rem; line-height: 1; color: $text-muted; cursor: pointer;
  &:hover { background: rgba($text-muted, 0.15); color: $text-primary; }
}
.tab-add {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
  border-radius: 4px; border: 1px dashed rgba($accent-indigo, 0.15);
  cursor: pointer; transition: all 0.2s; flex-shrink: 0;
  span { font-size: 0.9rem; color: $text-muted; line-height: 1; }
  &:hover { border-color: rgba($accent-indigo, 0.3); background: rgba($bg-elevated, 0.3); }
}

/* ===== 欢迎页 ===== */
.welcome {
  flex: 1; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.welcome-bg { position: absolute; inset: 0; pointer-events: none; }
.welcome-orb {
  position: absolute; width: 400px; height: 400px;
  top: 50%; left: 50%; transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba($accent-indigo, 0.04) 0%, transparent 70%);
  border-radius: 50%; filter: blur(60px);
  animation: warpPulse 6s ease-in-out infinite;
}
@keyframes warpPulse {
  0%,100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.3); opacity: 1; }
}
.welcome-content {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; max-width: 480px; padding: 20px;
}
.welcome-icon {
  font-size: 3.5rem; filter: drop-shadow(0 0 30px rgba($accent-indigo, 0.2));
  animation: floatY 3s ease-in-out infinite;
}
@keyframes floatY {
  0%,100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.welcome-title { font-size: 1.3rem; font-weight: 700; margin: 0; color: $text-primary; }
.welcome-desc { font-size: 0.8rem; color: $text-muted; margin: 0; text-align: center; }
.welcome-cards {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
  margin-top: 8px; width: 100%;
}
.wl-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; background: rgba($bg-surface, 0.5);
  border: 1px solid rgba($accent-indigo, 0.1); border-radius: 12px;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: rgba($accent-indigo, 0.2); background: rgba($bg-surface, 0.8); transform: translateY(-1px); }
}
.wl-card-icon { font-size: 1.4rem; flex-shrink: 0; }
.wl-card-text { display: flex; flex-direction: column; min-width: 0; }
.wl-card-title { font-size: 0.78rem; font-weight: 600; color: $text-primary; }
.wl-card-desc { font-size: 0.65rem; color: $text-muted; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ===== 沉浸式对话 ===== */
.chat {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; height: 100%;
}
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px 8px; flex-shrink: 0;
}
.ch-left { display: flex; align-items: center; gap: 10px; }
.ch-icon { font-size: 1.4rem; }
.ch-name { font-size: 0.95rem; font-weight: 700; margin: 0 0 1px; color: $text-primary; }
.ch-role { font-size: 0.68rem; color: $text-muted; margin: 0; }
.ch-right { display: flex; align-items: center; gap: 8px; }
.ch-tool-btn {
  padding: 6px 14px; background: rgba($bg-elevated, 0.4);
  border: 1px solid rgba($accent-indigo, 0.1); border-radius: 8px;
  color: $text-secondary; font-size: 0.75rem; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans;
  &:hover { border-color: rgba($accent-indigo, 0.2); background: $bg-elevated; }
}

/* 工具链标签 */
.chat-tools {
  display: flex; align-items: center; gap: 6px;
  margin: 0 20px 8px; padding: 6px 10px;
  background: rgba($bg-elevated, 0.15); border-radius: 6px;
  flex-shrink: 0;
}
.cht-label { font-size: 0.6rem; color: $text-muted; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; }
.cht-tag { padding: 2px 7px; background: rgba($bg-elevated, 0.2); border-radius: 999px; font-size: 0.65rem; color: $text-secondary; }

/* ===== 对话消息区（占据最大空间）===== */
.chat-messages {
  flex: 1; overflow-y: auto; padding: 8px 20px 4px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba($text-muted, 0.15); border-radius: 4px; }
}
.cmsg-list { display: flex; flex-direction: column; gap: 12px; }
.cmsg {
  display: flex; gap: 10px; max-width: 85%;
  animation: msgSlide 0.2s ease;
  &.user { align-self: flex-end; flex-direction: row-reverse; }
  &.assistant { align-self: flex-start; }
  &.typing { align-self: flex-start; }
}
@keyframes msgSlide { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.cmsg-avatar {
  width: 28px; height: 28px; display: flex; align-items: center;
  justify-content: center; font-size: 1rem; flex-shrink: 0;
  margin-top: 4px;
}
.cmsg-body { display: flex; flex-direction: column; gap: 3px; }
.cmsg-bubble {
  padding: 10px 14px; border-radius: 14px;
  background: rgba($bg-elevated, 0.25);
  .cmsg.user & { background: rgba($accent-indigo, 0.08); }
  &.typing-bubble { background: rgba($bg-elevated, 0.2); padding: 12px 18px; display: flex; gap: 4px; align-items: center; }
}
.cmsg-text { font-size: 0.82rem; color: $text-primary; margin: 0; line-height: 1.5; white-space: pre-wrap; }
.cmsg-time { font-size: 0.6rem; color: $text-muted; padding: 0 2px; }

/* 打字指示器 */
.typing-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: $text-muted; animation: typingBounce 1.4s ease-in-out infinite;
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}
@keyframes typingBounce {
  0%,60%,100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 空状态 */
.cmsg-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; padding: 60px 20px; color: $text-muted;
}
.cmsg-empty-icon { font-size: 2.5rem; opacity: 0.4; }
.cmsg-empty p { font-size: 0.85rem; margin: 0; }
.cmsg-empty p strong { color: $text-secondary; }
.cmsg-empty span { font-size: 0.72rem; }

/* ===== 快捷指令 ===== */
.chat-quick {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding: 4px 20px 8px; flex-shrink: 0;
}
.cq-chip {
  padding: 5px 12px; background: rgba($bg-elevated, 0.25);
  border: 1px solid rgba($accent-indigo, 0.1); border-radius: 8px;
  font-size: 0.72rem; color: $text-muted; cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: rgba($accent-indigo, 0.2); color: $text-secondary; background: $bg-elevated; }
}

/* ===== 输入区（固定底部）===== */
.chat-input-area {
  padding: 0 20px 14px; flex-shrink: 0;
}
.chat-input {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 4px 4px 14px;
  background: rgba($bg-elevated, 0.2);
  border: 1px solid rgba($accent-indigo, 0.1); border-radius: 12px;
  transition: all 0.2s;
  &:focus-within { border-color: rgba($accent-indigo, 0.2); }
}
.ci-field {
  flex: 1; padding: 10px 4px; background: transparent;
  border: none; color: $text-primary; font-size: 0.82rem; outline: none;
  font-family: $font-sans;
  &::placeholder { color: $text-placeholder; }
}
.ci-btn {
  padding: 8px 18px; background: rgba($accent-indigo, 0.1);
  border: none; border-radius: 8px; color: $accent-indigo;
  font-size: 0.78rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s; font-family: $font-sans; white-space: nowrap;
  &:hover:not(:disabled) { background: rgba($accent-indigo, 0.2); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

/* ===== 底部状态栏 ===== */
.status-bar {
  position: relative; z-index: 10;
  display: flex; align-items: center; gap: 8px;
  padding: 6px 20px;
  background: rgba($bg-surface, 0.5);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba($accent-indigo, 0.15);
  flex-shrink: 0;
}
.sb-item {
  font-size: 0.7rem; color: $text-muted;
  display: flex; align-items: center; gap: 4px;
}
.sb-dot { width: 6px; height: 6px; border-radius: 50%; &.online { background: $color-success; } }
.sb-divider { width: 1px; height: 12px; background: rgba($accent-indigo, 0.15); }
.sb-live { color: $color-success; }
.live-dot-pulse {
  width: 6px; height: 6px; border-radius: 50%;
  background: $color-success; animation: pulse 1.5s ease-in-out infinite;
}

/* ===== 历史抽屉 ===== */
.drawer-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(0, 0, 0, 0.4);
  display: flex; justify-content: flex-end;
}
.history-drawer {
  width: 420px; max-width: 90vw;
  height: 100%; background: $bg-surface;
  display: flex; flex-direction: column;
  box-shadow: -8px 0 40px rgba(0, 0, 0, 0.3);
}
.hd-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid rgba($accent-indigo, 0.12);
}
.hd-title { font-size: 0.95rem; font-weight: 700; margin: 0; }
.hd-close {
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid rgba($accent-indigo, 0.1); border-radius: 6px;
  color: $text-muted; cursor: pointer; font-size: 0.75rem;
  &:hover { background: $bg-elevated; }
}
.hd-toolbar {
  padding: 12px 20px; border-bottom: 1px solid rgba($accent-indigo, 0.12);
}
.hd-tabs {
  display: flex; gap: 4px; margin-bottom: 8px;
}
.hd-tab {
  padding: 4px 12px; border-radius: 6px;
  font-size: 0.75rem; color: $text-muted; cursor: pointer; font-weight: 500;
  &.active { color: $accent-indigo; background: rgba($accent-indigo, 0.08); }
}
.hd-actions { display: flex; gap: 6px; }
.hd-search {
  flex: 1; padding: 6px 10px;
  background: rgba($bg-elevated, 0.3); border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 6px; color: $text-primary; font-size: 0.75rem; outline: none;
  font-family: $font-sans;
  &::placeholder { color: $text-placeholder; }
}
.hd-btn {
  padding: 6px 10px; border: 1px solid rgba($accent-indigo, 0.1); border-radius: 6px;
  background: transparent; color: $text-muted; font-size: 0.7rem; cursor: pointer;
  font-family: $font-sans; white-space: nowrap;
  &:hover { background: $bg-elevated; }
  &.batch { color: $color-warning; border-color: rgba($color-warning, 0.2); }
  &.clear { color: $color-danger; border-color: rgba($color-danger, 0.15); }
}
.hd-body {
  flex: 1; overflow-y: auto; padding: 8px 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba($text-muted, 0.15); border-radius: 3px; }
}
.hd-log {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 20px; font-size: 0.7rem; cursor: pointer;
  transition: background 0.15s;
  &:hover { background: rgba($bg-elevated, 0.3); }
  &.success { border-left: 2px solid $color-success; }
  &.error { border-left: 2px solid $color-danger; }
  &.executing { border-left: 2px solid $color-warning; }
}
.hdt {
  &.check { flex-shrink: 0; input { cursor: pointer; } }
  &.time { color: $text-muted; flex-shrink: 0; min-width: 50px; }
  &.agent { flex-shrink: 0; min-width: 80px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &.badge { flex-shrink: 0; padding: 1px 6px; border-radius: 4px; font-size: 0.6rem; font-weight: 600; }
  &.desc { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $text-secondary; }
  &.cursor { color: $color-success; animation: blink 1s step-end infinite; }
  &.actions { display: flex; gap: 4px; flex-shrink: 0; }
}
.hd-log .hdt.badge {
  &.success, &.observe { background: rgba($color-success, 0.1); color: $color-success; }
  &.error { background: rgba($color-danger, 0.1); color: $color-danger; }
  &.task { background: rgba($accent-indigo, 0.1); color: $accent-indigo; }
  &.think, &.reflect, &.replan, &.act { background: rgba($color-warning, 0.1); color: $color-warning; }
}
.hda {
  padding: 2px 6px; border: none; background: transparent;
  cursor: pointer; font-size: 0.75rem; opacity: 0.6;
  &:hover { opacity: 1; }
  &.danger:hover { color: $color-danger; }
}
.hd-empty {
  text-align: center; padding: 40px 20px; color: $text-muted; font-size: 0.78rem;
}
.hd-line {
  padding: 8px 20px; font-size: 0.72rem;
  &.muted { color: $text-muted; }
  &.error { color: $color-danger; }
  &.live { color: $color-success; display: flex; align-items: center; gap: 6px; }
}
.retry-btn {
  padding: 2px 8px; background: transparent; border: 1px solid currentColor;
  border-radius: 4px; color: inherit; cursor: pointer; font-size: 0.72rem; margin-left: 4px;
  font-family: $font-sans;
}
.hd-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 20px; border-top: 1px solid rgba($accent-indigo, 0.12);
  font-size: 0.7rem; color: $text-muted;
}
.hd-count { font-size: 0.7rem; }
.hd-check-all { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.hd-check-all input { cursor: pointer; }

/* ===== 抽屉动画 ===== */
.drawer-slide-enter-active, .drawer-slide-leave-active {
  transition: all 0.25s ease;
}
.drawer-slide-enter-from, .drawer-slide-leave-to {
  opacity: 0;
  .history-drawer { transform: translateX(100%); }
}

/* ===== 任务对话框 ===== */
.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px); display: flex; align-items: center;
  justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease;
}
.task-dialog {
  width: 640px; max-width: 90vw; max-height: 85vh;
  background: rgba($bg-surface, 0.95);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba($accent-indigo, 0.05);
  display: flex; flex-direction: column; animation: modalEnter 0.25s ease;
}
.dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px; border-bottom: 1px solid rgba($accent-indigo, 0.08); flex-shrink: 0;
}
.dialog-header-left { display: flex; align-items: center; gap: 12px; }
.dialog-icon { font-size: 1.6rem; }
.dialog-name { font-size: 16px; font-weight: 700; color: $text-primary; margin: 0 0 2px; }
.dialog-role { font-size: 0.75rem; color: $text-muted; margin: 0; }
.dialog-close {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: rgba($accent-indigo, 0.06); border: none; border-radius: 8px;
  font-size: 14px; cursor: pointer; color: $text-muted; transition: all 0.15s;
  &:hover { background: rgba($accent-indigo, 0.12); color: $text-primary; }
}

.dialog-body { padding: 18px 22px; overflow-y: auto; flex: 1; }
.example-section { margin-bottom: 16px; }
.example-label { font-size: 0.78rem; font-weight: 600; color: $text-secondary; margin-bottom: 8px; }
.hint { font-weight: 400; color: $text-placeholder; }
.example-list { display: flex; flex-direction: column; gap: 4px; }
.example-item {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 14px; background: rgba($accent-indigo, 0.03);
  border: 1px solid rgba($accent-indigo, 0.06); border-radius: 10px;
  cursor: pointer; transition: all 0.2s; font-size: 0.8rem;
  &:hover { background: rgba($accent-indigo, 0.06); border-color: rgba($accent-indigo, 0.12); }
  &.active { background: rgba($accent-indigo, 0.1); border-color: rgba($accent-indigo, 0.25); color: $accent-indigo; }
}
.ex-cat { font-size: 0.65rem; padding: 2px 8px; background: rgba($accent-violet, 0.1); color: $accent-violet; border-radius: 6px; flex-shrink: 0; font-weight: 500; }
.ex-text { color: $text-secondary; }

.agent-opts { margin-bottom: 14px; }
.opt-label { font-size: 0.78rem; color: $text-secondary; margin-bottom: 6px; }
.opt-group { display: flex; gap: 6px; flex-wrap: wrap; }
.opt-btn {
  padding: 6px 14px; border-radius: 8px; font-size: 0.78rem;
  background: rgba($accent-indigo, 0.04); border: 1px solid rgba($accent-indigo, 0.08);
  color: $text-secondary; cursor: pointer; transition: all 0.2s; font-family: $font-sans;
  &:hover { color: $accent-indigo; border-color: rgba($accent-indigo, 0.2); }
  &.active { background: rgba($accent-indigo, 0.1); border-color: rgba($accent-indigo, 0.3); color: $accent-indigo; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.count-group { display: flex; align-items: center; gap: 8px; }
.count-btn {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: rgba($accent-indigo, 0.06); border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 8px; color: $text-secondary; font-size: 1rem; cursor: pointer;
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.count-val { font-size: 0.9rem; font-weight: 700; color: $text-primary; min-width: 24px; text-align: center; font-family: $font-mono; }

.toggle-row { display: flex; align-items: center; justify-content: space-between; font-size: 0.82rem; color: $text-secondary; }
.toggle {
  width: 42px; height: 24px; border-radius: 12px; cursor: pointer;
  background: rgba($accent-indigo, 0.12); position: relative; transition: background 0.2s;
  &.on { background: rgba($accent-indigo, 0.5); }
}
.toggle-knob {
  width: 20px; height: 20px; border-radius: 50%; background: $text-primary;
  position: absolute; top: 2px; left: 2px; transition: transform 0.2s;
  .toggle.on & { transform: translateX(18px); }
}

.task-input-section { margin-top: 4px; }
.input-label { display: block; font-size: 0.78rem; font-weight: 600; color: $text-secondary; margin-bottom: 6px; }
.task-textarea {
  width: 100%; padding: 12px 14px;
  background: rgba($accent-indigo, 0.04); border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 10px; color: $text-primary; font-size: 0.85rem;
  resize: vertical; min-height: 72px; outline: none; font-family: $font-sans;
  &::placeholder { color: $text-placeholder; }
  &:focus { border-color: rgba($accent-indigo, 0.35); box-shadow: 0 0 0 3px rgba($accent-indigo, 0.06); }
  &:disabled { opacity: 0.5; }
}
.tool-dialog-error {
  margin-top: 8px; padding: 8px 12px;
  background: rgba($color-danger, 0.08); border: 1px solid rgba($color-danger, 0.15);
  border-radius: 8px; color: $color-danger; font-size: 0.78rem;
}

.dialog-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 14px 22px; border-top: 1px solid rgba($accent-indigo, 0.08); flex-shrink: 0;
}
.dialog-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 20px; border-radius: 10px; font-weight: 600; font-size: 0.82rem;
  cursor: pointer; transition: all 0.2s; border: none; font-family: $font-sans;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &.cancel { background: rgba($accent-indigo, 0.06); color: $text-secondary; &:hover { background: rgba($accent-indigo, 0.12); } }
  &.primary {
    background: linear-gradient(135deg, rgba($accent-indigo, 0.85), rgba($accent-indigo, 0.85));
    color: $text-primary;
    box-shadow: 0 2px 12px rgba($accent-indigo, 0.2);
    &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba($accent-indigo, 0.3); }
  }
}

/* ===== 通用动画 ===== */
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes cardAppear { from { opacity: 0; transform: translateY(12px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
@keyframes breathPulse {
  0%,100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.8); opacity: 0; }
}
@keyframes pulseBorder {
  0%,100% { box-shadow: 0 0 28px rgba($color-warning, 0.12); }
  50% { box-shadow: 0 0 40px rgba($color-warning, 0.25); }
}
@keyframes pulseDot {
  0%,100% { box-shadow: 0 0 0 0 rgba($accent-amber, 0.6); transform: scale(1); }
  50% { box-shadow: 0 0 0 5px rgba($accent-amber, 0); transform: scale(1.1); }
}
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

.btn-spinner-sm {
  width: 12px; height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3); border-top-color: $text-primary;
  border-radius: 50%; animation: spin 0.8s linear infinite; display: inline-block;
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.25s ease; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(-8px); }

/* ===== 过渡动画 ===== */
.card-trans-enter-active { transition: all 0.3s ease; }
.card-trans-leave-active { transition: all 0.2s ease; position: absolute; }
.card-trans-enter-from { opacity: 0; transform: translateY(16px); }
.card-trans-leave-to { opacity: 0; transform: scale(0.95); }
.card-trans-move { transition: transform 0.3s ease; }

.log-trans-enter-active { transition: all 0.3s ease; }
.log-trans-leave-active { transition: all 0.2s ease; }
.log-trans-enter-from { opacity: 0; transform: translateX(-12px); }
.log-trans-leave-to { opacity: 0; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .sidebar { width: 180px; }
  .history-drawer { width: 360px; }
  .top-bar-center { max-width: 280px; }
  .workspace-content { padding: 12px; }
  .conv-msg { max-width: 92%; }
  .wc-input { padding: 2px 2px 2px 10px; }
  .welcome-cards { grid-template-columns: 1fr; }
  .cmsg { max-width: 92%; }
  .tab-name { max-width: 80px; }
}

@media (max-width: 640px) {
  .top-bar { padding: 8px 12px; gap: 8px; }
  .top-bar-center { display: none; }
  .sidebar { width: 0; position: absolute; z-index: 50; }
  .history-drawer { width: 100%; }
  .workspace-content { padding: 10px; }
  .tab-bar { padding: 2px 4px; }
  .tab-item { padding: 2px 6px 2px 4px; }
}

/* Legacy responsive */
@media (max-width: 768px) {
  .uniform-grid { grid-template-columns: 1fr; }
  .agent-card.span-2 { grid-column: span 1; }
  .command-wrap { flex-direction: column; align-items: stretch; }
  .command-icon { display: none; }
  .command-btn { align-self: flex-end; }
  .terminal-output { max-height: 300px; }
}

/* ===== 右侧: Orchestrator 编排面板 ===== */
.orchestrator-panel {
  width: 0;
  overflow: hidden;
  background: rgba($bg-surface, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba($accent-indigo, 0.12);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.3s ease;
  position: relative;

  &.orchestrator--open {
    width: 380px;
  }
}

/* 面板伸缩标签 - 始终固定在视图右侧边缘 */
.orch-tab {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1001;
  width: 36px;
  height: 100px;
  background: rgba($accent-teal, 0.35);
  border: 1px solid rgba($accent-teal, 0.5);
  border-right: none;
  border-radius: 10px 0 0 10px;
  color: $accent-teal;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s ease;
  box-shadow: -3px 0 16px rgba($accent-teal, 0.2);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  &:hover {
    width: 42px;
    background: rgba($accent-teal, 0.5);
    border-color: rgba($accent-teal, 0.7);
    box-shadow: -5px 0 24px rgba($accent-teal, 0.35);
    transform: translateY(-50%) translateX(-3px);
  }

  .orch-tab-label {
    font-size: 11px;
    font-weight: 600;
    writing-mode: vertical-rl;
    letter-spacing: 2px;
    color: $accent-teal;
  }

  /* 面板打开时，标签移动到面板左侧边缘 */
  &.orch-tab--open {
    right: 380px;
    border-radius: 0 10px 10px 0;
    border: 1px solid rgba($accent-teal, 0.5);
    border-left: none;
    box-shadow: 3px 0 16px rgba($accent-teal, 0.2);
  }
}

.orch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba($accent-indigo, 0.12);
  flex-shrink: 0;
  height: 48px;
}

.orch-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: $text-primary;

  .orch-icon { font-size: 1.2rem; }
}

.orch-toggle {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover { color: $accent-indigo; background: rgba($accent-indigo, 0.08); }
}

.orch-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.orch-empty {
  text-align: center;
  padding: 40px 16px;
  color: $text-muted;
  font-size: 0.75rem;
  line-height: 1.6;

  p { margin: 0 0 16px; }
  strong { color: $accent-indigo; }
}

.orch-hints {
  .hint {
    display: block;
    font-size: 0.72rem;
    color: $text-placeholder;
    background: rgba($accent-indigo, 0.05);
    border: 1px dashed rgba($accent-indigo, 0.15);
    border-radius: 8px;
    padding: 12px;
    margin-top: 12px;
  }
}

.orch-message {
  background: $bg-elevated;
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
  padding: 12px;
  animation: fadeIn 0.3s ease;

  &.user {
    border-color: rgba($accent-indigo, 0.2);
    background: rgba($accent-indigo, 0.04);
  }

  &.orchestration_start {
    border-color: rgba($accent-violet, 0.2);
    background: rgba($accent-violet, 0.04);
  }

  &.decomposition {
    border-color: rgba($color-warning, 0.2);
    background: rgba($color-warning, 0.04);
  }

  &.subtask_start { border-left: 3px solid $accent-indigo; }
  &.subtask_done { border-left: 3px solid $color-success; }
  &.subtask_error { border-left: 3px solid $color-danger; }
  &.aggregating { border-color: rgba($accent-violet, 0.2); }
  &.orchestration_done { border-color: rgba($color-success, 0.3); background: rgba($color-success, 0.04); }
  &.error { border-color: rgba($color-danger, 0.3); background: rgba($color-danger, 0.04); }
}

.orch-msg-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.orch-msg-icon { font-size: 1rem; }

.orch-msg-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: $text-primary;
  flex: 1;
}

.orch-msg-agent {
  font-size: 10px;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
}

.orch-msg-content {
  font-size: 0.75rem;
  color: $text-secondary;
  line-height: 1.6;

  code {
    background: rgba($accent-indigo, 0.08);
    border-radius: 3px;
    padding: 1px 5px;
    font-size: 11px;
    font-family: 'Fira Code', monospace;
  }
}

.orch-subtasks {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.orch-subtask {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba($bg-surface, 0.5);
  border-radius: 8px;
  border: 1px solid rgba($accent-indigo, 0.08);
  font-size: 0.75rem;

  .st-agent {
    font-weight: 600;
    color: $accent-indigo;
    min-width: 70px;
  }

  .st-desc {
    flex: 1;
    color: $text-secondary;
  }

  .st-status {
    font-size: 10px;
    white-space: nowrap;
    color: $text-muted;

    &.pending { color: $text-muted; }
    &.running { color: $accent-indigo; }
    &.done { color: $color-success; }
    &.error { color: $color-danger; }
  }

  &.st-running { border-color: rgba($accent-indigo, 0.3); }
  &.st-done { border-color: rgba($color-success, 0.3); }
  &.st-error { border-color: rgba($color-danger, 0.3); }
}

.orch-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: $text-muted;
  font-size: 0.75rem;

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba($accent-indigo, 0.2);
    border-top-color: $accent-indigo;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

.orch-input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid rgba($accent-indigo, 0.12);
  flex-shrink: 0;
}

.orch-input {
  flex: 1;
  background: $bg-elevated;
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.75rem;
  color: $text-primary;
  outline: none;
  font-family: $font-sans;
  transition: border-color 0.2s;

  &:focus { border-color: $accent-indigo; }
  &:disabled { opacity: 0.5; }
  &::placeholder { color: $text-placeholder; }
}

.orch-send {
  padding: 8px 16px;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: 8px;
  color: $accent-indigo;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  font-family: $font-sans;

  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.25);
    border-color: rgba($accent-indigo, 0.5);
  }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
</style>