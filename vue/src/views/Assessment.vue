<template>
  <div class="assessment-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
      <!-- 雷达扫描背景 -->
      <div class="radar-scanner">
        <div class="radar-ring ring-1"></div>
        <div class="radar-ring ring-2"></div>
        <div class="radar-ring ring-3"></div>
        <div class="radar-sweep"></div>
        <div class="radar-dots">
          <span v-for="i in 8" :key="i" class="radar-dot" :style="getRadarDotStyle(i)"></span>
        </div>
      </div>
      <!-- 神经网络连线 -->
      <div class="neural-network">
        <svg class="neural-svg" viewBox="0 0 400 400" preserveAspectRatio="xMidYMid slice">
          <defs>
            <linearGradient id="lineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="rgba(139,92,246,0)" />
              <stop offset="50%" stop-color="rgba(139,92,246,0.3)" />
              <stop offset="100%" stop-color="rgba(139,92,246,0)" />
            </linearGradient>
          </defs>
          <line v-for="(line, i) in neuralLines" :key="i" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2" stroke="url(#lineGrad)" stroke-width="0.5" class="neural-line" :style="{ animationDelay: i * 0.3 + 's' }"/>
          <circle v-for="(node, i) in neuralNodes" :key="'n'+i" :cx="node.x" :cy="node.y" r="2" fill="rgba(139,92,246,0.5)" class="neural-node" :style="{ animationDelay: i * 0.2 + 's' }"/>
        </svg>
      </div>
    </div>

    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">🎯</span>
            <span>能力测评</span>
            <span class="title-sub">多维度能力评估与画像</span>
          </h1>
        </div>
        <div class="header-right">
          <button v-if="phase === 'result'" class="retry-btn" @click="startAssessment">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" /></svg>
            <span>重新测评</span>
          </button>
        </div>
      </div>
    </header>

    <!-- ===== 状态一：开始页（含科目选择） ===== -->
    <div v-if="phase === 'welcome'" class="content">
      <!-- AI 智能推荐区 -->
      <div v-if="aiRecommendation" class="ai-recommendation-card glass-card">
        <div class="ai-rec-header">
          <span class="ai-rec-icon">🎯</span>
          <span class="ai-rec-title">AI 智能推荐</span>
          <span class="ai-rec-badge">个性化</span>
        </div>
        <div class="ai-rec-content">
          <p class="ai-rec-message">{{ aiRecommendation.message }}</p>
          <div v-if="aiRecommendation.reasons?.length > 0" class="ai-rec-reasons">
            <span v-for="(reason, i) in aiRecommendation.reasons" :key="i" class="reason-tag">{{ reason }}</span>
          </div>
        </div>
        <button class="ai-rec-action-btn" @click="applyAIRecommendation">
          开始推荐测评 →
        </button>
      </div>

      <div class="welcome-card glass-card">
        <div class="welcome-icon">📋</div>
        <h2 class="welcome-title">多维度能力测评</h2>
        <p class="welcome-desc">选择科目后开始测评，系统将自动生成题目评估你的知识水平。</p>
        <div class="subject-input-area">
          <label class="selector-label">
            <span class="label-icon">🎯</span>
            输入测评科目
          </label>
          <div class="input-wrapper-ai">
            <span class="input-prefix-icon">❯</span>
            <input
              v-model="subjectInput"
              type="text"
              class="subject-input-ai"
              :placeholder="currentPlaceholder"
              @focus="isInputFocused = true"
              @blur="handleInputBlur"
            />
            <span v-if="subjectInput" class="input-clear" @click="subjectInput = ''">✕</span>
            <div class="input-ai-badge">
              <span class="ai-dot"></span>
              AI
            </div>
          </div>
          <!-- AI 联想下拉 -->
          <transition name="dropdown-fade">
            <div v-if="isInputFocused && subjectInput && aiSuggestions.length > 0" class="ai-suggestions-dropdown">
              <div v-for="(sug, i) in aiSuggestions" :key="i" class="suggestion-item" @mousedown.prevent="selectSuggestion(sug)">
                <span class="sug-icon">{{ sug.icon }}</span>
                <div class="sug-content">
                  <span class="sug-name">{{ sug.name }}</span>
                  <span class="sug-tag" v-if="sug.isAI">AI 推荐</span>
                </div>
                <span class="sug-arrow">→</span>
              </div>
            </div>
          </transition>
          
          <!-- 测评目标预览 -->
          <transition name="slide-up">
            <div v-if="subjectInput.trim()" class="assessment-targets">
              <span class="target-label">本次测评将重点考察：</span>
              <div class="target-tags">
                <span class="target-tag" v-for="(t, i) in currentTargets" :key="i" :style="{ animationDelay: i * 0.1 + 's' }">{{ t }}</span>
              </div>
            </div>
          </transition>
          
          <div class="quick-subjects">
            <span
              v-for="(tag, i) in quickSubjects"
              :key="tag"
              class="quick-tag-magnetic"
              :class="{ active: subjectInput === tag }"
              :style="{ animationDelay: i * 0.05 + 's' }"
              @click="subjectInput = tag"
              @mouseenter="magneticEnter($event)"
              @mouseleave="magneticLeave($event)"
            >
              <span class="tag-text">{{ tag }}</span>
              <span v-if="subjectInput === tag" class="tag-check">✓</span>
            </span>
          </div>
        </div>

        <!-- 自适应难度推荐（基于历史测评真实数据） -->
        <div v-if="adaptiveConfig" class="adaptive-recommend">
          <div class="adaptive-recommend-header">
            <span class="adaptive-icon">🧠</span>
            <div class="adaptive-recommend-text">
              <span class="adaptive-title">自适应难度推荐</span>
              <span class="adaptive-desc">基于你 {{ adaptiveConfig.totalAttempts || 0 }} 次《{{ subjectInput }}》测评历史，推荐合适难度</span>
            </div>
            <span v-if="adaptiveLoading" class="adaptive-loading">分析中...</span>
          </div>
          <div class="adaptive-recommend-body">
            <div class="adaptive-item">
              <span class="adaptive-item-label">推荐难度</span>
              <span class="adaptive-item-value">{{ adaptiveDifficultyLabel }}</span>
            </div>
            <div class="adaptive-item">
              <span class="adaptive-item-label">历史正确率</span>
              <span class="adaptive-item-value">{{ adaptiveConfig.historicalAccuracy ?? '—' }}%</span>
            </div>
            <div class="adaptive-item">
              <span class="adaptive-item-label">推荐题数</span>
              <span class="adaptive-item-value">{{ adaptiveConfig.recommendedCount ?? 10 }} 题</span>
            </div>
            <label class="adaptive-switch">
              <input v-model="adaptiveEnabled" type="checkbox" />
              <span class="adaptive-switch-text">{{ adaptiveEnabled ? '已开启自适应' : '已关闭' }}</span>
            </label>
          </div>
        </div>

        <div class="difficulty-selector">
          <label class="selector-label">
            <span class="label-icon">⚡</span>
            选择难度
          </label>
          <div class="difficulty-options-game">
            <div 
              v-for="d in difficulties" 
              :key="d.value" 
              class="difficulty-card-game"
              :class="{ active: selectedDifficulty === d.value, [d.value]: true }"
              @click="selectedDifficulty = d.value"
            >
              <div class="diff-bg-pattern" :class="'pattern-' + d.value"></div>
              <div class="diff-content">
                <span class="diff-emoji">{{ d.icon }}</span>
                <span class="diff-name">{{ d.label }}</span>
                <span class="diff-desc-game">{{ d.desc }}</span>
              </div>
              <div class="diff-wave">
                <svg viewBox="0 0 120 30" preserveAspectRatio="none">
                  <path v-if="d.value === 'easy'" d="M0,15 Q30,5 60,15 T120,15" fill="none" stroke="currentColor" stroke-width="2" class="wave-path"/>
                  <path v-else-if="d.value === 'medium'" d="M0,15 Q20,5 40,20 T80,10 T120,15" fill="none" stroke="currentColor" stroke-width="2" class="wave-path"/>
                  <path v-else d="M0,15 Q15,2 30,25 T60,5 T90,28 T120,15" fill="none" stroke="currentColor" stroke-width="2" class="wave-path"/>
                </svg>
              </div>
              <div v-if="selectedDifficulty === d.value" class="diff-active-glow"></div>
            </div>
          </div>
        </div>
        <!-- 动态参数面板 -->
        <div class="dashboard-panel">
          <div class="dashboard-item">
            <div class="dash-gauge">
              <svg viewBox="0 0 60 36">
                <path d="M5,30 A25,25 0 0,1 55,30" fill="none" stroke="rgba(139,92,246,0.15)" stroke-width="4" stroke-linecap="round"/>
                <path d="M5,30 A25,25 0 0,1 55,30" fill="none" stroke="url(#timeGrad)" stroke-width="4" stroke-linecap="round" :stroke-dasharray="78.54" :stroke-dashoffset="78.54 * (1 - animatedTime / 30)"/>
                <defs>
                  <linearGradient id="timeGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#8b5cf6"/>
                    <stop offset="100%" stop-color="#06b6d4"/>
                  </linearGradient>
                </defs>
              </svg>
              <span class="dash-value">{{ animatedTime }}</span>
            </div>
            <span class="dash-label">预计时间(分钟)</span>
          </div>
          <div class="dashboard-divider"></div>
          <div class="dashboard-item">
            <div class="dash-gauge">
              <svg viewBox="0 0 60 36">
                <path d="M5,30 A25,25 0 0,1 55,30" fill="none" stroke="rgba(16,185,129,0.15)" stroke-width="4" stroke-linecap="round"/>
                <path d="M5,30 A25,25 0 0,1 55,30" fill="none" stroke="url(#countGrad)" stroke-width="4" stroke-linecap="round" :stroke-dasharray="78.54" :stroke-dashoffset="78.54 * (1 - animatedCount / 20)"/>
                <defs>
                  <linearGradient id="countGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#10b981"/>
                    <stop offset="100%" stop-color="#06b6d4"/>
                  </linearGradient>
                </defs>
              </svg>
              <span class="dash-value">{{ animatedCount }}</span>
            </div>
            <span class="dash-label">题目数量</span>
          </div>
          <div class="dashboard-divider"></div>
          <div class="dashboard-item">
            <div class="dash-radar-mini" @mouseenter="showRadarTooltip = true" @mouseleave="showRadarTooltip = false">
              <svg viewBox="0 0 40 40">
                <polygon points="20,5 35,15 35,25 20,35 5,25 5,15" fill="none" stroke="rgba(245,158,11,0.3)" stroke-width="1"/>
                <polygon points="20,10 30,17 30,23 20,30 10,23 10,17" fill="rgba(245,158,11,0.1)" stroke="rgba(245,158,11,0.5)" stroke-width="1"/>
                <circle cx="20" cy="17" r="2" fill="#f59e0b"/>
              </svg>
              <transition name="tooltip-pop">
                <div v-if="showRadarTooltip" class="radar-tooltip">
                  <span class="radar-dim">语法</span>
                  <span class="radar-dim">逻辑</span>
                  <span class="radar-dim">架构</span>
                  <span class="radar-dim">算法</span>
                </div>
              </transition>
            </div>
            <span class="dash-label">多维度分析</span>
          </div>
        </div>
        <!-- AI 自适应模式开关 -->
        <div class="adaptive-mode-section">
          <div class="adaptive-mode-toggle" @click="adaptiveEnabled = !adaptiveEnabled"
            @mouseenter="showAdaptiveTooltip = true"
            @mouseleave="showAdaptiveTooltip = false">
            <div class="toggle-track" :class="{ active: adaptiveEnabled }">
              <div class="toggle-thumb"></div>
            </div>
            <div class="toggle-info">
              <span class="toggle-title">AI 自适应模式</span>
              <span class="toggle-desc" :class="{ active: adaptiveEnabled }">
                {{ adaptiveEnabled ? '智能' : '普通' }}
              </span>
            </div>
            <div class="toggle-badge" v-if="adaptiveEnabled">
              <span class="badge-pulse"></span>
              智能
            </div>
            <!-- Tooltip -->
            <transition name="tooltip-pop">
              <div v-if="showAdaptiveTooltip" class="adaptive-tooltip">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                <span>系统将根据您的答题正确率，实时动态调整下一题的难度系数。</span>
              </div>
            </transition>
          </div>
        </div>

        <div class="action-buttons">
          <button class="btn-start-cyber" :disabled="loading || !subjectInput.trim()" @click="startAssessment">
            <div class="btn-bg"></div>
            <div class="btn-content">
              <span v-if="loading" class="loading-state">
                <span class="loading-dots">
                  <span></span><span></span><span></span>
                </span>
                <span class="loading-text">{{ loadingText }}</span>
              </span>
              <template v-else>
                <span class="btn-icon">🚀</span>
                <span class="btn-text">启动 AI 评估</span>
                <span class="btn-arrow">→</span>
              </template>
            </div>
          </button>
          <button class="btn-history-link" @click="openHistory">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            查看我的能力雷达图
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 状态二：答题中 ===== -->
    <div v-if="phase === 'quiz'" class="content">
      <div class="quiz-progress-bar">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }"></div>
          <div v-for="(_, i) in questions" :key="i" class="progress-dot" :class="{ active: i <= currentIndex, current: i === currentIndex }" :style="{ left: ((i + 0.5) / questions.length * 100) + '%' }"></div>
        </div>
        <div class="progress-meta">
          <span class="progress-text">第 <strong>{{ currentIndex + 1 }}</strong> / {{ questions.length }} 题</span>
          <div class="quiz-tags">
            <span class="quiz-category">{{ subjectLabel }}</span>
            <!-- 非自适应模式显示固定难度 -->
            <span v-if="!adaptiveEnabled" class="quiz-difficulty" :class="'diff-' + selectedDifficulty">{{ difficultyLabel }}</span>
            <!-- 自适应模式显示动态难度指示器 -->
            <div v-else class="adaptive-difficulty-indicator">
              <span class="difficulty-label">当前难度</span>
              <span class="difficulty-stars">{{ getDifficultyStars(currentDifficultyLevel) }}</span>
              <span class="difficulty-level-text">{{ getDifficultyLevelText(currentDifficultyLevel) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 自适应模式：AI 分析提示 -->
      <div v-if="adaptiveEnabled" class="ai-analysis-hint">
        <div class="hint-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 14a1 1 0 1 1 1-1 1 1 0 0 1-1 1zm1-4h-2V7h2z"/></svg>
        </div>
        <span class="hint-text">AI 正在根据您的答题表现实时调整题目难度</span>
        <span class="hint-ability">能力估值: {{ abilityEstimate.toFixed(1) }}</span>
      </div>

      <div class="quiz-card glass-card">
        <div class="question-number">Q{{ currentIndex + 1 }}</div>
        <h3 class="question-text">{{ currentQuestion?.questionText }}</h3>
        <div class="question-options">
          <label
v-for="(opt, i) in (currentQuestion?.options || [])" :key="i" class="option-item" :class="{
            selected: selectedOption === i
          }" @click="selectOption(i)"
>
            <span class="option-letter">{{ ['A','B','C','D'][i] }}</span>
            <span class="option-text">{{ opt }}</span>
            <span v-if="selectedOption === i" class="option-check">✓</span>
          </label>
        </div>
      </div>

      <div class="quiz-actions">
        <button class="btn-prev" :disabled="currentIndex === 0" @click="prevQuestion">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6" /></svg>
          上一题
        </button>
        <div class="action-right">
          <button v-if="currentIndex < questions.length - 1" class="btn-next" @click="nextQuestion">
            下一题
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
          <button v-else class="btn-submit-all" :disabled="!allAnswered" @click="submitAllAnswers">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12" /></svg>
            提交全部答案
          </button>
        </div>
      </div>

      <div v-if="!allAnswered && currentIndex === questions.length - 1" class="unanswered-tip">
        请完成所有题目后再提交
      </div>
    </div>

    <!-- ===== 状态三：结果页 ===== -->
    <div v-if="phase === 'result'" class="content">
      <!-- 普通模式结果 -->
      <div v-if="!adaptiveEnabled" class="result-hero glass-card">
        <div class="result-score-section">
          <span class="result-label">综合得分</span>
          <div class="result-score-wrap">
            <span class="result-score" :style="{ color: scoreColor }">{{ resultData.totalScore }}</span>
            <span class="result-total">/ 100</span>
          </div>
          <span class="result-level" :style="{ background: levelBg, color: scoreColor, borderColor: scoreColor + '30' }">{{ resultData.level }}</span>
        </div>
        <div class="result-quick-stats">
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.correctCount }}</span>
            <span class="qs-label">答对</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.wrongCount }}</span>
            <span class="qs-label">答错</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.accuracy }}%</span>
            <span class="qs-label">正确率</span>
          </div>
        </div>
      </div>

      <!-- 自适应模式结果 -->
      <div v-else class="result-hero glass-card adaptive-result">
        <div class="result-score-section">
          <span class="result-label">能力值 (IRT)</span>
          <div class="result-score-wrap">
            <span class="result-score adaptive-score" :style="{ color: adaptiveScoreColor }">{{ abilityEstimate.toFixed(0) }}</span>
          </div>
          <span class="result-level" :style="{ background: adaptiveLevelBg, color: adaptiveScoreColor, borderColor: adaptiveScoreColor + '30' }">{{ adaptiveLevel }}</span>
        </div>
        <div class="result-quick-stats">
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.correctCount }}</span>
            <span class="qs-label">答对</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.wrongCount }}</span>
            <span class="qs-label">答错</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ abilitySE.toFixed(2) }}</span>
            <span class="qs-label">标准误</span>
          </div>
        </div>
      </div>

      <!-- 自适应模式：难度曲线图 -->
      <div v-if="adaptiveEnabled" class="difficulty-curve-card glass-card">
        <div class="curve-header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          <span class="curve-title">难度爬坡曲线</span>
        </div>
        <div class="curve-chart">
          <div class="curve-y-axis">
            <span>困难</span>
            <span>中等</span>
            <span>简单</span>
          </div>
          <div class="curve-svg-wrap">
            <svg viewBox="0 0 400 150" class="curve-svg">
              <!-- 背景网格 -->
              <line v-for="i in 3" :key="'grid-'+i" :x1="0" :y1="i * 50" :x2="400" :y2="i * 50" stroke="rgba(139,92,246,0.08)" stroke-width="1"/>
              <!-- 难度曲线 -->
              <polyline :points="difficultyCurvePoints" fill="none" stroke="url(#curveGradient)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 数据点 -->
              <circle v-for="(point, i) in difficultyCurveData" :key="'pt-'+i" :cx="point.x" :cy="point.y" r="4" :fill="point.correct ? '#00E676' : '#FF4D4F'" stroke="rgba(15,20,35,0.8)" stroke-width="2"/>
              <defs>
                <linearGradient id="curveGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="#8b5cf6"/>
                  <stop offset="100%" stop-color="#06b6d4"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
        </div>
        <div class="curve-legend">
          <span class="legend-item"><span class="legend-dot correct"></span>答对</span>
          <span class="legend-item"><span class="legend-dot wrong"></span>答错</span>
        </div>
      </div>

      <div class="result-card-list glass-card">
        <h3 class="section-title">📝 答题详情</h3>
        <div v-for="(item, i) in resultData.details" :key="i" class="result-question-item" :class="{ correct: item.correct, wrong: !item.correct }">
          <div class="rq-header">
            <span class="rq-number">Q{{ i + 1 }}</span>
            <span class="rq-badge" :class="item.correct ? 'badge-correct' : 'badge-wrong'">{{ item.correct ? '正确' : '错误' }}</span>
          </div>
          <p class="rq-text">{{ item.questionText }}</p>
          <div class="rq-options">
            <div
v-for="(opt, j) in item.options" :key="j" class="rq-option" :class="{
              'rq-correct': j === item.correctAnswer,
              'rq-wrong': j === item.userAnswer && j !== item.correctAnswer
            }"
>
              <span class="rq-opt-letter">{{ ['A','B','C','D'][j] }}</span>
              <span class="rq-opt-text">{{ opt }}</span>
              <span v-if="j === item.correctAnswer" class="rq-mark">✅</span>
              <span v-if="j === item.userAnswer && j !== item.correctAnswer" class="rq-mark">❌</span>
            </div>
          </div>
          <div class="rq-explanation">
            <span class="rq-explain-icon">💡</span>
            <span>{{ item.explanation }}</span>
          </div>
        </div>
      </div>

      <!-- AI 诊断分析入口 -->
      <div class="ai-diagnosis-card glass-card">
        <div class="diagnosis-header">
          <span class="diagnosis-icon">🧠</span>
          <span class="diagnosis-title">AI 诊断分析</span>
        </div>
        <div class="diagnosis-content">
          <p class="diagnosis-message">
            基于你的测评结果，诊断 Agent 将分析你的薄弱点并生成个性化的学习建议。
          </p>
          <div class="diagnosis-stats">
            <span class="diag-stat">📊 测评科目：{{ subjectLabel }}</span>
            <span class="diag-stat">📈 正确率：{{ resultData.accuracy }}%</span>
            <span class="diag-stat">📝 答对：{{ resultData.correctCount }} 题</span>
          </div>
        </div>
        <div class="diagnosis-actions">
          <button class="btn-diagnosis" @click="openDiagnosis">
            <span>🔍</span>
            <span>获取详细诊断报告</span>
          </button>
          <button class="btn-adjust-path" @click="adjustLearningPath">
            <span>📋</span>
            <span>根据结果调整学习路径</span>
          </button>
          <button v-if="weaknessTopics.length > 0" class="btn-weak-path" @click="generateWeakPath">
            <span>🎯</span>
            <span>生成专项训练路径（7天）</span>
          </button>
        </div>
      </div>

      <div class="result-actions">
        <button class="btn-secondary" @click="startAssessment">🔄 重新测评</button>
      </div>
    </div>
  </div>

  <div class="history-dialog-container">
    <HistoryDialog v-model="historyVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getQuestions, submitAnswers, getSubjects, getAdaptiveConfig, generateAdaptiveQuiz } from '@/api/assessmentApi'
import { ElMessage } from 'element-plus'
import HistoryDialog from '@/components/tools/HistoryDialog.vue'

const router = useRouter()

// ===== 背景动画数据 =====
const neuralNodes = ref([
  { x: 50, y: 80 }, { x: 120, y: 150 }, { x: 80, y: 250 }, { x: 180, y: 120 },
  { x: 250, y: 200 }, { x: 320, y: 100 }, { x: 350, y: 280 }, { x: 200, y: 350 },
  { x: 100, y: 320 }, { x: 300, y: 350 }, { x: 380, y: 180 }, { x: 160, y: 50 }
])
const neuralLines = computed(() => {
  const nodes = neuralNodes.value
  const lines = []
  for (let i = 0; i < nodes.length; i++) {
    for (let j = i + 1; j < nodes.length; j++) {
      const dist = Math.hypot(nodes[i].x - nodes[j].x, nodes[i].y - nodes[j].y)
      if (dist < 150) {
        lines.push({ x1: nodes[i].x, y1: nodes[i].y, x2: nodes[j].x, y2: nodes[j].y })
      }
    }
  }
  return lines
})

const getRadarDotStyle = (i) => {
  const angle = (i / 8) * 360
  const radius = 80 + Math.random() * 60
  const x = Math.cos((angle * Math.PI) / 180) * radius
  const y = Math.sin((angle * Math.PI) / 180) * radius
  return {
    left: `calc(50% + ${x}px)`,
    top: `calc(50% + ${y}px)`,
    animationDelay: `${i * 0.4}s`
  }
}

// ===== 科目数据（自定义输入） =====
const subjectInput = ref('')
const quickSubjects = ref([
  'Python', 'Java', 'C++', 'JavaScript', '数据结构与算法',
  '数据库', '网络基础', '机器学习', '前端开发', '系统设计'
])

// ===== 占位符轮播 =====
const placeholderTexts = [
  '试试输入 "Python 爬虫"...',
  '或者 "React 性能优化"...',
  '输入 "Java 并发编程"...',
  '探索 "机器学习入门"...',
  '挑战 "算法与数据结构"...'
]
const currentPlaceholderIndex = ref(0)
const currentPlaceholder = ref(placeholderTexts[0])
const isInputFocused = ref(false)
let placeholderTimer = null

const startPlaceholderRotation = () => {
  placeholderTimer = setInterval(() => {
    currentPlaceholderIndex.value = (currentPlaceholderIndex.value + 1) % placeholderTexts.length
    currentPlaceholder.value = placeholderTexts[currentPlaceholderIndex.value]
  }, 3000)
}

const handleInputBlur = () => {
  setTimeout(() => { isInputFocused.value = false }, 200)
}

// ===== AI 联想 =====
const aiSuggestions = computed(() => {
  const input = subjectInput.value.toLowerCase()
  if (!input) return []
  
  const allSuggestions = [
    { name: 'Python 基础语法', icon: '🐍', isAI: false },
    { name: 'Python 爬虫开发', icon: '🕷️', isAI: true },
    { name: 'Python 数据分析', icon: '📊', isAI: true },
    { name: 'Python 机器学习', icon: '🤖', isAI: true },
    { name: 'Java 核心基础', icon: '☕', isAI: false },
    { name: 'Java 并发编程', icon: '⚡', isAI: true },
    { name: 'Java SpringBoot', icon: '🌱', isAI: true },
    { name: 'JavaScript ES6+', icon: '📜', isAI: false },
    { name: 'React 框架', icon: '⚛️', isAI: true },
    { name: 'Vue 框架', icon: '💚', isAI: true },
    { name: '数据结构与算法', icon: '🧮', isAI: false },
    { name: '机器学习算法', icon: '🧠', isAI: true },
    { name: '深度学习', icon: '🔮', isAI: true },
    { name: '计算机网络', icon: '🌐', isAI: false },
    { name: '数据库原理', icon: '🗄️', isAI: false },
    { name: '系统设计', icon: '🏗️', isAI: false }
  ]
  
  return allSuggestions.filter(s => s.name.toLowerCase().includes(input)).slice(0, 5)
})

const selectSuggestion = (sug) => {
  subjectInput.value = sug.name.replace(/ (基础语法|爬虫开发|数据分析|机器学习|核心基础|并发编程|框架|算法|原理)$/, '')
  isInputFocused.value = false
}

// ===== 测评目标预览 =====
const currentTargets = computed(() => {
  const subject = subjectInput.value.toLowerCase()
  if (subject.includes('python')) return ['核心语法', '常用库', '实战场景']
  if (subject.includes('java')) return ['面向对象', '集合框架', '并发编程']
  if (subject.includes('react') || subject.includes('vue')) return ['组件化', '状态管理', '性能优化']
  if (subject.includes('算法') || subject.includes('数据结构')) return ['时间复杂度', '排序算法', '图论']
  if (subject.includes('机器学习') || subject.includes('深度学习')) return ['模型原理', '特征工程', '模型评估']
  if (subject.includes('网络')) return ['TCP/IP', 'HTTP协议', '网络安全']
  if (subject.includes('数据库')) return ['SQL语法', '索引优化', '事务管理']
  return ['基础知识', '核心概念', '应用场景']
})

// ===== 磁吸标签效果 =====
const magneticEnter = (e) => {
  const el = e.currentTarget
  el.style.transform = 'scale(1.08) translateY(-2px)'
  el.style.boxShadow = '0 4px 12px rgba(139,92,246,0.2)'
}

const magneticLeave = (e) => {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.boxShadow = ''
}

// ===== 难度数据 =====
const difficulties = [
  { value: 'easy', label: '简单', icon: '🌱', desc: '适合初学者' },
  { value: 'medium', label: '中等', icon: '⚡', desc: '适合有一定基础' },
  { value: 'hard', label: '困难', icon: '🔥', desc: '挑战高阶能力' },
]
const selectedDifficulty = ref('medium')

const difficultyLabel = computed(() => {
  const d = difficulties.find(d => d.value === selectedDifficulty.value)
  return d ? d.label : '中等'
})

// ===== 动态参数面板 =====
const animatedTime = ref(15)
const animatedCount = ref(10)
const showRadarTooltip = ref(false)

const difficultyParams = {
  easy: { time: 10, count: 8 },
  medium: { time: 15, count: 10 },
  hard: { time: 20, count: 15 }
}

const animateNumber = (from, to, setter, duration = 400) => {
  const start = performance.now()
  const step = (t) => {
    const p = Math.min((t - start) / duration, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    setter(Math.round(from + (to - from) * ease))
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(selectedDifficulty, (val) => {
  const params = difficultyParams[val]
  animateNumber(animatedTime.value, params.time, (v) => animatedTime.value = v)
  animateNumber(animatedCount.value, params.count, (v) => animatedCount.value = v)
})

// ===== 自适应难度（基于历史测评真实数据） =====
const adaptiveEnabled = ref(true)
const adaptiveConfig = ref(null)
const adaptiveLoading = ref(false)
let adaptiveTimer = null

const adaptiveDifficultyLabel = computed(() => {
  const d = difficulties.find(d => d.value === adaptiveConfig.value?.difficulty)
  return d ? d.label : '中等'
})

// ===== AI 智能推荐（基于用户学习记录生成） =====
const aiRecommendation = computed(() => {
  if (!adaptiveConfig.value || !adaptiveEnabled.value) return null

  const config = adaptiveConfig.value
  const subject = subjectInput.value
  const historyCount = config.totalAttempts || 0
  const accuracy = config.historicalAccuracy

  if (historyCount === 0) {
    return {
      message: `检测到你是《${subject}》的新手，基于学习路径推荐从 ${adaptiveDifficultyLabel.value} 难度开始。`,
      reasons: ['新科目', '打好基础']
    }
  }

  if (accuracy < 50) {
    return {
      message: `你的《${subject}》正确率偏低（${accuracy}%），建议从基础题开始巩固，查漏补缺。`,
      reasons: ['正确率偏低', '需要巩固基础', `历史${historyCount}次测评`]
    }
  }

  if (accuracy >= 80 && historyCount >= 3) {
    return {
      message: `你在《${subject}》表现优秀（正确率${accuracy}%），建议挑战更高难度，突破自己的能力边界。`,
      reasons: ['正确率高', '可挑战高难度', `历史${historyCount}次测评`]
    }
  }

  return {
    message: `基于你的 ${historyCount} 次《${subject}》测评历史，推荐 ${adaptiveDifficultyLabel.value} 难度，继续保持学习节奏。`,
    reasons: [`历史${historyCount}次测评`, `正确率${accuracy}%`]
  }
})

const applyAIRecommendation = () => {
  if (adaptiveConfig.value?.difficulty) {
    selectedDifficulty.value = adaptiveConfig.value.difficulty
  }
  // 应用推荐后自动开始测评
  startAssessment()
}

// 科目变化时加载自适应推荐（防抖 400ms）
const loadAdaptiveConfig = async (subject) => {
  if (!subject || !subject.trim()) {
    adaptiveConfig.value = null
    return
  }
  adaptiveLoading.value = true
  try {
    const res = await getAdaptiveConfig(subject.trim())
    adaptiveConfig.value = res?.data || null
  } catch (e) {
    adaptiveConfig.value = null
  } finally {
    adaptiveLoading.value = false
  }
}

watch(subjectInput, (val) => {
  clearTimeout(adaptiveTimer)
  adaptiveTimer = setTimeout(() => loadAdaptiveConfig(val), 400)
})

// ===== 历史记录弹窗 =====
const historyVisible = ref(false)
function openHistory() {
  historyVisible.value = true
}

// ===== 状态管理 =====
const phase = ref('welcome') // 'welcome' | 'quiz' | 'result'
const currentIndex = ref(0)
const selectedOption = ref(null)
const userAnswers = ref({}) // questionId -> answer index
const loading = ref(false)
const loadingText = ref('AI 正在构建知识图谱...')
const loadingTexts = ['AI 正在构建知识图谱...', '正在分析知识体系...', '智能生成测评题目...']
const adaptiveLoadingTexts = ['AI 正在分析您的答题特征...', '正在计算能力估值...', '正在生成个性化题目...', '正在优化题目难度分布...']
const showAdaptiveTooltip = ref(false)
let loadingTextTimer = null
const resultData = ref(null)

// ===== CAT 算法状态（自适应模式） =====
const abilityEstimate = ref(0) // 能力估值 (IRT)
const abilitySE = ref(1.0) // 标准误
const difficultyHistory = ref([]) // 难度历史记录
const currentDifficultyLevel = ref(0.5) // 当前难度系数 (0-1)
const consecutiveCorrect = ref(0) // 连续答对数
const consecutiveWrong = ref(0) // 连续答错数
const maxQuestions = 20 // 最大题数
const seThreshold = 0.3 // 标准误阈值
const abilityHistory = ref([]) // 能力值变化历史
const difficultyCurve = ref([]) // 难度曲线数据

// ===== 题目数据（从 API 获取） =====
const questions = ref([])

const currentQuestion = computed(() => questions.value[currentIndex.value])

const subjectLabel = computed(() => subjectInput.value || '未选择')

const allAnswered = computed(() => {
  return questions.value.length > 0 && questions.value.every(q => userAnswers.value[q.id] !== undefined)
})

// ===== 颜色计算 =====
const scoreColor = computed(() => {
  const s = resultData.value?.totalScore || 0
  return s >= 80 ? '#00f5d4' : s >= 60 ? '#f59e0b' : s >= 40 ? '#fb923c' : '#ef4444'
})
const levelBg = computed(() => {
  const s = resultData.value?.totalScore || 0
  return s >= 80 ? 'rgba(0,245,212,0.1)' : s >= 60 ? 'rgba(245,158,11,0.1)' : s >= 40 ? 'rgba(251,146,60,0.1)' : 'rgba(239,68,68,0.1)'
})

// ===== 自适应模式结果计算 =====
const adaptiveScoreColor = computed(() => {
  const a = abilityEstimate.value
  return a >= 1200 ? '#00E676' : a >= 1000 ? '#06b6d4' : a >= 800 ? '#F59E0B' : a >= 600 ? '#fb923c' : '#FF4D4F'
})

const adaptiveLevelBg = computed(() => {
  const a = abilityEstimate.value
  return a >= 1200 ? 'rgba(0,230,118,0.1)' : a >= 1000 ? 'rgba(6,182,212,0.1)' : a >= 800 ? 'rgba(245,158,11,0.1)' : a >= 600 ? 'rgba(251,146,60,0.1)' : 'rgba(255,77,79,0.1)'
})

const adaptiveLevel = computed(() => {
  const a = abilityEstimate.value
  if (a >= 1200) return '精通'
  if (a >= 1000) return '优秀'
  if (a >= 800) return '良好'
  if (a >= 600) return '一般'
  return '需提升'
})

const difficultyCurveData = computed(() => {
  const data = difficultyHistory.value
  if (!data.length) return []
  const width = 400
  const height = 150
  const step = data.length > 1 ? width / (data.length - 1) : width / 2
  return data.map((d, i) => ({
    x: i * step,
    y: height - (d.difficulty * height),
    correct: d.correct
  }))
})

const difficultyCurvePoints = computed(() => {
  return difficultyCurveData.value.map(p => `${p.x},${p.y}`).join(' ')
})

// ===== 测评流程 =====
async function startAssessment() {
  if (!subjectInput.value.trim()) {
    ElMessage.warning('请输入要测评的科目')
    return
  }
  localStorage.setItem('quizSubject', subjectInput.value)
  localStorage.setItem('quizDifficulty', selectedDifficulty.value)
  loading.value = true
  let textIdx = 0
  loadingText.value = adaptiveEnabled.value ? adaptiveLoadingTexts[0] : loadingTexts[0]
  loadingTextTimer = setInterval(() => {
    const texts = adaptiveEnabled.value ? adaptiveLoadingTexts : loadingTexts
    textIdx = (textIdx + 1) % texts.length
    loadingText.value = texts[textIdx]
  }, 1500)
  try {
    let list = null
    if (adaptiveEnabled.value) {
      // 自适应模式：后端基于历史测评计算推荐难度
      const res = await generateAdaptiveQuiz(subjectInput.value, true, null, 10)
      list = res?.data || res || []
      if (list && list.length > 0 && adaptiveConfig.value?.difficulty) {
        selectedDifficulty.value = adaptiveConfig.value.difficulty
      }
    } else {
      const res = await getQuestions(subjectInput.value, 10, selectedDifficulty.value)
      list = res?.data || res || []
    }
    if (!list || list.length === 0) {
      ElMessage.warning('未能获取到题目，请稍后重试')
      return
    }
    questions.value = list
    phase.value = 'quiz'
    currentIndex.value = 0
    selectedOption.value = null
    userAnswers.value = {}
    
    // 自适应模式：初始化 CAT 算法状态
    if (adaptiveEnabled.value) {
      abilityEstimate.value = 800 // 初始能力估值
      abilitySE.value = 1.0
      currentDifficultyLevel.value = 0.5 // 初始难度系数
      consecutiveCorrect.value = 0
      consecutiveWrong.value = 0
      difficultyHistory.value = []
      abilityHistory.value = [800]
      difficultyCurve.value = []
    }
    
    loadCurrentAnswer()
  } catch (e) {
    console.error('获取题目失败:', e)
    ElMessage.error('获取题目失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
    if (loadingTextTimer) {
      clearInterval(loadingTextTimer)
      loadingTextTimer = null
    }
  }
}

function openDiagnosis() {
  router.push({
    path: '/capability/diagnosis-report',
    query: {
      subject: subjectLabel.value,
      accuracy: resultData.value?.accuracy,
      score: resultData.value?.totalScore,
      correctCount: resultData.value?.correctCount,
      wrongCount: resultData.value?.wrongCount,
      from: 'assessment',
      // 自适应模式数据
      adaptive: adaptiveEnabled.value ? '1' : '0',
      abilityEstimate: adaptiveEnabled.value ? abilityEstimate.value.toString() : '',
      difficultyHistory: adaptiveEnabled.value ? JSON.stringify(difficultyHistory.value) : ''
    }
  })
}

function adjustLearningPath() {
  router.push({
    path: '/learning-path',
    query: {
      from: 'assessment',
      subject: subjectLabel.value,
      accuracy: resultData.value?.accuracy?.toString() || '0',
      generate: '1', // 触发路径生成
      weakness: getWeaknessTopics() // 薄弱知识点
    }
  })
}

// 获取薄弱知识点（基于错误题目）
function getWeaknessTopics() {
  if (!resultData.value?.details) return ''
  const wrongTopics = resultData.value.details
    .filter(d => !d.correct)
    .map(d => d.category || d.topic)
    .filter(Boolean)
  return [...new Set(wrongTopics)].join(',')
}

// 薄弱知识点列表
const weaknessTopics = computed(() => {
  if (!resultData.value?.details) return []
  const wrongTopics = resultData.value.details
    .filter(d => !d.correct)
    .map(d => d.category || d.topic)
    .filter(Boolean)
  return [...new Set(wrongTopics)].slice(0, 5) // 最多显示5个
})

// 生成专项训练路径
async function generateWeakPath() {
  if (weaknessTopics.value.length === 0) return
  
  const topics = weaknessTopics.value.join('、')
  const goal = `专项突破：${topics}`
  
  try {
    const { generatePath } = await import('@/api/learningPath')
    await generatePath({
      goal,
      durationWeeks: 1, // 7天
      source: 'ai_assessment'
    })
    ElMessage.success('专项训练路径已生成 🎉')
    router.push('/learning-path')
  } catch (error) {
    ElMessage.error('生成失败：' + (error?.response?.data?.message || error?.message || '请稍后重试'))
  }
}

function selectOption(i) {
  if (phase.value !== 'quiz') return
  selectedOption.value = i
  if (currentQuestion.value) {
    userAnswers.value[currentQuestion.value.id] = i
  }
}

// ===== CAT 算法函数 =====
function getDifficultyStars(level) {
  if (level >= 0.75) return '⭐⭐⭐'
  if (level >= 0.5) return '⭐⭐'
  return '⭐'
}

function getDifficultyLevelText(level) {
  if (level >= 0.75) return '困难'
  if (level >= 0.5) return '中等'
  return '简单'
}

// CAT 算法：根据答题情况更新能力估值
function updateAbilityEstimate(isCorrect) {
  const difficulty = currentDifficultyLevel.value
  
  // 简化的 IRT 3PL 模型
  // P(correct) = c + (1-c) / (1 + exp(-a*(θ-b)))
  // 其中 θ 是能力, b 是难度, a 是区分度, c 是猜测参数
  const a = 1.0 // 区分度
  const c = 0.25 // 猜测参数 (四选一)
  
  // 计算预期正确概率
  const expTerm = Math.exp(-a * (abilityEstimate.value - difficulty * 1500))
  const pCorrect = c + (1 - c) / (1 + expTerm)
  
  // 更新能力估值 (简化版)
  const learningRate = 0.3
  if (isCorrect) {
    abilityEstimate.value += learningRate * (1 - pCorrect) * 100
  } else {
    abilityEstimate.value -= learningRate * pCorrect * 100
  }
  
  // 限制范围
  abilityEstimate.value = Math.max(200, Math.min(1800, abilityEstimate.value))
  
  // 更新标准误 (简化版)
  const information = pCorrect * (1 - pCorrect)
  if (information > 0) {
    abilitySE.value = Math.sqrt(1 / (1 / (abilitySE.value * abilitySE.value) + information * 10))
  }
  
  // 记录能力值变化
  abilityHistory.value.push(abilityEstimate.value)
}

// CAT 算法：动态调整下一题难度
function adjustDifficulty(isCorrect) {
  // 记录当前难度和答题结果
  difficultyHistory.value.push({
    difficulty: currentDifficultyLevel.value,
    correct: isCorrect,
    timestamp: Date.now()
  })
  
  // 更新连续计数
  if (isCorrect) {
    consecutiveCorrect.value++
    consecutiveWrong.value = 0
  } else {
    consecutiveWrong.value++
    consecutiveCorrect.value = 0
  }
  
  // 动态调整难度
  const difficultyStep = 0.15
  
  if (isCorrect) {
    // 答对：难度提升
    currentDifficultyLevel.value = Math.min(1, currentDifficultyLevel.value + difficultyStep)
  } else {
    // 答错：难度降低
    currentDifficultyLevel.value = Math.max(0, currentDifficultyLevel.value - difficultyStep)
  }
  
  // 连续波动锁定
  if (consecutiveCorrect.value >= 3) {
    // 连续答对3题，锁定当前层级进行细分测试
    currentDifficultyLevel.value = Math.min(1, currentDifficultyLevel.value + 0.1)
  } else if (consecutiveWrong.value >= 3) {
    // 连续答错3题，降低难度进行巩固
    currentDifficultyLevel.value = Math.max(0, currentDifficultyLevel.value - 0.1)
  }
  
  // 更新难度曲线
  difficultyCurve.value.push({
    level: currentDifficultyLevel.value,
    correct: isCorrect
  })
}

// 检查是否应该终止测评
function shouldTerminate() {
  return abilitySE.value < seThreshold || 
         difficultyHistory.value.length >= maxQuestions
}

function loadCurrentAnswer() {
  if (currentQuestion.value) {
    const saved = userAnswers.value[currentQuestion.value.id]
    selectedOption.value = saved !== undefined ? saved : null
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    // 自适应模式：记录答题结果并调整难度
    if (adaptiveEnabled.value && currentQuestion.value) {
      const userAnswer = userAnswers.value[currentQuestion.value.id]
      const isCorrect = userAnswer === currentQuestion.value.correctAnswer
      updateAbilityEstimate(isCorrect)
      adjustDifficulty(isCorrect)
    }
    
    currentIndex.value++
    loadCurrentAnswer()
    
    // 自适应模式：检查是否应该终止
    if (adaptiveEnabled.value && shouldTerminate()) {
      ElMessage.info('AI 已准确评估您的能力水平，即将生成报告')
      setTimeout(() => submitAllAnswers(), 500)
    }
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    loadCurrentAnswer()
  }
}

async function submitAllAnswers() {
  if (!allAnswered.value) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }
  
  // 自适应模式：记录最后一题
  if (adaptiveEnabled.value && currentQuestion.value) {
    const userAnswer = userAnswers.value[currentQuestion.value.id]
    const isCorrect = userAnswer === currentQuestion.value.correctAnswer
    updateAbilityEstimate(isCorrect)
    adjustDifficulty(isCorrect)
  }
  
  try {
    const res = await submitAnswers(subjectInput.value, userAnswers.value, questions.value, selectedDifficulty.value)
    resultData.value = res?.data || res
    
    // 自适应模式：添加额外数据
    if (adaptiveEnabled.value) {
      resultData.value.abilityEstimate = abilityEstimate.value
      resultData.value.abilitySE = abilitySE.value
      resultData.value.difficultyHistory = difficultyHistory.value
      resultData.value.adaptiveMode = true
    }
    
    phase.value = 'result'
  } catch (e) {
    console.error('提交答案失败:', e)
    ElMessage.error('提交失败：' + (e.message || '未知错误'))
  }
}

onMounted(async () => {
  startPlaceholderRotation()
  const savedSubject = localStorage.getItem('quizSubject')
  const savedDifficulty = localStorage.getItem('quizDifficulty')
  if (savedSubject) subjectInput.value = savedSubject
  if (savedDifficulty) selectedDifficulty.value = savedDifficulty
  try {
    const res = await getSubjects()
    if (res?.data && res.data.length > 0) {
      // 使用服务端科目列表更新快速选择
      const serverSubjects = res.data.map(s => s.label)
      if (serverSubjects.length > 0) {
        quickSubjects.value = [...new Set([...serverSubjects, ...quickSubjects])].slice(0, 10)
      }
    }
  } catch (e) { /* 忽略 */ }
  // 初始科目加载自适应推荐
  loadAdaptiveConfig(subjectInput.value)
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
.assessment-page { min-height: calc(100vh - 68px); position: relative; overflow-x: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba($accent-primary,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba($accent-primary,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

/* 雷达扫描 */
.radar-scanner {
  position: absolute;
  width: 300px;
  height: 300px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.4;
}

.radar-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba($accent-primary, 0.1);
  
  &.ring-1 { inset: 0; }
  &.ring-2 { inset: 20%; }
  &.ring-3 { inset: 40%; }
}

.radar-sweep {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(from 0deg, transparent 0deg, rgba($accent-primary, 0.15) 30deg, transparent 60deg);
  animation: radarSweep 4s linear infinite;
}

@keyframes radarSweep {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.radar-dots {
  position: absolute;
  inset: 0;
}

.radar-dot {
  position: absolute;
  width: 4px;
  height: 4px;
  background: $accent-primary;
  border-radius: 50%;
  animation: radarBlink 2s ease-in-out infinite;
}

@keyframes radarBlink {
  0%, 100% { opacity: 0.2; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.5); }
}

/* 神经网络 */
.neural-network {
  position: absolute;
  inset: 0;
  opacity: 0.3;
}

.neural-svg {
  width: 100%;
  height: 100%;
}

.neural-line {
  animation: linePulse 3s ease-in-out infinite;
}

@keyframes linePulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.8; }
}

.neural-node {
  animation: nodePulse 2s ease-in-out infinite;
}

@keyframes nodePulse {
  0%, 100% { opacity: 0.3; r: 2; }
  50% { opacity: 0.8; r: 3; }
}

.glass-card { background: rgba($bg-primary,0.5); backdrop-filter: blur(12px); border: 1px solid rgba($accent-secondary,0.08); border-radius: 16px; }

.page-header { position: relative; z-index: 1; margin-bottom: 24px; animation: slideUp 0.6s ease both; display: flex; align-items: center; }
.header-row { display: flex; align-items: flex-start; justify-content: space-between; width: 100%; margin-bottom: 4px; }
.header-left { flex: 1; }
.header-right { display: flex; align-items: center; gap: 12px; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }
.retry-btn { display: flex; align-items: center; gap: 6px; padding: 9px 18px; background: linear-gradient(135deg, rgba($accent-primary,0.12), rgba(123,97,255,0.08)); border: 1px solid rgba($accent-primary,0.15); border-radius: 10px; color: $accent-primary; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.25s ease; &:hover { transform: translateY(-1px); box-shadow: 0 0 20px rgba($accent-primary,0.12); } }

.content { max-width: 800px; margin: 0 auto; padding: 24px 32px 60px; position: relative; z-index: 1; }

/* ===== AI 智能推荐卡片 ===== */
.ai-recommendation-card {
  margin-bottom: 20px;
  padding: 20px 24px;
  border: 1px solid rgba($accent-primary, 0.15);
  background: rgba($accent-primary, 0.04);
}
.ai-rec-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.ai-rec-icon { font-size: 1.2rem; }
.ai-rec-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; }
.ai-rec-badge {
  margin-left: auto;
  padding: 3px 10px;
  background: rgba($accent-primary, 0.15);
  border-radius: 12px;
  font-size: 0.72rem;
  color: $accent-primary;
}
.ai-rec-content { margin-bottom: 14px; }
.ai-rec-message {
  font-size: 0.88rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 10px;
}
.ai-rec-reasons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.reason-tag {
  padding: 4px 10px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 6px;
  font-size: 0.72rem;
  color: $text-muted;
}
.ai-rec-action-btn {
  width: 100%;
  padding: 10px 0;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba($accent-primary, 0.15);
  }
}

/* ===== 开始页 ===== */
.welcome-card { text-align: center; padding: 50px 40px; margin-top: 40px; border-radius: 20px; }

/* ===== 科目输入区域 ===== */
.subject-input-area { margin-bottom: 20px; text-align: left; position: relative; }
.selector-label { display: flex; align-items: center; gap: 8px; font-size: 0.85rem; color: $text-secondary; margin-bottom: 12px; font-weight: 600; }
.label-icon { font-size: 1rem; }

.input-wrapper-ai {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  background: rgba($bg-primary, 0.6);
  border: 1.5px solid rgba($accent-secondary, 0.12);
  border-radius: 14px;
  transition: all 0.3s ease;
  position: relative;
  
  &:focus-within {
    border-color: $accent-primary;
    box-shadow: 0 0 20px rgba($accent-primary, 0.15);
    background: rgba($bg-primary, 0.8);
  }
}

.input-prefix-icon {
  color: $accent-primary;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  font-size: 1rem;
}

.subject-input-ai {
  flex: 1;
  padding: 14px 0;
  background: transparent;
  border: none;
  color: $text-primary;
  font-size: 0.95rem;
  outline: none;
  caret-color: $accent-primary;
  
  &::placeholder {
    color: rgba($text-muted, 0.5);
  }
}

.input-clear {
  padding: 4px 8px;
  background: rgba($text-muted, 0.2);
  border-radius: 6px;
  font-size: 0.75rem;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: rgba($accent-red, 0.2);
    color: $accent-red;
  }
}

.input-ai-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba($accent-secondary, 0.1));
  border-radius: 8px;
  font-size: 0.72rem;
  font-weight: 700;
  color: $accent-primary;
}

.ai-dot {
  width: 5px;
  height: 5px;
  background: $accent-emerald;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

/* AI 联想下拉 */
.ai-suggestions-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: rgba(15, 20, 35, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  z-index: 100;
  overflow: hidden;
}

.dropdown-fade-enter-active, .dropdown-fade-leave-active {
  transition: all 0.2s ease;
}
.dropdown-fade-enter-from, .dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.15s;
  
  &:hover {
    background: rgba($accent-primary, 0.08);
  }
  
  &:not(:last-child) {
    border-bottom: 1px solid rgba($accent-primary, 0.06);
  }
}

.sug-icon { font-size: 1rem; }

.sug-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sug-name {
  font-size: 0.88rem;
  color: $text-primary;
}

.sug-tag {
  padding: 2px 8px;
  background: rgba($accent-primary, 0.15);
  border-radius: 10px;
  font-size: 0.65rem;
  color: $accent-primary;
  font-weight: 600;
}

.sug-arrow {
  color: $text-muted;
  font-size: 0.85rem;
}

/* 测评目标预览 */
.assessment-targets {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.1);
  border-radius: 10px;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from, .slide-up-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.target-label {
  font-size: 0.78rem;
  color: $text-muted;
  flex-shrink: 0;
}

.target-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.target-tag {
  padding: 3px 10px;
  background: rgba($accent-cyan, 0.1);
  border: 1px solid rgba($accent-cyan, 0.2);
  border-radius: 12px;
  font-size: 0.72rem;
  color: $accent-cyan;
  animation: tagAppear 0.3s ease forwards;
  opacity: 0;
}

@keyframes tagAppear {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}

/* 磁吸式标签 */
.quick-subjects { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 14px; }

.quick-tag-magnetic {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: rgba($bg-primary, 0.5);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 20px;
  font-size: 0.82rem;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  animation: tagFadeIn 0.4s ease forwards;
  opacity: 0;
  
  &:hover {
    border-color: rgba($accent-primary, 0.3);
    color: $text-primary;
    background: rgba($accent-primary, 0.06);
  }
  
  &.active {
    border-color: $accent-primary;
    background: rgba($accent-primary, 0.12);
    color: $accent-primary;
    box-shadow: 0 0 16px rgba($accent-primary, 0.2);
  }
}

@keyframes tagFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.tag-check {
  font-size: 0.7rem;
  font-weight: 700;
}

/* ===== 操作按钮区域 ===== */
.action-buttons { display: flex; gap: 12px; justify-content: center; align-items: center; flex-direction: column; }

/* ===== 科目选择器（保留兼容） ===== */
.subject-selector { margin-bottom: 20px; }
.selector-label { display: block; font-size: 0.85rem; color: $text-secondary; margin-bottom: 10px; font-weight: 600; text-align: left; }
.subject-select { width: 280px; }
:deep(.subject-select .el-input__wrapper) { background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); border-radius: 10px; box-shadow: none; }
:deep(.subject-select .el-input__inner) { color: $text-primary; font-size: 0.9rem; }
:deep(.subject-select .el-select__caret) { color: $text-secondary; }

/* ===== 难度选择器 - 游戏化 ===== */
.difficulty-selector { margin-bottom: 20px; }
.difficulty-selector .selector-label { margin-bottom: 14px; }

.difficulty-options-game {
  display: flex;
  gap: 14px;
  justify-content: center;
}

.difficulty-card-game {
  position: relative;
  flex: 1;
  max-width: 160px;
  padding: 20px 16px;
  background: rgba($bg-primary, 0.5);
  border: 1.5px solid rgba($accent-secondary, 0.1);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  
  &:hover {
    transform: translateY(-4px);
    border-color: rgba($accent-secondary, 0.2);
  }
  
  &.active {
    transform: translateY(-6px) scale(1.02);
    border-color: transparent;
    
    &.easy {
      border-color: rgba($accent-emerald, 0.5);
      box-shadow: 0 8px 32px rgba($accent-emerald, 0.2);
    }
    &.medium {
      border-color: rgba($accent-primary, 0.5);
      box-shadow: 0 8px 32px rgba($accent-primary, 0.2);
    }
    &.hard {
      border-color: rgba($accent-red, 0.5);
      box-shadow: 0 8px 32px rgba($accent-red, 0.2);
    }
  }
}

.diff-bg-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.15;
  
  &.pattern-easy {
    background: linear-gradient(135deg, rgba($accent-emerald, 0.2), transparent);
  }
  &.pattern-medium {
    background: linear-gradient(135deg, rgba($accent-primary, 0.2), transparent);
  }
  &.pattern-hard {
    background: linear-gradient(135deg, rgba($accent-red, 0.2), transparent);
  }
}

.diff-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.diff-emoji {
  font-size: 2rem;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
  transition: transform 0.3s ease;
  
  .difficulty-card-game.active & {
    transform: scale(1.15);
  }
}

.diff-name {
  font-size: 1rem;
  font-weight: 700;
  color: $text-primary;
}

.diff-desc-game {
  font-size: 0.72rem;
  color: $text-muted;
}

.diff-wave {
  margin-top: 12px;
  height: 24px;
  opacity: 0.4;
  
  .difficulty-card-game.easy & { color: $accent-emerald; }
  .difficulty-card-game.medium & { color: $accent-primary; }
  .difficulty-card-game.hard & { color: $accent-red; }
}

.wave-path {
  stroke-dasharray: 200;
  animation: waveDraw 2s ease-in-out infinite;
}

@keyframes waveDraw {
  0% { stroke-dashoffset: 200; }
  50% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -200; }
}

.diff-active-glow {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  
  .difficulty-card-game.easy & {
    background: linear-gradient(90deg, transparent, $accent-emerald, transparent);
  }
  .difficulty-card-game.medium & {
    background: linear-gradient(90deg, transparent, $accent-primary, transparent);
  }
  .difficulty-card-game.hard & {
    background: linear-gradient(90deg, transparent, $accent-red, transparent);
  }
  animation: glowPulse 1.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ===== 自适应难度推荐 ===== */
.adaptive-recommend {
  margin-bottom: 20px;
  padding: 16px 18px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.18);
  border-radius: 12px;
  text-align: left;
}

.adaptive-recommend-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.adaptive-icon {
  font-size: 1.2rem;
}

.adaptive-recommend-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.adaptive-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: $accent-primary;
}

.adaptive-desc {
  font-size: 0.72rem;
  color: $text-muted;
}

.adaptive-loading {
  font-size: 0.72rem;
  color: $accent-primary;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.adaptive-recommend-body {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.adaptive-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.adaptive-item-label {
  font-size: 0.68rem;
  color: $text-muted;
}

.adaptive-item-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: $text-primary;
  font-family: 'JetBrains Mono', monospace;
}

.adaptive-switch {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.adaptive-switch input {
  width: 16px;
  height: 16px;
  accent-color: $accent-primary;
  cursor: pointer;
}

.adaptive-switch-text {
  font-size: 0.78rem;
  color: $text-secondary;
}

/* ===== 结果页答题详情 ===== */
.result-card-list { padding: 24px; margin-bottom: 20px; }
.result-question-item { padding: 16px; margin-bottom: 12px; border-radius: 12px; border: 1px solid rgba($accent-secondary,0.08); background: rgba($accent-secondary,0.02); &.correct { border-color: rgba(16,185,129,0.15); background: rgba(16,185,129,0.02); } &.wrong { border-color: rgba(239,68,68,0.15); background: rgba(239,68,68,0.02); } }
.rq-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.rq-number { font-size: 0.75rem; font-weight: 700; color: $text-muted; font-family: 'JetBrains Mono', monospace; }
.rq-badge { padding: 2px 10px; border-radius: 10px; font-size: 0.7rem; font-weight: 600; &.badge-correct { background: rgba(16,185,129,0.1); color: $accent-emerald; } &.badge-wrong { background: rgba(239,68,68,0.1); color: $accent-red; } }
.rq-text { font-size: 0.9rem; color: $text-primary; margin: 0 0 12px; line-height: 1.5; }
.rq-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.rq-option { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px; font-size: 0.82rem; color: $text-secondary; background: rgba($accent-secondary,0.03); &.rq-correct { background: rgba(16,185,129,0.06); color: $accent-emerald; } &.rq-wrong { background: rgba(239,68,68,0.06); color: $accent-red; } }
.rq-opt-letter { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 6px; background: rgba($accent-secondary,0.06); font-size: 0.72rem; font-weight: 700; flex-shrink: 0; }
.rq-opt-text { flex: 1; }
.rq-mark { font-size: 0.85rem; }
.rq-explanation { display: flex; gap: 8px; padding: 10px 12px; background: rgba(123,97,255,0.04); border: 1px solid rgba(123,97,255,0.08); border-radius: 8px; font-size: 0.78rem; color: $text-secondary; line-height: 1.5; }
.rq-explain-icon { flex-shrink: 0; }
.welcome-icon { font-size: 3rem; margin-bottom: 16px; }
.welcome-title { font-size: 1.5rem; font-weight: 800; color: $text-primary; margin: 0 0 12px; }
.welcome-desc { font-size: 0.9rem; color: $text-secondary; line-height: 1.7; margin: 0 auto 28px; max-width: 500px; }
.welcome-meta { display: flex; gap: 16px; justify-content: center; margin-bottom: 32px; flex-wrap: wrap; }
.meta-chip { display: flex; align-items: center; gap: 6px; padding: 8px 18px; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.08); border-radius: 20px; font-size: 0.82rem; color: $text-secondary; }
.meta-icon { font-size: 1rem; }

/* 仪表盘式动态参数面板 */
.dashboard-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 20px;
  margin-bottom: 24px;
  background: rgba($bg-primary, 0.4);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 14px;
}

.dashboard-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.dashboard-divider {
  width: 1px;
  height: 40px;
  background: rgba($accent-secondary, 0.1);
}

.dash-gauge {
  position: relative;
  width: 60px;
  height: 36px;
  
  svg {
    width: 100%;
    height: 100%;
  }
  
  path {
    transition: stroke-dashoffset 0.5s ease;
  }
}

.dash-value {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 1rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: $text-primary;
}

.dash-label {
  font-size: 0.7rem;
  color: $text-muted;
}

.dash-radar-mini {
  position: relative;
  width: 40px;
  height: 40px;
  cursor: pointer;
  
  svg {
    width: 100%;
    height: 100%;
  }
}

.radar-tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(15, 20, 35, 0.95);
  border: 1px solid rgba($accent-amber, 0.2);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
}

.tooltip-pop-enter-active, .tooltip-pop-leave-active {
  transition: all 0.2s ease;
}
.tooltip-pop-enter-from, .tooltip-pop-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}

.radar-dim {
  font-size: 0.68rem;
  color: $accent-amber;
  padding: 2px 6px;
  background: rgba($accent-amber, 0.1);
  border-radius: 4px;
}
.btn-start { display: inline-flex; align-items: center; gap: 8px; padding: 12px 32px; background: linear-gradient(135deg, $accent-primary, $accent-purple); border: none; border-radius: 12px; color: #fff; font-size: 1rem; font-weight: 700; cursor: pointer; transition: all 0.3s ease; &:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba($accent-primary,0.25); } &:active { transform: translateY(0); } }

/* AI 自适应模式开关 */
.adaptive-mode-section {
  margin-bottom: 24px;
}

.adaptive-mode-toggle {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: rgba($bg-primary, 0.4);
  border: 1px solid rgba($accent-primary, 0.1);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: rgba($accent-primary, 0.2);
    background: rgba($accent-primary, 0.03);
  }
}

.toggle-track {
  position: relative;
  width: 48px;
  height: 26px;
  background: rgba($text-muted, 0.2);
  border-radius: 13px;
  transition: all 0.3s ease;
  flex-shrink: 0;
  
  &.active {
    background: linear-gradient(135deg, $accent-primary, $accent-secondary);
  }
}

.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  .toggle-track.active & {
    transform: translateX(22px);
  }
}

.toggle-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: $text-primary;
}

.toggle-desc {
  font-size: 0.75rem;
  color: $text-muted;
}

.toggle-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.2), rgba($accent-secondary, 0.1));
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 600;
  color: $accent-primary;
}

.badge-pulse {
  width: 6px;
  height: 6px;
  background: $accent-emerald;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

/* ===== 自适应模式增强样式 ===== */
.adaptive-tooltip {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(15, 20, 35, 0.95);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  font-size: 0.78rem;
  color: $text-secondary;
  z-index: 100;
  
  svg {
    flex-shrink: 0;
    color: $accent-primary;
  }
}

.toggle-desc {
  &.active {
    color: $accent-primary;
    font-weight: 600;
  }
}

/* 难度指示器 */
.adaptive-difficulty-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.12), rgba($accent-secondary, 0.08));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 12px;
  animation: indicatorPulse 2s ease-in-out infinite;
}

@keyframes indicatorPulse {
  0%, 100% { box-shadow: 0 0 8px rgba($accent-primary, 0.1); }
  50% { box-shadow: 0 0 16px rgba($accent-primary, 0.2); }
}

.difficulty-label {
  font-size: 0.72rem;
  color: $text-muted;
}

.difficulty-stars {
  font-size: 0.8rem;
  letter-spacing: 1px;
}

.difficulty-level-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: $accent-primary;
}

/* AI 分析提示 */
.ai-analysis-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin-bottom: 16px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.1);
  border-radius: 10px;
  font-size: 0.82rem;
  color: $text-secondary;
}

.hint-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: rgba($accent-primary, 0.1);
  border-radius: 8px;
  color: $accent-primary;
  animation: hintPulse 2s ease-in-out infinite;
}

@keyframes hintPulse {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 1; }
}

.hint-text {
  flex: 1;
}

.hint-ability {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  color: $accent-primary;
}

/* 自适应结果卡片 */
.adaptive-result {
  border: 1px solid rgba($accent-primary, 0.2);
}

.adaptive-score {
  font-size: 3.5rem !important;
  background: linear-gradient(135deg, #a78bfa, #06b6d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 20px rgba($accent-primary, 0.4));
}

/* 难度曲线卡片 */
.difficulty-curve-card {
  padding: 20px;
  margin-bottom: 20px;
}

.curve-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: $text-primary;
  
  svg {
    color: $accent-primary;
  }
}

.curve-title {
  font-size: 0.92rem;
  font-weight: 700;
}

.curve-chart {
  display: flex;
  gap: 12px;
  height: 160px;
}

.curve-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 0.68rem;
  color: $text-muted;
  padding: 10px 0;
}

.curve-svg-wrap {
  flex: 1;
  background: rgba($accent-primary, 0.02);
  border: 1px solid rgba($accent-primary, 0.06);
  border-radius: 8px;
  padding: 10px;
}

.curve-svg {
  width: 100%;
  height: 100%;
}

.curve-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: $text-muted;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  
  &.correct {
    background: #00E676;
  }
  
  &.wrong {
    background: #FF4D4F;
  }
}

/* 渐变流光按钮 */
.btn-start-cyber {
  position: relative;
  padding: 14px 36px;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    
    .btn-bg {
      opacity: 1;
    }
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  &:active:not(:disabled) {
    transform: translateY(0);
  }
}

.btn-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, $accent-primary, $accent-purple, $accent-secondary, $accent-primary);
  background-size: 300% 300%;
  animation: gradientShift 3s ease infinite;
  opacity: 0.9;
  transition: opacity 0.3s;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.btn-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 700;
}

.btn-icon {
  font-size: 1.2rem;
}

.btn-arrow {
  transition: transform 0.2s;
  
  .btn-start-cyber:hover & {
    transform: translateX(4px);
  }
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 10px;
}

.loading-dots {
  display: flex;
  gap: 4px;
  
  span {
    width: 6px;
    height: 6px;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 50%;
    animation: loadingBounce 1.4s infinite;
    
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes loadingBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.loading-text {
  font-size: 0.9rem;
}

.btn-history-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: transparent;
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 10px;
  color: $text-muted;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    border-color: rgba($accent-primary, 0.2);
    color: $accent-primary;
    background: rgba($accent-primary, 0.04);
  }
}

/* ===== 答题页 ===== */
.quiz-progress-bar { margin-bottom: 20px; }
.progress-track { position: relative; height: 6px; background: rgba($accent-secondary,0.1); border-radius: 3px; overflow: visible; margin-bottom: 10px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, $accent-primary, $accent-purple); border-radius: 3px; transition: width 0.4s ease; }
.progress-dot { position: absolute; top: 50%; transform: translate(-50%, -50%); width: 12px; height: 12px; border-radius: 50%; background: rgba($accent-secondary,0.15); z-index: 1; transition: all 0.3s ease; &.active { background: $accent-primary; box-shadow: 0 0 8px rgba($accent-primary,0.4); } &.current { width: 16px; height: 16px; box-shadow: 0 0 12px rgba($accent-primary,0.5); } }
.progress-meta { display: flex; align-items: center; justify-content: space-between; }
.progress-text { font-size: 0.82rem; color: $text-secondary; strong { color: $accent-primary; font-family: 'JetBrains Mono', monospace; } }
.quiz-tags { display: flex; gap: 8px; align-items: center; }
.quiz-category { padding: 3px 12px; background: rgba($accent-primary,0.08); border: 1px solid rgba($accent-primary,0.15); border-radius: 12px; font-size: 0.75rem; color: $accent-primary; font-weight: 600; }
.quiz-difficulty { padding: 3px 12px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; &.diff-easy { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.2); color: $accent-emerald; } &.diff-medium { background: rgba(59,130,246,0.1); border: 1px solid rgba(59,130,246,0.2); color: $accent-blue; } &.diff-hard { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.2); color: $accent-red; } }

.quiz-card { padding: 28px 24px; margin-bottom: 16px; position: relative; }
.question-number { position: absolute; top: 16px; right: 20px; font-size: 0.7rem; font-family: 'JetBrains Mono', monospace; color: rgba($accent-secondary,0.3); font-weight: 700; letter-spacing: 1px; }
.question-text { font-size: 1.1rem; font-weight: 700; color: $text-primary; margin: 0 0 24px; line-height: 1.6; padding-right: 50px; }
.question-options { display: flex; flex-direction: column; gap: 10px; }
.option-item { display: flex; align-items: center; gap: 14px; padding: 16px 18px; background: rgba($accent-secondary,0.03); border: 1.5px solid rgba($accent-secondary,0.08); border-radius: 12px; cursor: pointer; transition: all 0.2s ease; position: relative; &:hover { border-color: rgba($accent-primary,0.15); background: rgba($accent-primary,0.02); } &.selected { border-color: $accent-primary; background: rgba($accent-primary,0.06); .option-letter { background: $accent-primary; color: $bg-primary; } } }
.option-letter { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: rgba($accent-secondary,0.08); font-size: 0.82rem; font-weight: 700; color: $text-secondary; flex-shrink: 0; transition: all 0.2s; }
.option-text { flex: 1; font-size: 0.9rem; color: $text-primary; line-height: 1.4; }
.option-check { font-size: 1rem; position: absolute; right: 16px; color: $accent-primary; }

.unanswered-tip { text-align: center; padding: 12px; margin-top: 12px; font-size: 0.82rem; color: $accent-amber; background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.15); border-radius: 10px; }

.quiz-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.action-right { display: flex; gap: 8px; margin-left: auto; }
.btn-prev, .btn-next, .btn-submit-all { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; border-radius: 10px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
.btn-prev { background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); color: $text-secondary; &:hover:not(:disabled) { background: rgba($accent-secondary,0.1); } &:disabled { opacity: 0.3; cursor: not-allowed; } }
.btn-submit-all { background: linear-gradient(135deg, $accent-emerald, $accent-primary); color: #fff; &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(16,185,129,0.25); } &:disabled { opacity: 0.4; cursor: not-allowed; } }
.btn-next { background: linear-gradient(135deg, $accent-primary, $accent-purple); color: #fff; &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba($accent-primary,0.2); } }

/* ===== 结果页 ===== */
.result-hero { display: flex; align-items: center; gap: 24px; padding: 28px 32px; margin-bottom: 20px; }
.result-score-section { text-align: center; flex-shrink: 0; }
.result-label { font-size: 0.78rem; color: $text-muted; display: block; margin-bottom: 4px; }
.result-score-wrap { display: flex; align-items: baseline; justify-content: center; gap: 2px; }
.result-score { font-size: 3rem; font-weight: 900; font-family: 'JetBrains Mono', monospace; text-shadow: 0 0 20px currentColor; line-height: 1; }
.result-total { font-size: 1.1rem; color: $text-muted; }
.result-level { display: inline-block; margin-top: 8px; padding: 4px 16px; border-radius: 16px; font-size: 0.85rem; font-weight: 600; }
.result-quick-stats { flex: 1; display: flex; justify-content: space-around; }
.quick-stat { text-align: center; }
.qs-value { display: block; font-size: 1.5rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; color: $text-primary; }
.qs-label { font-size: 0.72rem; color: $text-muted; }

.result-radar { padding: 24px; margin-bottom: 20px; }
.radar-container { height: 300px; width: 100%; }
.radar-chart { width: 100%; height: 100%; }

.btn-start:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

.section-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; margin: 0 0 16px; }

.result-details { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.result-strengths, .result-weaknesses { padding: 20px; }
.strength-item, .weakness-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid rgba($accent-secondary,0.04); &:last-child { border: none; } }
.strength-icon, .weakness-icon { font-size: 0.85rem; }
.strength-name, .weakness-name { flex: 1; font-size: 0.85rem; color: $text-secondary; }
.tag-score { font-size: 0.78rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: $accent-primary; &.low { color: $accent-red; } }
.empty-tip { font-size: 0.82rem; color: $text-muted; text-align: center; padding: 16px 0; }

/* ===== AI 诊断分析卡片 ===== */
.ai-diagnosis-card {
  margin-bottom: 20px;
  padding: 20px 24px;
  border: 1px solid rgba($accent-purple, 0.15);
  background: rgba($accent-purple, 0.04);
}
.diagnosis-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.diagnosis-icon { font-size: 1.2rem; }
.diagnosis-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; }
.diagnosis-content { margin-bottom: 14px; }
.diagnosis-message {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 10px;
}
.diagnosis-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.diag-stat {
  font-size: 0.78rem;
  color: $text-muted;
}
.diagnosis-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.btn-diagnosis, .btn-adjust-path {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-diagnosis {
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  color: $accent-primary;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-primary, 0.15);
  }
}
.btn-adjust-path {
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.12);
  color: $text-secondary;
  &:hover {
    border-color: rgba($accent-purple, 0.3);
    color: $accent-purple;
  }
}
.btn-weak-path {
  background: linear-gradient(135deg, rgba($accent-amber, 0.1), rgba($accent-red, 0.05));
  border: 1px solid rgba($accent-amber, 0.2);
  color: $accent-amber;
  &:hover {
    border-color: rgba($accent-amber, 0.4);
    box-shadow: 0 0 16px rgba($accent-amber, 0.15);
    transform: translateY(-1px);
  }
}

.result-suggestions { padding: 20px; margin-bottom: 20px; }
.suggestion-item { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid rgba($accent-secondary,0.04); &:last-child { border: none; } }
.sug-icon { font-size: 1rem; flex-shrink: 0; margin-top: 1px; }
.suggestion-item p { margin: 0; font-size: 0.82rem; color: $text-secondary; line-height: 1.6; }

.result-actions { display: flex; gap: 12px; justify-content: center; margin-bottom: 24px; }
.btn-secondary, .btn-primary { padding: 8px 20px; border-radius: 8px; font-size: 0.82rem; font-weight: 500; cursor: pointer; transition: all 0.3s ease; font-family: inherit; }
.btn-secondary { background: rgba($accent-indigo, 0.06); border: 1px solid rgba($accent-indigo, 0.12); color: $text-secondary; &:hover { border-color: rgba($accent-indigo, 0.25); color: $accent-indigo-light; background: rgba($accent-indigo, 0.1); } }
.btn-primary { background: rgba($accent-indigo, 0.1); border: 1px solid rgba($accent-indigo, 0.25); color: $accent-indigo; &:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba($accent-indigo, 0.15); background: rgba($accent-indigo, 0.18); border-color: rgba($accent-indigo, 0.4); } }

.result-history { padding: 20px; margin-bottom: 20px; }
.history-chart { display: flex; align-items: flex-end; gap: 12px; height: 120px; padding: 12px 0; }
.history-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; }
.history-bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.history-bar { width: 60%; border-radius: 6px 6px 0 0; background: linear-gradient(180deg, $accent-primary, rgba($accent-primary,0.25)); position: relative; min-height: 4px; transition: height 0.4s ease; &.latest { background: linear-gradient(180deg, $accent-purple, rgba(123,97,255,0.25)); } }
.history-bar-value { position: absolute; top: -18px; left: 50%; transform: translateX(-50%); font-size: 0.65rem; color: $text-secondary; font-family: 'JetBrains Mono', monospace; }
.history-label { font-size: 0.65rem; color: $text-muted; }

@media (max-width: 1024px) { .page-header { padding: 12px 20px; } .content { padding: 20px; } .result-details { grid-template-columns: 1fr; } }
@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .content { padding: 12px; }
  .welcome-card { padding: 30px 20px; margin-top: 20px; }
  .welcome-title { font-size: 1.2rem; }
  .welcome-meta { gap: 10px; }
  .meta-chip { padding: 6px 14px; font-size: 0.78rem; }
  .quiz-card { padding: 20px 16px; }
  .question-text { font-size: 1rem; padding-right: 30px; }
  .question-number { font-size: 0.6rem; }
  .option-item { padding: 12px 14px; }
  .option-text { font-size: 0.82rem; }
  .result-hero { flex-direction: column; padding: 20px; gap: 16px; }
  .result-score { font-size: 2.2rem; }
  .radar-container { height: 240px; }
  .result-actions { flex-direction: column; }
}

.history-dialog-container { display: contents; }
</style>