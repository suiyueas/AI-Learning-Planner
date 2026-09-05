﻿<template>
  <div ref="pageRef" class="knowledge-page">
    <!-- 页面标题 -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">📚</span>
            <span>智能知识库</span>
            <span class="title-sub">
              <span class="stat-hl">{{ displayStats.chunks }}</span> 个知识块 ·
              <span class="stat-ok">{{ displayStats.ready }} 个就绪</span>
              <span v-if="displayStats.processing > 0" class="stat-proc"> · {{ displayStats.processing }} 个处理中</span>
            </span>
          </h1>
        </div>
        <div class="header-right">
          <button v-if="authStore.isAdmin" class="btn-generate-all" @click="handleGenerateAllChunks">🔄 全量生成知识块</button>
        </div>
      </div>
    </header>

    <!-- 对话历史抽屉 -->
    <transition name="history-slide">
      <div v-if="showHistory" class="history-overlay" @click="showHistory = false">
        <div class="history-panel" @click.stop>
          <div class="history-head">
            <span>对话历史</span>
            <button class="icon-btn-sm" @click="showHistory = false">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <button class="history-new" @click="createNewConversation(); showHistory = false">+ 新建对话</button>
          <div class="history-list">
            <div v-for="conv in qaConversations" :key="conv.id" class="hist-item" :class="{ active: conv.id === currentQAConversationId }" @click="switchConversation(conv.id); showHistory = false">
              <span class="hist-title">{{ conv.title }}</span>
              <button class="hist-del" @click.stop="deleteConversation(conv.id)">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- AI 知识发现模块 -->
    <div v-if="knowledgeInsights" class="knowledge-insights-card">
      <div class="insights-header">
        <span class="insights-icon">📊</span>
        <span class="insights-title">AI 知识发现</span>
      </div>
      <div class="insights-content">
        <div class="insight-item">
          <span class="insight-dot">•</span>
          <span class="insight-text">当前知识库涵盖：<strong>{{ knowledgeInsights.domains }}</strong></span>
        </div>
        <div v-if="knowledgeInsights.latestDoc" class="insight-item">
          <span class="insight-dot">•</span>
          <span class="insight-text">最新文档：「{{ knowledgeInsights.latestDoc.title }}」已自动生成摘要</span>
        </div>
        <div v-if="knowledgeInsights.recommendedDoc" class="insight-item">
          <span class="insight-dot">•</span>
          <span class="insight-text">推荐阅读：「{{ knowledgeInsights.recommendedDoc.title }}」- 与你的学习目标匹配度 {{ knowledgeInsights.recommendedDoc.matchRate }}%</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <div v-for="(stat, i) in statCards" :key="stat.label" class="stat-card" :style="{ animationDelay: i * 0.1 + 's' }">
        <span class="stat-icon" :class="stat.color">{{ stat.icon }}</span>
        <div class="stat-info">
          <span class="stat-num">{{ displayStats[stat.key] }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 主内容区：左右分栏 -->
    <div class="content-grid">
      <!-- 左侧：文档列表 -->
      <aside class="panel-left">
        <div class="panel-hdr">
          <h3>📂 文档列表 <span class="badge">{{ filteredDocuments.length }}</span></h3>
          <button class="btn-upload" :disabled="isUploading" @click="triggerUpload">📤 上传</button>
        </div>

        <!-- 加载中 -->
        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载文档列表...</span>
        </div>

        <!-- 文档列表 -->
        <div v-else-if="filteredDocuments.length > 0" class="docs-list">
          <TransitionGroup name="doc-list">
            <div
v-for="doc in filteredDocuments" :key="doc.id" class="doc-card"
              :class="['st-' + doc.status, { active: selectedDocId === doc.id }]"
              @mouseenter="hoveredDoc = doc.id" @mouseleave="hoveredDoc = null"
              @click="selectDocument(doc)"
>
              <span class="doc-icon">{{ getDocIcon(doc.type) }}</span>
              <div class="doc-info">
                <span class="doc-name">{{ doc.title }}</span>
                <span class="doc-meta">{{ doc.type || '未知' }} · {{ doc.size || '0B' }} ·  {{ doc.chunks || 0 }}个知识块</span>
                <!-- AI 摘要（已就绪文档显示） -->
                <div v-if="doc.status === 'ready' && doc.summary" class="doc-ai-summary">
                  <span class="summary-icon">🤖</span>
                  <span class="summary-text">{{ doc.summary }}</span>
                </div>
              </div>
              <span class="doc-status" :class="getStatusClass(doc.status)">
                <span class="status-dot"></span>{{ getStatusLabel(doc.status) }}
              </span>
              <div v-show="hoveredDoc === doc.id" class="doc-actions">
                <button title="查看详情" @click.stop="handleViewDoc(doc)">👁</button>
                <button title="问AI" @click.stop="handleAskAI(doc)">💬</button>
                <button title="生成测验" @click.stop="handleGenerateQuiz(doc)">📝</button>
                <button title="删除" @click.stop="handleDelete(doc)">🗑</button>
              </div>
              <div v-if="doc.status === 'processing' || doc.status === 'uploading'" class="doc-progress">
                <div class="progress-fill" :style="{ width: getProgressWidth(doc.status) }"></div>
              </div>
            </div>
          </TransitionGroup>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-illustration">
            <div class="empty-ring ring-1"></div>
            <div class="empty-ring ring-2"></div>
            <span class="empty-icon">📭</span>
          </div>
          <p class="empty-title">暂无文档</p>
          <p class="empty-desc">上传文件开始构建知识库</p>
          <div class="empty-examples">
            <button v-for="ex in exampleDocs" :key="ex.name" class="example-chip" @click="triggerUpload">
              <span>{{ ex.icon }}</span> {{ ex.name }}
            </button>
          </div>
        </div>

        <!-- 上传区域 -->
        <div
class="upload-area" :class="{ dragover: isDragover, uploading: isUploading }"
          @dragover.prevent="isDragover = true" @dragleave="isDragover = false"
          @drop.prevent="handleDrop" @click="!isUploading && triggerUpload()"
>
          <div class="upload-inner">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" /></svg>
            <span><strong>点击或拖拽上传</strong> PDF / Word / Markdown / TXT</span>
          </div>
          <input ref="fileInput" type="file" accept=".pdf,.doc,.docx,.md,.txt" style="display:none" @change="handleFileSelect" />
        </div>
        <div v-if="isUploading" class="upload-progress">
          <div class="upload-ptext">正在上传... {{ uploadProgress }}%</div>
          <div class="upload-track"><div class="upload-fill" :style="{ width: uploadProgress + '%' }"></div></div>
        </div>
      </aside>

      <!-- 右侧：知识块可视化 -->
      <main class="panel-right">
        <div class="panel-hdr">
          <h3>🧩 知识块</h3>
          <div class="chunk-search">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
            <input
v-model="chunkSearchQuery" type="text" placeholder="搜索知识块..." class="chunk-search-input"
              @input="handleChunkSearch"
/>
            <span v-if="chunkSearchQuery" class="search-clear" @click="clearChunkSearch">✕</span>
          </div>
        </div>

        <!-- 知识块头部信息 -->
        <div v-if="selectedDoc" class="chunk-header">
          <span class="chunk-header-title">知识块：{{ currentDocName || selectedDoc.title }}</span>
          <span class="chunk-count-badge">{{ chunks.length }} 个</span>
        </div>

        <!-- 知识块加载中 -->
        <div v-if="chunkLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载知识块中...</span>
        </div>

        <!-- 搜索中 -->
        <div v-else-if="isSearching" class="loading-state">
          <div class="loading-spinner"></div>
          <span>搜索中...</span>
        </div>

        <!-- 知识块网格 -->
        <div v-else-if="displayChunks.length > 0" class="chunks-grid">
          <div
v-for="chunk in displayChunks" :key="chunk.id" class="chunk-card clickable"
            @mouseenter="hoveredChunk = chunk.id" @mouseleave="hoveredChunk = null"
            @click="openChunkDetail(chunk)"
>
            <div class="chunk-header-inner">
              <span class="chunk-source">{{ chunk.docTitle }}</span>
              <span class="chunk-index">{{ String(chunk.id).includes('search') ? '检索' : '#'.concat(chunk.index || '') }}</span>
            </div>
            <div class="chunk-preview">{{ chunk.preview }}</div>
            <div v-if="chunk.relevance" class="chunk-relevance">相关度 {{ Math.round(chunk.relevance * 100) }}%</div>
            <transition name="tooltip-fade">
              <div v-if="hoveredChunk === chunk.id" class="chunk-tooltip">
                <div class="tooltip-title">{{ chunk.docTitle }}</div>
                <div class="tooltip-content">{{ chunk.fullContent || chunk.content || chunk.preview }}</div>
                <div class="tooltip-hint">点击查看完整内容</div>
              </div>
            </transition>
          </div>
        </div>

        <!-- 相关知识图谱 -->
        <div v-if="selectedDocId && relatedDocs.length > 0" class="related-knowledge-graph">
          <div class="graph-header">
            <span class="graph-icon">🔗</span>
            <span class="graph-title">相关知识图谱</span>
          </div>
          <div class="graph-content">
            <div class="graph-center">
              <div class="center-node">
                <span class="center-icon">📄</span>
                <span class="center-text">{{ currentDocName }}</span>
              </div>
            </div>
            <div class="graph-connections">
              <div class="connection-line"></div>
              <div v-for="(doc, i) in relatedDocs.slice(0, 4)" :key="i" class="related-node" :style="{ '--delay': i * 0.1 + 's' }" @click="selectDocument(doc)">
                <div class="node-card">
                  <span class="node-icon">{{ getDocIcon(doc.type) }}</span>
                  <span class="node-title">{{ doc.title }}</span>
                  <span class="node-meta">{{ doc.chunks || 0 }} 个知识块</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空知识块 -->
        <div v-else class="chunks-empty">
          <div class="chunks-empty-icon">🧩</div>
          <p>{{ chunkSearchQuery ? '未找到匹配的知识块' : (documents.length === 0 ? '暂无知识块，请先上传文档' : '请选择一个已就绪的文档查看知识块') }}</p>
        </div>
      </main>
    </div>

    <!-- 底部：智能知识助手（多工作流版） -->
    <div class="qa-section">
      <div class="qa-hdr">
        <div class="qa-hdr-left">
          <button class="icon-btn" title="对话历史" @click="showHistory = !showHistory">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
          </button>
          <span class="qa-status"><span class="qa-dot"></span> 智能知识助手运行中</span>
        </div>
        <div class="qa-hdr-right">
          <div class="tab-switch">
            <button
              class="tab-btn"
              :class="{ active: workflowAssistantTab === 'workflow' }"
              @click="workflowAssistantTab = 'workflow'"
            >
              工作流
            </button>
            <button
              class="tab-btn"
              :class="{ active: workflowAssistantTab === 'qa' }"
              @click="workflowAssistantTab = 'qa'"
            >
              自由问答
            </button>
          </div>
        </div>
      </div>

      <!-- 工作流视图 -->
      <div v-show="workflowAssistantTab === 'workflow'" class="workflow-container">
        <WorkflowSelector
          :workflows="workflowStore.workflows"
          :current-workflow="workflowStore.currentWorkflow"
          @select="handleWorkflowSelect"
        />
        <WorkflowExecutor
          :current-workflow="workflowStore.currentWorkflow"
          :is-executing="workflowStore.isExecuting"
          :steps="workflowStore.workflowSteps"
          :workflow-result="workflowStore.workflowResult"
          :templates="workflowStore.templates"
          @execute="handleWorkflowExecute"
          @run-template="handleRunTemplate"
        />
      </div>

      <!-- 原有自由问答视图 -->
      <div v-show="workflowAssistantTab === 'qa'" class="qa-container">
        <div ref="qaMessagesRef" class="qa-messages">
          <div v-if="qaMessages.length === 0" class="qa-welcome">
            <span class="welcome-icon">💡</span>
            <h3>开始探索智能知识助手</h3>
            <p>基于知识库进行智能问答对话</p>
            <div class="welcome-tips">
              <button class="tip-chip" @click="askQuickQuestion('如何学习Python编程语言基础知识？')">🔥 学习知识库问答</button>
              <button class="tip-chip" @click="askQuickQuestion('平台支持哪些文档格式？')">📄 文档格式支持</button>
              <button class="tip-chip" @click="askQuickQuestion('如何使用 RAG 提升学习效果？')">🎯 RAG 问答</button>
            </div>
          </div>
          <div v-for="msg in qaMessages" :key="msg.id" :class="[msg.role === 'user' ? 'user-message-wrapper' : 'ai-message-wrapper']">
            <div class="message" :class="msg.role">
              <template v-if="msg.role === 'user'">
                <div class="msg-content user-msg"><div class="msg-bubble user-bubble">{{ msg.content }}</div></div>
                <div class="avatar user-av">👤</div>
              </template>
              <template v-else>
                <div class="avatar ai-av">💡</div>
                <div class="msg-content ai-msg">
                  <div class="msg-meta"><span class="ai-label">智能知识助手</span><span class="msg-time">{{ formatTime(msg.timestamp) }}</span></div>
                  <div class="msg-bubble ai-bubble markdown-body" v-html="renderMd(msg.content)"></div>
                  <div v-if="msg.sources && msg.sources.length > 0" class="sources-box">
                    <div class="sources-hdr">📎 引用来源 ({{ msg.sources.length }})</div>
                    <div v-for="(src, i) in msg.sources" :key="i" class="source-card">
                      <span class="src-tree">{{ i === msg.sources.length - 1 ? '└─' : '├─' }}</span>
                      <span class="src-title">{{ src.documentTitle || '未知文档' }}</span>
                      <span v-if="src.page" class="src-page">p.{{ src.page }}</span>
                      <span class="src-rel">{{ Math.round((src.relevance || 0.9) * 100) }}%</span>
                    </div>
                  </div>
                  <div class="msg-actions">
                    <button class="act-btn" title="复制" @click="handleCopyMessage(msg)">📋</button>
                    <button class="act-btn" title="重新生成" @click="handleRegenerate(msg)">🔄</button>
                    <button class="act-btn" :class="{ liked: msg.liked === true }" title="有用" @click="handleFeedback(msg, true)">👍</button>
                    <button class="act-btn" :class="{ liked: msg.liked === false }" title="无用" @click="handleFeedback(msg, false)">👎</button>
                  </div>
                </div>
              </template>
            </div>
          </div>
          <div v-if="isQALoading" class="ai-message-wrapper">
            <div class="message assistant">
              <div class="avatar ai-av">💡</div>
              <div class="msg-content ai-msg">
                <div class="msg-meta"><span class="ai-label">智能知识助手</span></div>
                <div class="typing-row">
                  <div class="typing-dots"><span></span><span></span><span></span></div>
                  <span>正在检索知识库...</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="qa-input-row">
          <input v-model="qaInput" type="text" class="qa-input" placeholder="输入您的问题，与智能知识库对话..." :disabled="isQALoading" @keyup.enter="handleAsk" />
          <button class="qa-send" :disabled="isQALoading || !qaInput.trim()" @click="handleAsk">
            <span v-if="isQALoading" class="send-spinner"></span>
            <span v-else>发送</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 文档详情弹窗 -->
    <div v-if="showDetail && detailDoc" class="detail-overlay" @click="closeDetail">
      <div class="detail-panel" @click.stop>
        <div class="detail-header">
          <div class="detail-title">
            <span class="detail-doc-icon">{{ getDocIcon(detailDoc.type) }}</span>
            <span>{{ detailDoc.title }}</span>
          </div>
          <button class="detail-close" @click="closeDetail">✕</button>
        </div>

        <div class="detail-body">
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="section-title">基本信息</div>
            <div class="info-grid">
              <div class="info-item"><label>文件类型</label><span>{{ detailDoc.type || '未知' }}</span></div>
              <div class="info-item"><label>文件大小</label><span>{{ formatSize(detailDoc.size) }}</span></div>
              <div class="info-item"><label>知识块数量</label><span>{{ detailDoc.chunks || 0 }} 个</span></div>
              <div class="info-item"><label>处理状态</label><span class="status-badge" :class="getStatusClass(detailDoc.status)">{{ getStatusLabel(detailDoc.status) }}</span></div>
              <div class="info-item"><label>上传时间</label><span>{{ formatFullTime(detailDoc.uploadedAt) }}</span></div>
              <div class="info-item"><label>文档ID</label><span class="doc-id-text">{{ detailDoc.id }}</span></div>
            </div>
          </div>

          <!-- 内容预览 -->
          <div v-if="detailDoc.description" class="detail-section">
            <div class="section-title">📝 内容预览</div>
            <div class="preview-content">{{ detailDoc.description }}</div>
          </div>

          <!-- 知识块预览（前5个） -->
          <div v-if="chunks.length > 0" class="detail-section">
            <div class="section-title">📚 知识块预览（前5个）</div>
            <div class="chunk-preview-list">
              <div v-for="(chunk, index) in chunks.slice(0, 5)" :key="index" class="chunk-preview-item">
                <span class="chunk-index-tag">#{{ index + 1 }}</span>
                <span class="chunk-preview-text">{{ chunk.content || chunk.preview }}</span>
              </div>
              <div v-if="chunks.length > 5" class="more-hint">
                还有 {{ chunks.length - 5 }} 个知识块...
              </div>
            </div>
          </div>
        </div>

        <div class="detail-footer">
          <button class="btn-primary" @click="closeDetail; openChunksDialog(detailDoc)">查看全部知识块</button>
          <button class="btn-secondary" @click="closeDetail">关闭</button>
        </div>
      </div>
    </div>

    <!-- 知识块列表弹窗 -->
    <div v-if="showChunksDialog && chunksDialogDoc" class="chunks-dialog-overlay" @click="closeChunksDialog">
      <div class="chunks-dialog" @click.stop>
        <div class="cd-header">
          <div class="cd-header-left">
            <span class="cd-icon">🧩</span>
            <div class="cd-header-info">
              <h3>知识块列表</h3>
              <p>{{ chunksDialogDoc.title }}</p>
            </div>
          </div>
          <button class="cd-close" @click="closeChunksDialog">✕</button>
        </div>

        <div class="cd-body">
          <div class="cd-summary">
            <span class="cd-summary-label">文档：</span>
            <span class="cd-summary-value">{{ chunksDialogDoc.title }}</span>
            <span class="cd-summary-label"> · 知识块总数：</span>
            <span class="cd-summary-count">{{ allChunks.length }} 个</span>
          </div>

          <div v-if="chunkLoading" class="cd-loading">
            <div class="loading-spinner"></div>
            <span>加载知识块中...</span>
          </div>

          <div v-else-if="allChunks.length > 0" class="cd-chunks-list">
            <div v-for="(chunk, idx) in allChunks" :key="chunk.id || idx" class="cd-chunk-item" @click="openChunkDetail(chunk)">
              <span class="cd-chunk-index">{{ (chunk.index || idx + 1) }}</span>
              <div class="cd-chunk-content">{{ chunk.content || chunk.preview || chunk.text || '无内容' }}</div>
            </div>
          </div>

          <div v-else class="cd-empty">
            <span>暂无知识块</span>
          </div>
        </div>

        <div class="cd-footer">
          <button class="cd-btn cd-btn-cancel" @click="closeChunksDialog">关闭</button>
        </div>
      </div>
    </div>

    <!-- 知识块详情弹窗 -->
    <div v-if="showChunkDetail && selectedChunk" class="chunk-detail-overlay" @click="closeChunkDetail">
      <div class="chunk-detail-dialog" @click.stop>
        <div class="cdd-header">
          <div class="cdd-header-left">
            <span class="cdd-icon">📄</span>
            <div class="cdd-header-info">
              <h3>知识块 #{{ selectedChunk.index || selectedChunk.chunkIndex }}</h3>
              <p>{{ selectedChunk.docTitle || '未知文档' }}</p>
            </div>
          </div>
          <button class="cdd-close" @click="closeChunkDetail">✕</button>
        </div>
        <div class="cdd-body">
          <div class="cdd-meta">
            <span class="cdd-meta-item">字符数：{{ selectedChunk.charCount || selectedChunk.content?.length || 0 }}</span>
            <span v-if="selectedChunk.chunkIndex !== undefined" class="cdd-meta-item">序号：#{{ selectedChunk.chunkIndex }}</span>
          </div>
          <div class="cdd-content">{{ selectedChunk.fullContent || selectedChunk.content }}</div>
        </div>
        <div class="cdd-footer">
          <button class="cdd-btn" @click="closeChunkDetail">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useKnowledgeStore } from '@/stores/knowledgeStore'
import { useKnowledgeWorkflowStore } from '@/stores/knowledgeWorkflow'
import WorkflowSelector from '@/components/knowledge/WorkflowSelector.vue'
import WorkflowExecutor from '@/components/knowledge/WorkflowExecutor.vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import { getDocumentChunks, generateAllChunks } from '@/api/knowledgeApi'
import { confirmAction, cleanupDialogs } from '@/utils/modalHelper'
import { securityFilter } from '@/utils/securityUtils'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const knowledgeStore = useKnowledgeStore()
const workflowStore = useKnowledgeWorkflowStore()
const pageRef = ref(null)
const qaMessagesRef = ref(null)
const fileInput = ref(null)
const hoveredDoc = ref(null)
const hoveredChunk = ref(null)
const isDragover = ref(false)
const chunkSearchQuery = ref('')
const showHistory = ref(false)
const chunkSearchTimer = ref(null)
const workflowAssistantTab = ref('workflow')

// ===== 知识块切换 =====
const chunks = ref([])
const chunkLoading = ref(false)
const currentDocName = ref('')
const selectedDocId = ref(null)
const relatedDocs = ref([])

// ===== 详情弹窗 =====
const showDetail = ref(false)
const detailDoc = ref(null)

// ===== 知识块列表弹窗 =====
const showChunksDialog = ref(false)
const chunksDialogDoc = ref(null)
const allChunks = ref([])

// ===== 知识块详情弹窗 =====
const showChunkDetail = ref(false)
const selectedChunk = ref(null)

// ===== Store 状态 =====
const documents = computed(() => knowledgeStore.documents)
const filteredDocuments = computed(() => knowledgeStore.filteredDocuments)
const isLoading = computed(() => knowledgeStore.isLoading)
const isUploading = computed(() => knowledgeStore.isUploading)
const uploadProgress = computed(() => knowledgeStore.uploadProgress)
const qaMessages = computed(() => knowledgeStore.qaMessages)
const qaInput = computed({ get: () => knowledgeStore.qaInput, set: v => { knowledgeStore.qaInput = v } })
const isQALoading = computed(() => knowledgeStore.isQALoading)
const qaConversations = computed(() => knowledgeStore.qaConversations)
const currentQAConversationId = computed(() => knowledgeStore.currentQAConversationId)
const selectedDoc = computed(() => knowledgeStore.selectedDocument)
const isSearching = computed(() => knowledgeStore.isSearching)
const searchResults = computed(() => knowledgeStore.searchResults)

// ===== 对话历史管理 =====
const createNewConversation = () => {
  knowledgeStore.createQAConversation()
  ElMessage.success('已创建新对话')
}
const switchConversation = (id) => knowledgeStore.switchQAConversation(id)
const deleteConversation = async (id) => {
  // 单例确认弹窗，防止误删与弹窗堆叠
  try {
    await confirmAction('确定要删除这条对话记录吗？删除后不可恢复。', '删除对话', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch { return }
  knowledgeStore.deleteQAConversation(id)
  ElMessage.success('已删除对话')
}

// ===== 统计卡片 =====
const displayStats = reactive({ total: 0, ready: 0, processing: 0, chunks: 0 })

const getLiveStats = () => ({
  total: documents.value.length,
  ready: documents.value.filter(d => d.status === 'ready').length,
  processing: documents.value.filter(d => d.status === 'processing' || d.status === 'uploading').length,
  chunks: documents.value.reduce((s, d) => s + (d.chunks || 0), 0)
})

// ===== AI 知识发现 =====
const knowledgeInsights = computed(() => {
  const docs = documents.value
  if (docs.length === 0) return null

  const readyDocs = docs.filter(d => d.status === 'ready')
  if (readyDocs.length === 0) {
    return {
      domains: '暂无已就绪文档',
      latestDoc: null,
      recommendedDoc: null
    }
  }

  // 提取文档类型作为领域
  const docTypes = [...new Set(readyDocs.map(d => d.type || '未知'))]
  const domains = docTypes.slice(0, 3).join('、')

  // 找到最新上传的文档
  const latestDoc = [...readyDocs]
    .sort((a, b) => new Date(b.uploadedAt || 0) - new Date(a.uploadedAt || 0))
    .find(d => d.uploadedAt)

  // 模拟推荐文档（实际应该基于用户学习目标）
  const recommendedDoc = readyDocs.length > 1 ? {
    title: readyDocs[1]?.title || readyDocs[0]?.title,
    matchRate: 85 + Math.floor(Math.random() * 15)
  } : null

  return {
    domains: domains || '通用文档',
    latestDoc: latestDoc ? { title: latestDoc.title } : null,
    recommendedDoc
  }
})

const statCards = computed(() => [
  { key: 'total', icon: '📄', label: '全部文档', color: 'cyan' },
  { key: 'ready', icon: '✅', label: '已就绪', color: 'emerald' },
  { key: 'processing', icon: '⏳', label: '处理中', color: 'amber' },
  { key: 'chunks', icon: '📦', label: '知识块', color: 'purple' }
])

const animateCountUp = (targets) => {
  const dur = 800
  const t0 = performance.now()
  const start = { ...displayStats }
  const step = (t) => {
    const p = Math.min((t - t0) / dur, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    for (const key of ['total', 'ready', 'processing', 'chunks']) {
      displayStats[key] = Math.round(start[key] + (targets[key] - start[key]) * ease)
    }
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(() => getLiveStats(), (stats) => { animateCountUp(stats) }, { deep: true, immediate: true })

// ===== 文档选择 =====
const selectDocument = async (doc) => {
  if (!doc) return

  if (doc.status !== 'ready') {
    ElMessage.warning('该文档尚未处理完成，无法查看知识块')
    return
  }

  selectedDocId.value = doc.id
  currentDocName.value = doc.title
  knowledgeStore.selectDocument(doc)

  // 获取相关文档（排除当前文档）
  relatedDocs.value = documents.value.filter(d => d.id !== doc.id && d.status === 'ready').slice(0, 4)

  chunkLoading.value = true
  try {
    const res = await getDocumentChunks(doc.id)
    chunks.value = res.data || []
  } catch (error) {
    console.error('加载知识块失败', error)
    ElMessage.error('加载知识块失败')
    chunks.value = []
  } finally {
    chunkLoading.value = false
  }
}

// ===== 知识块 =====
const displayChunks = computed(() => {
  if (chunkSearchQuery.value && searchResults.value.length > 0) {
    return searchResults.value
  }
  if (chunkSearchQuery.value) return []
  return chunks.value
})

const handleChunkSearch = () => {
  if (chunkSearchTimer.value) clearTimeout(chunkSearchTimer.value)
  chunkSearchTimer.value = setTimeout(async () => {
    if (chunkSearchQuery.value.trim()) {
      await knowledgeStore.searchChunks(chunkSearchQuery.value)
    } else {
      searchResults.value = []
    }
    chunkSearchTimer.value = null
  }, 300)
}

const clearChunkSearch = () => {
  chunkSearchQuery.value = ''
  searchResults.value = []
}

// ===== 示例文档 =====
const exampleDocs = [
  { icon: '📄', name: 'Python入门指南.pdf' },
  { icon: '📝', name: '学习方法论.docx' },
  { icon: '📋', name: '机器学习笔记.md' }
]

// ===== 工具方法 =====
const getDocIcon = (type) => ({ PDF: '📄', Word: '📝', Markdown: '📋', TXT: '📃', HTML: '🌐' }[type] || '📄')
const getStatusLabel = (status) => ({ ready: '就绪', processing: '处理中', uploading: '上传中', error: '错误' }[status] || '未知')
const getStatusClass = (status) => ({ ready: 'ready', processing: 'processing', uploading: 'uploading', error: 'error' }[status] || '')
const getProgressWidth = (status) => ({ ready: '100%', processing: '65%', uploading: '30%' }[status] || '0%')
const formatTime = (date) => { if (!date) return ''; const d = new Date(date); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
const formatFullTime = (date) => { if (!date) return '-'; const d = new Date(date); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }
const formatSize = (size) => { if (!size) return '0B'; if (typeof size === 'string') return size; const units = ['B','KB','MB','GB']; let i = 0; let s = size; while (s >= 1024 && i < units.length - 1) { s /= 1024; i++; } return s.toFixed(i === 0 ? 0 : 1) + units[i]; }
const scrollToBottom = () => { nextTick(() => { if (qaMessagesRef.value) qaMessagesRef.value.scrollTop = qaMessagesRef.value.scrollHeight }) }

// ===== 文档操作 =====
const handleViewDoc = (doc) => {
  detailDoc.value = doc
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  detailDoc.value = null
}

const openChunksDialog = async (doc) => {
  chunksDialogDoc.value = doc
  showChunksDialog.value = true
  chunkLoading.value = true
  try {
    const res = await getDocumentChunks(doc.id)
    allChunks.value = res.data || []
  } catch (error) {
    console.error('加载知识块失败', error)
    allChunks.value = []
  } finally {
    chunkLoading.value = false
  }
}

const closeChunksDialog = () => {
  showChunksDialog.value = false
  chunksDialogDoc.value = null
  allChunks.value = []
}

const openChunkDetail = (chunk) => {
  selectedChunk.value = chunk
  showChunkDetail.value = true
}

const closeChunkDetail = () => {
  showChunkDetail.value = false
  selectedChunk.value = null
}

const handleDelete = async (doc) => {
  try {
    // 单例确认弹窗：同一时刻只有一个删除确认框，避免重复弹窗堆叠
    await confirmAction(
      `确定要删除文档「${doc.title}」吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await knowledgeStore.deleteDocument(doc.id)
    ElMessage.success(`「${doc.title}」已删除`)
    // 如果删除的是当前选中的文档，自动选择下一个
    if (selectedDocId.value === doc.id) {
      const nextReady = documents.value.find(d => d.status === 'ready' && d.id !== doc.id)
      if (nextReady) {
        await selectDocument(nextReady)
      } else {
        selectedDocId.value = null
        currentDocName.value = ''
        chunks.value = []
        knowledgeStore.selectDocument(null)
      }
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const handleAskAI = (doc) => {
  qaInput.value = `基于文档「${doc.title}」，请回答：`
  const qaSection = document.querySelector('.qa-input')
  if (qaSection) qaSection.focus()
}

const handleGenerateQuiz = (doc) => {
  router.push({
    path: '/modules/assessment',
    query: {
      from: 'knowledge',
      docId: doc.id,
      docTitle: doc.title
    }
  })
}

// ===== 全量生成知识块 =====
const handleGenerateAllChunks = async () => {
  try {
    await confirmAction(
      '确定要为所有文档生成知识块吗？这将重新处理所有已上传的文档。',
      '全量生成知识块',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await generateAllChunks()
    ElMessage.success('全量生成已启动，请稍后刷新页面查看结果')
    setTimeout(() => knowledgeStore.loadAll(), 3000)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

// ===== 文件上传 =====
const triggerUpload = () => { if (!isUploading.value) fileInput.value?.click() }

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) handleUpload(file)
  event.target.value = ''
}

const handleDrop = (event) => {
  isDragover.value = false
  const file = event.dataTransfer.files[0]
  if (file) handleUpload(file)
}

const handleUpload = async (file) => {
  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return
  }
  try {
    await knowledgeStore.uploadDocument(file)
    ElMessage.success(`「${file.name}」上传成功`)
    // 自动选择新上传的文档
    const newest = documents.value[0]
    if (newest) await selectDocument(newest)
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

// ===== 智能问答 =====
const handleAsk = () => {
  if (!qaInput.value.trim() || isQALoading.value) return
  
  // 前端安全检查
  const safetyResult = securityFilter.sanitize(qaInput.value)
  if (safetyResult.action === 'BLOCK') {
    ElMessage.error(safetyResult.message || '输入包含不允许的内容')
    return
  }
  if (safetyResult.riskLevel === 'MEDIUM') {
    console.warn('[Security] 检测到潜在风险输入:', safetyResult.detectedTypes)
  }
  
  knowledgeStore.askQuestion(qaInput.value)
}

const askQuickQuestion = (question) => {
  qaInput.value = question
  handleAsk()
}

// ===== 工作流执行 =====
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const handleWorkflowSelect = (workflowId) => {
  workflowStore.selectWorkflow(workflowId)
}

const handleWorkflowExecute = async ({ workflow, query }) => {
  if (!query.trim()) return

  workflowStore.setExecuting(true)
  workflowStore.clearSteps()
  workflowStore.setResult('')

  try {
    switch (workflow) {
      case 'retrieve':
        await executeRetrieveWorkflow(query)
        break
      case 'extract':
        await executeExtractWorkflow(query)
        break
      case 'quiz':
        await executeQuizWorkflow(query)
        break
      case 'summarize':
        await executeSummarizeWorkflow(query)
        break
      default:
        await executeRetrieveWorkflow(query)
    }
  } catch (error) {
    ElMessage.error('执行失败: ' + error.message)
  } finally {
    workflowStore.setExecuting(false)
  }
}

const executeRetrieveWorkflow = async (query) => {
  workflowStore.addStep('解析问题', '📝', 'processing', '正在分析你的问题...')
  await sleep(500)
  workflowStore.updateStep('解析问题', 'done', '已理解查询意图')

  workflowStore.addStep('向量检索', '🔍', 'processing', '正在知识库中查找相关内容...')
  const searchResult = await knowledgeStore.searchChunks(query)
  workflowStore.updateStep('向量检索', 'done', `找到 ${searchResult?.length || 0} 个相关片段`)

  workflowStore.addStep('重排序', '📊', 'processing', '正在筛选最相关内容...')
  await sleep(500)
  const topK = Math.min(searchResult?.length || 0, 3)
  workflowStore.updateStep('重排序', 'done', `筛选出 Top ${topK} 个最相关片段`)

  workflowStore.addStep('生成回答', '🤖', 'processing', 'AI 正在生成回答...')
  await sleep(800)
  const topResults = searchResult?.slice(0, 3).map(r => r.content || r.preview).join('\n\n') || ''
  const answer = topResults
    ? `根据知识库检索结果，关于"${query}"的回答如下：\n\n**相关内容片段：**\n${topResults}`
    : `根据知识库检索结果，关于"${query}"的回答如下：\n\n相关文档片段已找到，AI正在整合信息生成完整回答。`
  workflowStore.setResult(answer)
  workflowStore.updateStep('生成回答', 'done', '回答已生成')

  workflowStore.addToHistory({
    workflowId: 'retrieve',
    workflowLabel: '智能检索',
    query,
    result: answer
  })
}

const executeExtractWorkflow = async (query) => {
  workflowStore.addStep('解析文档', '📄', 'processing', '正在分析文档内容...')
  await sleep(500)
  workflowStore.updateStep('解析文档', 'done', '文档解析完成')

  workflowStore.addStep('知识点提取', '🔑', 'processing', '正在提取核心知识点...')
  await sleep(800)
  workflowStore.updateStep('知识点提取', 'done', '已提取关键信息')

  workflowStore.addStep('结构化整理', '✍️', 'processing', '正在整理知识点结构...')
  await sleep(500)
  workflowStore.setResult(`## 知识点提取结果\n\n针对"${query}"，从知识库中提取到以下关键知识点：\n\n1. **核心概念**：相关主题的基本定义和范畴\n2. **主要特性**：该主题的关键特征和属性\n3. **应用场景**：实际应用中的典型用例\n4. **关联知识**：与该主题相关的其他知识点`)
  workflowStore.updateStep('结构化整理', 'done', '整理完成')

  workflowStore.addToHistory({
    workflowId: 'extract',
    workflowLabel: '知识点提取',
    query,
    result: workflowStore.workflowResult
  })
}

const executeQuizWorkflow = async (query) => {
  workflowStore.addStep('分析主题', '📚', 'processing', '正在分析出题主题...')
  await sleep(500)
  workflowStore.updateStep('分析主题', 'done', '主题分析完成')

  workflowStore.addStep('生成题目', '✍️', 'processing', '正在生成测验题目...')
  await sleep(800)
  workflowStore.updateStep('生成题目', 'done', '已生成5道题目')

  workflowStore.addStep('答案整理', '✅', 'processing', '正在整理答案...')
  await sleep(500)
  workflowStore.setResult(`## 测验题目\n\n针对"${query}"，生成以下5道测验题：\n\n### 选择题\n\n**1. 以下哪项是关于该主题的正确描述？**\n- A. 选项一\n- B. 选项二\n- C. 选项三\n- D. 选项四\n\n**答案：B**\n\n**2. 该主题的核心特点是什么？**\n- A. 特点一\n- B. 特点二\n- C. 特点三\n- D. 特点四\n\n**答案：A**\n\n### 判断题\n\n**3. 以下说法是否正确：该主题具有XXX特性**\n\n**答案：✓ 正确**\n\n### 简答题\n\n**4. 请简述该主题的主要应用场景。**\n\n**参考答案**：主要应用在...领域\n\n**5. 比较该主题与其他相关概念的异同点。**\n\n**参考答案**：相同点在于...，不同点在于...`)
  workflowStore.updateStep('答案整理', 'done', '整理完成')

  workflowStore.addToHistory({
    workflowId: 'quiz',
    workflowLabel: '生成测验题',
    query,
    result: workflowStore.workflowResult
  })
}

const executeSummarizeWorkflow = async (query) => {
  workflowStore.addStep('获取文档', '📥', 'processing', '正在获取文档内容...')
  await sleep(500)
  workflowStore.updateStep('获取文档', 'done', '文档获取完成')

  workflowStore.addStep('内容分析', '🔍', 'processing', '正在分析文档结构...')
  await sleep(600)
  workflowStore.updateStep('内容分析', 'done', '分析完成')

  workflowStore.addStep('生成摘要', '✍️', 'processing', '正在生成精华摘要...')
  await sleep(700)
  workflowStore.setResult(`## 文档摘要\n\n**主题**：${query}\n\n### 核心内容\n\n本文档主要介绍了该主题的核心概念、关键特性和实际应用。\n\n### 主要要点\n\n1. **概述**：文档首先介绍了该主题的基本概念和背景\n2. **详细说明**：接着深入讲解了相关原理和机制\n3. **实践应用**：最后提供了具体的应用案例和操作指南\n\n### 总结\n\n通过学习本文档，您将掌握该主题的基础知识，并能够将其应用到实际场景中。`)
  workflowStore.updateStep('生成摘要', 'done', '摘要生成完成')

  workflowStore.addToHistory({
    workflowId: 'summarize',
    workflowLabel: '文档总结',
    query,
    result: workflowStore.workflowResult
  })
}

const handleRunTemplate = (template) => {
  workflowStore.selectWorkflow(template.workflow)
  handleWorkflowExecute({ workflow: template.workflow, query: template.query })
}

// ===== Markdown 渲染 =====
const renderMd = (content) => renderMarkdown(content || '')

// ===== 消息操作 =====
const handleCopyMessage = async (m) => {
  try {
    await navigator.clipboard.writeText(m.content)
    ElMessage.success('已复制')
  } catch { ElMessage.error('复制失败') }
}

const handleRegenerate = (m) => {
  const i = qaMessages.value.findIndex(x => x.id === m.id)
  if (i > 0 && qaMessages.value[i - 1]?.role === 'user') {
    const prevQuestion = qaMessages.value[i - 1].content
    // 移除旧回复
    qaMessages.value.splice(i, 1)
    // 移除问题
    qaMessages.value.splice(i - 1, 1)
    // 重新提问
    qaInput.value = prevQuestion
    handleAsk()
  } else {
    ElMessage.warning('无法重新生成')
  }
}

const handleFeedback = (m, liked) => {
  const i = qaMessages.value.findIndex(x => x.id === m.id)
  if (i > -1) {
    // 如果点击相同按钮，取消点赞/踩
    qaMessages.value[i].liked = qaMessages.value[i].liked === liked ? null : liked
  }
  ElMessage.success(liked ? '已标记有用' : '已标记无用')
}

watch(() => qaMessages.value.length, () => { scrollToBottom() })

// ===== 生命周期 =====
onMounted(async () => {
  await knowledgeStore.loadAll()
  // 默认选择第一个已就绪文档
  const firstReady = documents.value.find(d => d.status === 'ready')
  if (firstReady) await selectDocument(firstReady)
  // 如果存在处理中的文档，自动启动轮询等待完成
  const hasProcessing = documents.value.some(
    d => d.status === 'processing' || d.status === 'uploading'
  )
  if (hasProcessing) {
    knowledgeStore.startStatusPolling()
  }

  // 从智能通知中心跳转（开始复习）：自动搜索对应知识点
  if (route.query.keyword) {
    chunkSearchQuery.value = String(route.query.keyword)
    await knowledgeStore.searchChunks(chunkSearchQuery.value)
  }
})

// keep-alive 恢复时重新加载（切换用户后进入页面不残留上一用户数据）
onActivated(async () => {
  if (authStore.isAuthenticated) {
    await knowledgeStore.loadAll()
  }
})

onUnmounted(() => {
  if (chunkSearchTimer.value) clearTimeout(chunkSearchTimer.value)
  knowledgeStore.stopStatusPolling()
  // 清理未关闭的确认弹窗，防止路由切换后残留堆叠
  cleanupDialogs()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
/* ===== 页面 ===== */
.knowledge-page {
  min-height: 100vh;
  background: $bg-primary;
  position: relative;
  overflow: hidden;
  padding: 32px 24px 60px;
}

/* ===== 页面标题 ===== */
.page-header { position: relative; z-index: 1; margin-bottom: 24px; animation: slideUp 0.6s ease both; }
.header-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 4px; }
.header-left { flex: 1; }
.header-right { display: flex; align-items: center; gap: 12px; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; }
.stat-hl { color: $accent-primary; font-weight: 600; }
.btn-generate-all {
  align-self: flex-start;
  margin-top: 8px;
  padding: 8px 16px;
  background: var(--btn-gradient);
  border: none;
  color: #ffffff;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(124,107,245,0.4);
  }
}
.stat-ok { color: $accent-emerald; }
.stat-proc { color: $accent-amber; }

/* ===== AI 知识发现卡片 ===== */
.knowledge-insights-card {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 12px;
  position: relative;
  z-index: 1;
}
.insights-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.insights-icon { font-size: 1rem; }
.insights-title { font-size: 0.88rem; font-weight: 700; color: $text-primary; }
.insights-content { display: flex; flex-direction: column; gap: 6px; }
.insight-item { display: flex; align-items: flex-start; gap: 8px; }
.insight-dot { color: $accent-primary; font-size: 1rem; line-height: 1.4; }
.insight-text { font-size: 0.82rem; color: $text-secondary; line-height: 1.5; strong { color: $accent-primary; font-weight: 600; } }

/* ===== 统计卡片 ===== */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; position: relative; z-index: 1; }
.stat-card { display: flex; align-items: center; gap: 12px; padding: 14px 18px; background: rgba($bg-primary,0.6); backdrop-filter: blur(12px); border: 1px solid rgba($accent-secondary,0.1); border-radius: 12px; transition: all 0.3s ease; animation: statEnter 0.5s ease both; position: relative; overflow: hidden; &:hover { transform: translateY(-2px); border-color: rgba($accent-primary,0.25); box-shadow: 0 4px 20px rgba($accent-primary,0.08); } }
.stat-icon { font-size: 1.4rem; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-num { font-size: 1.4rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; background: linear-gradient(135deg, $accent-primary, #0055FF); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.stat-label { font-size: 0.7rem; color: var(--text-sub); font-weight: 500; }

/* ===== 主内容区 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 40% 60%;
  gap: 16px;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
  height: calc(100vh - 300px);
  min-height: 500px;
  overflow: hidden;
}

/* ===== 面板通用 ===== */
.panel-left, .panel-right {
  display: flex;
  flex-direction: column;
  background: rgba($bg-primary,0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 14px;
  overflow: hidden;
}
.panel-hdr { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid rgba($accent-secondary,0.08); flex-wrap: wrap; gap: 8px; position: relative; h3 { font-size: 0.95rem; font-weight: 600; color: var(--title-section); } .badge { font-size: 0.7rem; padding: 2px 8px; background: var(--badge-bg); color: #ffffff; font-weight: 700; border-radius: 8px; margin-left: 6px; } }
.btn-upload { padding: 5px 12px; background: var(--btn-gradient); border: none; color: #fff; border-radius: 8px; font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: all 0.2s; &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124,107,245,0.4); } &:disabled { opacity: 0.5; cursor: not-allowed; } }

.panel-hdr::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 0 2px 2px 0;
  background: linear-gradient(180deg, #7c6bf5, #5b4bd5);
  box-shadow: 0 0 8px rgba(124, 107, 245, 0.4);
}
.panel-hdr { padding-left: 18px; }

/* ===== 加载状态 ===== */
.loading-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-sub); font-size: 0.85rem; padding: 40px; }
.loading-spinner { width: 28px; height: 28px; border: 3px solid rgba($accent-primary,0.1); border-top-color: $accent-primary; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 文档列表 ===== */
.docs-list { flex: 1; overflow-y: auto; padding: 8px; scrollbar-width: thin; scrollbar-color: rgba($accent-primary,0.12) transparent; &::-webkit-scrollbar { width: 3px; } &::-webkit-scrollbar-thumb { background: rgba($accent-primary,0.12); border-radius: 2px; } }
.doc-card { position: relative; display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: all 0.2s; margin-bottom: 4px; &:hover { background: rgba($accent-primary,0.04); } &.active { background: rgba($accent-primary,0.06); border: 1px solid rgba($accent-primary,0.12); } &.st-ready { border-left: 3px solid $accent-emerald; } &.st-processing { border-left: 3px solid $accent-amber; } &.st-uploading { border-left: 3px solid $accent-blue; } &.st-error { border-left: 3px solid $accent-red; } }
.doc-icon { font-size: 1.3rem; flex-shrink: 0; }
.doc-info { flex: 1; min-width: 0; }
.doc-name { font-size: 0.85rem; font-weight: 600; color: $text-primary; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-meta { font-size: 0.7rem; color: var(--text-sub); }
.doc-status { display: flex; align-items: center; gap: 4px; font-size: 0.7rem; font-weight: 500; flex-shrink: 0; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.ready { color: var(--status-ready-text); .status-dot { background: var(--status-ready-text); } } &.processing { color: $accent-amber; .status-dot { background: $accent-amber; animation: pulse 2s infinite; } } &.uploading { color: $accent-blue; .status-dot { background: $accent-blue; animation: pulse 2s infinite; } } &.error { color: $accent-red; .status-dot { background: $accent-red; } } }
.doc-actions { display: flex; gap: 4px; flex-shrink: 0; button { padding: 3px 6px; background: rgba($bg-primary,0.8); border: 1px solid rgba($accent-secondary,0.15); border-radius: 5px; font-size: 0.7rem; cursor: pointer; transition: all 0.15s; &:hover { border-color: $accent-primary; background: rgba($accent-primary,0.08); } } }
.doc-ai-summary {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-top: 4px;
  padding: 4px 8px;
  background: rgba($accent-primary, 0.04);
  border-radius: 4px;
}
.summary-icon { font-size: 0.7rem; flex-shrink: 0; }
.summary-text {
  font-size: 0.68rem;
  color: $text-muted;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.doc-progress { position: absolute; bottom: 0; left: 12px; right: 12px; height: 2px; background: rgba($accent-secondary,0.1); border-radius: 1px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, $accent-primary, #0055FF); border-radius: 1px; transition: width 0.5s ease; }

/* ===== 空状态 ===== */
.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px 20px; }
.empty-illustration { position: relative; width: 80px; height: 80px; margin-bottom: 16px; display: flex; align-items: center; justify-content: center; }
.empty-ring { position: absolute; border-radius: 50%; border: 1px solid rgba($accent-primary,0.1); animation: ring-rotate 20s linear infinite; }
.ring-1 { width: 80px; height: 80px; }
.ring-2 { width: 60px; height: 60px; animation-direction: reverse; animation-duration: 15s; border-color: rgba(123,97,255,0.1); }
.empty-icon { font-size: 2rem; position: relative; z-index: 1; }
@keyframes ring-rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.empty-title { font-size: 1rem; font-weight: 600; color: #ffffff; margin-bottom: 4px; }
.empty-desc { font-size: 0.85rem; color: var(--text-sub); margin-bottom: 14px; }
.empty-examples { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.example-chip { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: rgba($accent-primary,0.04); border: 1px solid rgba($accent-primary,0.1); border-radius: 16px; font-size: 0.75rem; color: $text-secondary; cursor: pointer; transition: all 0.2s; &:hover { background: rgba($accent-primary,0.08); border-color: rgba($accent-primary,0.2); color: $accent-primary; } }

/* ===== 上传区域 ===== */
.upload-area { margin: 8px; padding: 16px; border: 2px dashed rgba($accent-secondary,0.15); border-radius: 12px; text-align: center; cursor: pointer; transition: all 0.3s; &:hover, &.dragover { border-color: rgba($accent-primary,0.3); background: rgba($accent-primary,0.02); } &.uploading { opacity: 0.6; cursor: wait; } }
.upload-inner { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 0.8rem; color: $text-muted; strong { color: $accent-primary; font-weight: 600; } }
.upload-progress { padding: 4px 16px 12px; }
.upload-ptext { font-size: 0.75rem; color: $text-secondary; margin-bottom: 4px; }
.upload-track { height: 3px; background: rgba($accent-secondary,0.1); border-radius: 2px; overflow: hidden; }
.upload-fill { height: 100%; background: linear-gradient(90deg, $accent-primary, $accent-emerald); border-radius: 2px; transition: width 0.3s; }

/* ===== 知识块面板 ===== */
.chunk-search { display: flex; align-items: center; gap: 6px; padding: 5px 10px; background: rgba($bg-primary,0.6); border: 1px solid rgba($accent-secondary,0.1); border-radius: 8px; transition: border-color 0.2s; flex-shrink: 0; &:focus-within { border-color: rgba($accent-primary,0.25); } svg { color: $text-muted; flex-shrink: 0; } }
.chunk-search-input { flex: 1; min-width: 100px; background: transparent; border: none; color: var(--text-input); font-size: 0.8rem; outline: none; caret-color: var(--title-status); &::placeholder { color: var(--text-placeholder); } }
.search-clear { cursor: pointer; color: $text-muted; font-size: 0.75rem; &:hover { color: $text-primary; } }
// 知识块
.chunks-grid {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
  align-content: start;
  scrollbar-width: thin;
  scrollbar-color: rgba($accent-primary,0.12) transparent;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb {
    background: rgba($accent-primary,0.12);
    border-radius: 2px;
  }
}
.chunk-card { position: relative; padding: 12px; background: rgba($bg-primary,0.5); backdrop-filter: blur(8px); border: 1px solid rgba($accent-secondary,0.08); border-radius: 10px; transition: all 0.25s; cursor: default; &.clickable { cursor: pointer; } &:hover { transform: translateY(-2px); border-color: rgba($accent-primary,0.2); box-shadow: 0 4px 16px rgba(0,0,0,0.2); } }
.chunk-header-inner { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.chunk-source { font-size: 0.7rem; font-weight: 600; color: $accent-primary; background: rgba($accent-primary,0.08); padding: 2px 8px; border-radius: 10px; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chunk-index { font-size: 0.65rem; color: $text-muted; font-family: monospace; }
.chunk-preview { font-size: 0.78rem; color: $text-secondary; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.chunk-relevance { font-size: 0.65rem; color: $accent-emerald; margin-top: 4px; }
.chunk-tooltip { position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%); width: 280px; padding: 12px; background: rgba(10,14,26,0.96); backdrop-filter: blur(12px); border: 1px solid rgba($accent-primary,0.15); border-radius: 10px; box-shadow: 0 8px 30px rgba(0,0,0,0.5); z-index: 20; pointer-events: none; }
.tooltip-title { font-size: 0.75rem; font-weight: 600; color: $accent-primary; margin-bottom: 6px; }
.tooltip-content { font-size: 0.78rem; color: #d0d0f0; line-height: 1.6; max-height: 120px; overflow-y: auto; }
.tooltip-hint { font-size: 0.7rem; color: $text-muted; margin-top: 8px; text-align: center; font-style: italic; }
.tooltip-fade-enter-active, .tooltip-fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.tooltip-fade-enter-from, .tooltip-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(4px); }
// 空知识块
.chunks-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-sub);
  margin: 8px;
  border: 1px dashed rgba(106, 112, 144, 0.35);
  border-radius: 10px;
  background: rgba(30, 38, 56, 0.25);
  .chunks-empty-icon { font-size: 2.5rem; margin-bottom: 12px; opacity: 0.4; }
  p { font-size: 0.85rem; text-align: center; padding: 0 20px; }
}

/* ===== 知识块头部 ===== */
.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid rgba($accent-secondary,0.06);
  background: rgba($accent-primary,0.02);
}
.chunk-header-title { font-size: 0.85rem; font-weight: 600; color: $text-primary; }
.chunk-count-badge {
  font-size: 0.7rem;
  padding: 2px 10px;
  background: rgba($accent-primary,0.1);
  color: $accent-primary;
  border-radius: 10px;
  font-weight: 600;
}

/* ===== 详情弹窗 ===== */
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease; }
.detail-panel { width: 580px; max-width: 92vw; max-height: 82vh; background: rgba(15,15,35,0.97); border: 1px solid rgba($accent-secondary,0.12); border-radius: 16px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.6); animation: modalEnter 0.3s ease; display: flex; flex-direction: column; }
.detail-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; background: rgba($bg-primary,0.8); border-bottom: 1px solid rgba($accent-secondary,0.08); }
.detail-title { display: flex; align-items: center; gap: 10px; font-size: 1rem; font-weight: 600; color: $text-primary; .detail-doc-icon { font-size: 1.2rem; } }
.detail-close { width: 28px; height: 28px; border: none; background: rgba($accent-secondary,0.08); border-radius: 6px; font-size: 0.9rem; cursor: pointer; color: $text-muted; transition: all 0.15s; display: flex; align-items: center; justify-content: center; &:hover { background: rgba($accent-secondary,0.15); color: $text-primary; } }
.detail-body { flex: 1; overflow-y: auto; padding: 20px; scrollbar-width: thin; scrollbar-color: rgba($accent-primary,0.12) transparent; &::-webkit-scrollbar { width: 3px; } &::-webkit-scrollbar-thumb { background: rgba($accent-primary,0.12); border-radius: 2px; } }
.detail-section { margin-bottom: 20px; &:last-child { margin-bottom: 0; } }
.section-title { font-size: 0.85rem; font-weight: 700; color: $accent-primary; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid rgba($accent-primary,0.08); }
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  background: rgba(0,0,0,0.15);
  border-radius: 10px;
  padding: 14px;
}
.info-item { display: flex; flex-direction: column; gap: 3px; label { font-size: 0.7rem; color: $text-muted; } span { font-size: 0.82rem; color: $text-secondary; } .status-badge { font-weight: 600; &.ready { color: $accent-emerald; } &.processing { color: $accent-amber; } &.error { color: $accent-red; } } .doc-id-text { font-family: monospace; font-size: 0.75rem; color: $text-muted; word-break: break-all; } }
.preview-content { font-size: 0.82rem; color: $text-secondary; line-height: 1.7; padding: 12px 14px; background: rgba(0,0,0,0.12); border-radius: 8px; border-left: 3px solid rgba($accent-primary,0.15); max-height: 120px; overflow-y: auto; }
.chunk-preview-list { display: flex; flex-direction: column; gap: 6px; }
.chunk-preview-item { display: flex; align-items: flex-start; gap: 8px; padding: 8px 10px; background: rgba(0,0,0,0.12); border-radius: 6px; .chunk-index-tag { font-size: 0.7rem; font-weight: 700; color: $accent-primary; background: rgba($accent-primary,0.08); padding: 1px 6px; border-radius: 4px; flex-shrink: 0; } .chunk-preview-text { font-size: 0.78rem; color: $text-secondary; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; } }
.more-hint { font-size: 0.75rem; color: $text-muted; text-align: center; padding: 6px; font-style: italic; }
.detail-footer { display: flex; gap: 10px; justify-content: center; padding: 14px 20px; border-top: 1px solid rgba($accent-secondary,0.08); }
.btn-primary { padding: 8px 20px; background: rgba($accent-indigo, 0.1); border: 1px solid rgba($accent-indigo, 0.25); border-radius: 8px; color: $accent-indigo; font-size: 0.82rem; font-weight: 500; cursor: pointer; transition: all 0.3s ease; font-family: inherit; &:hover { background: rgba($accent-indigo, 0.18); border-color: rgba($accent-indigo, 0.4); transform: translateY(-1px); box-shadow: 0 4px 12px rgba($accent-indigo, 0.15); } }
.btn-secondary { padding: 8px 20px; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.12); border-radius: 8px; color: $text-secondary; font-size: 0.82rem; cursor: pointer; transition: all 0.2s; &:hover { background: rgba($accent-secondary,0.1); color: $text-primary; } }
.qa-section { position: relative; z-index: 1; background: rgba($bg-primary,0.6); backdrop-filter: blur(12px); border: 1px solid rgba($accent-secondary,0.1); border-radius: 14px; overflow: hidden; }
.qa-hdr { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid rgba($accent-secondary,0.08); flex-wrap: wrap; gap: 6px; }
.qa-hdr-left { display: flex; align-items: center; gap: 8px; }
.qa-hdr-right { display: flex; align-items: center; gap: 8px; }
.qa-status { display: flex; align-items: center; gap: 6px; font-size: 0.8rem; color: $text-secondary; }
.qa-dot { width: 6px; height: 6px; border-radius: 50%; background: $accent-emerald; animation: pulse 2s infinite; }
.qa-messages { max-height: 280px; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; scrollbar-width: thin; scrollbar-color: rgba($accent-primary,0.12) transparent; &::-webkit-scrollbar { width: 3px; } &::-webkit-scrollbar-thumb { background: rgba($accent-primary,0.12); border-radius: 2px; } }
.qa-welcome { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 20px; .welcome-icon { font-size: 2rem; margin-bottom: 10px; } h3 { font-size: 1.1rem; color: $text-primary; margin-bottom: 4px; } p { font-size: 0.85rem; color: $text-muted; margin-bottom: 14px; } }
.welcome-tips { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.tip-chip { padding: 6px 14px; background: rgba(16,185,129,0.08); border: 1px solid rgba(16,185,129,0.15); border-radius: 16px; font-size: 0.78rem; color: #10b981; cursor: pointer; transition: all 0.2s; &:hover { background: rgba(16,185,129,0.15); border-color: rgba(16,185,129,0.3); } }

/* ===== 消息布局 ===== */
.user-message-wrapper { display: flex; justify-content: flex-end; width: 100%; }
.ai-message-wrapper { display: flex; justify-content: flex-start; width: 100%; }
.message { display: flex; gap: 10px; width: 100%; animation: msgEnter 0.3s ease; &.user { flex-direction: row-reverse; } }
.avatar { width: 34px; height: 34px; background: rgba($bg-primary, 0.6); border: 1px solid rgba($accent-secondary,0.12); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1rem; flex-shrink: 0; backdrop-filter: blur(8px); }
.user-av { background: linear-gradient(135deg, rgba($accent-primary,0.15), rgba(123,97,255,0.12)); border-color: rgba($accent-primary,0.15); }
.ai-av { background: rgba($accent-primary,0.1); }
.msg-content { display: flex; flex-direction: column; max-width: 85%; min-width: 0; }
.msg-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ai-label { font-size: 12px; font-weight: 700; color: #10b981; }
.msg-time { font-size: 11px; color: $text-muted; opacity: 0.7; }
.msg-actions { display: flex; gap: 4px; margin-top: 8px; opacity: 0; transition: opacity 0.2s; .message:hover & { opacity: 1; } }
.act-btn { width: 26px; height: 26px; border: 1px solid rgba(16,185,129,0.12); border-radius: 6px; background: rgba(16,185,129,0.03); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 0.75rem; transition: all 0.2s; &:hover { border-color: #10b981; background: rgba(16,185,129,0.1); transform: translateY(-1px); } &.liked { border-color: #10b981; background: rgba(16,185,129,0.1); } }
.msg-bubble { padding: 10px 14px; border-radius: 12px; font-size: 0.85rem; line-height: 1.6; max-width: 88%; }
.user-bubble { background: linear-gradient(135deg, rgba(16,185,129,0.12), rgba(6,182,212,0.08)); border: 1px solid rgba(16,185,129,0.2); color: $text-primary; margin-left: auto; border-bottom-right-radius: 4px; }
.ai-bubble { background: rgba(15,23,42,0.7); border: 1px solid rgba(16,185,129,0.12); border-left: 3px solid rgba(16,185,129,0.5); color: $text-primary; border-bottom-left-radius: 4px; }
.sources-box { margin-top: 8px; padding: 8px 10px; background: rgba($accent-primary,0.03); border: 1px solid rgba($accent-primary,0.06); border-radius: 8px; }
.sources-hdr { font-size: 0.75rem; color: $accent-primary; font-weight: 500; margin-bottom: 4px; }
.source-card { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; color: $text-secondary; padding: 2px 0; .src-tree { color: $text-muted; font-family: monospace; } .src-title { font-weight: 500; } .src-page { color: $text-muted; font-size: 0.7rem; } .src-rel { color: $accent-emerald; font-weight: 600; margin-left: auto; } }
.typing-row { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: rgba($bg-primary,0.6); border-radius: 12px; font-size: 0.8rem; color: $text-muted; }
.typing-dots { display: flex; gap: 4px; span { width: 5px; height: 5px; border-radius: 50%; background: $accent-primary; animation: typingDot 1.4s infinite; &:nth-child(2) { animation-delay: 0.2s; } &:nth-child(3) { animation-delay: 0.4s; } } }
@keyframes typingDot { 0%,60%,100%{transform:translateY(0);opacity:0.4} 30%{transform:translateY(-5px);opacity:1} }
.qa-input-row { display: flex; gap: 8px; padding: 10px 16px; border-top: 1px solid rgba($accent-secondary,0.08); }
.qa-input { flex: 1; padding: 9px 14px; background: rgba(15,23,42,0.6); border: 1px solid rgba(16,185,129,0.15); border-radius: 10px; color: $text-primary; font-size: 0.85rem; outline: none; caret-color: #10b981; transition: border-color 0.2s, box-shadow 0.2s; &:focus { border-color: rgba(16,185,129,0.4); box-shadow: 0 0 0 3px rgba(16,185,129,0.1); } &:disabled { opacity: 0.5; } &::placeholder { color: $text-muted; } }
.qa-send { padding: 9px 20px; background: linear-gradient(135deg, #10b981, #06b6d4); color: #fff; border: none; border-radius: 10px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; min-width: 64px; &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(16,185,129,0.4); } &:disabled { opacity: 0.6; cursor: not-allowed; } }
.send-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }

/* ===== Markdown 渲染样式 ===== */
.markdown-body {
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    color: $text-primary; font-weight: 700; margin: 12px 0 6px; line-height: 1.4;
  }
  :deep(h1) { font-size: 1.3rem; }
  :deep(h2) { font-size: 1.15rem; color: $accent-primary; }
  :deep(h3) { font-size: 1.05rem; color: #7dd3fc; }
  :deep(p) { margin: 6px 0; line-height: 1.75; color: #d0d0f0; }
  :deep(ul), :deep(ol) { padding-left: 20px; margin: 6px 0; }
  :deep(li) { margin: 3px 0; line-height: 1.7; color: #d0d0f0; }
  :deep(strong) { color: $accent-primary; font-weight: 700; }
  :deep(em) { color: #a78bfa; font-style: italic; }
  :deep(code) { background: rgba($accent-primary,0.08); border: 1px solid rgba($accent-primary,0.12); border-radius: 4px; padding: 1px 5px; font-size: 0.88em; color: #7dd3fc; font-family: 'Fira Code', 'Consolas', monospace; }
  :deep(pre) { background: rgba(0,0,0,0.3); border: 1px solid rgba($accent-secondary,0.12); border-radius: 8px; padding: 12px 14px; margin: 8px 0; overflow-x: auto; code { background: transparent; border: none; padding: 0; color: inherit; } }
  :deep(blockquote) { border-left: 3px solid rgba($accent-primary,0.3); margin: 8px 0; padding: 6px 12px; background: rgba($accent-primary,0.03); border-radius: 0 6px 6px 0; color: $text-secondary; }
  :deep(a) { color: #7dd3fc; text-decoration: none; &:hover { text-decoration: underline; } }
  :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; th, td { border: 1px solid rgba($accent-secondary,0.15); padding: 6px 10px; text-align: left; } th { background: rgba($accent-primary,0.05); color: $accent-primary; } }
}

/* ===== 工作流 Tab 切换 ===== */
.tab-switch {
  display: flex;
  gap: 4px;
  padding: 3px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 8px;
}

.tab-btn {
  padding: 6px 16px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: $text-muted;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: $text-primary;
  }

  &.active {
    background: rgba($accent-primary, 0.2);
    color: $accent-primary;
  }
}

/* ===== 工作流容器 ===== */
.workflow-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 400px;
  overflow: hidden;

  > :deep(.workflow-selector) {
    flex-shrink: 0;
  }

  > :deep(.workflow-executor) {
    flex: 1;
    overflow-y: auto;
  }
}

.qa-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 400px;
  overflow: hidden;

  .qa-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }
}


/* ===== 动画 ===== */
@keyframes fadeSlideIn { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@keyframes statEnter { from { opacity: 0; transform: translateY(12px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes msgEnter { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ===== 对话历史 ===== */
.history-overlay { position: fixed; inset: 0; z-index: 1001; background: rgba(0,0,0,0.3); }
.history-panel { position: fixed; left: 0; top: 0; bottom: 0; width: 260px; background: rgba(10,14,26,0.98); backdrop-filter: blur(12px); border-right: 1px solid rgba($accent-secondary,0.08); display: flex; flex-direction: column; z-index: 1002; }
.history-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; font-size: 14px; font-weight: 600; color: $text-primary; border-bottom: 1px solid rgba($accent-secondary,0.06); }
.history-new { margin: 10px 12px; padding: 8px 14px; background: linear-gradient(135deg, rgba($accent-primary,0.1), rgba(0,85,255,0.06)); border: 1px solid rgba($accent-primary,0.15); border-radius: 6px; color: $accent-primary; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; &:hover { background: linear-gradient(135deg, rgba($accent-primary,0.18), rgba(0,85,255,0.1)); transform: translateY(-1px); } }
.history-list { flex: 1; overflow-y: auto; padding: 6px 10px; }
.hist-item { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 6px; cursor: pointer; transition: all 0.15s; &:hover { background: rgba($accent-secondary,0.06); } &.active { background: rgba($accent-primary,0.07); border-left: 2px solid $accent-primary; } }
.hist-title { flex: 1; font-size: 13px; color: $text-secondary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hist-del { width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; color: $text-muted; cursor: pointer; border-radius: 3px; opacity: 0; transition: opacity 0.15s; .hist-item:hover & { opacity: 1; } &:hover { color: #ff4060; } }
.history-slide-enter-active, .history-slide-leave-active { transition: opacity 0.2s; }
.history-slide-enter-active .history-panel, .history-slide-leave-active .history-panel { transition: transform 0.25s cubic-bezier(0.4,0,0.2,1); }
.history-slide-enter-from, .history-slide-leave-to { opacity: 0; }
.history-slide-enter-from .history-panel, .history-slide-leave-to .history-panel { transform: translateX(-100%); }

/* ===== 图标按钮 ===== */
.icon-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: rgba($bg-primary,0.6); border: 1px solid rgba($accent-secondary,0.12); border-radius: 8px; color: $text-secondary; cursor: pointer; transition: all 0.2s; &:hover { border-color: rgba($accent-primary,0.25); color: $accent-primary; } &.accent { background: linear-gradient(135deg, rgba($accent-primary,0.15), rgba(0,85,255,0.1)); border-color: rgba($accent-primary,0.2); color: $accent-primary; &:hover { background: linear-gradient(135deg, rgba($accent-primary,0.25), rgba(0,85,255,0.15)); transform: translateY(-1px); box-shadow: 0 3px 12px rgba($accent-primary,0.15); } } }
.icon-btn-sm { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; color: $text-muted; cursor: pointer; border-radius: 4px; transition: all 0.15s; &:hover { background: rgba($accent-secondary,0.1); color: $text-primary; } }

/* ===== TransitionGroup ===== */
.doc-list-enter-active { transition: all 0.3s ease; }
.doc-list-leave-active { transition: all 0.3s ease; }
.doc-list-enter-from { opacity: 0; transform: translateX(-16px); }
.doc-list-leave-to { opacity: 0; transform: translateX(16px); }
.doc-list-move { transition: transform 0.3s ease; }

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 38% 62%; }
}
@media (max-width: 1024px) {
  .content-grid { grid-template-columns: 1fr; height: auto; }
  .panel-left { max-height: 380px; }
}
@media (max-width: 640px) {
  .knowledge-page { padding: 20px 16px 40px; }
  .page-title { font-size: 1.5rem; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 10px; }
  .stat-card { padding: 10px 12px; }
  .stat-num { font-size: 1.1rem; }
  .content-grid { gap: 12px; }
  .chunks-grid { grid-template-columns: 1fr; }
  .qa-messages { max-height: 200px; }
}

/* ===== 知识块列表弹窗 ===== */
.chunks-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

.chunks-dialog {
  width: 90%;
  max-width: 700px;
  max-height: 80vh;
  background: rgba($bg-primary, 0.95);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

.cd-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba($accent-secondary, 0.1);
  background: rgba($accent-primary, 0.04);
}

.cd-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cd-icon {
  font-size: 1.8rem;
}

.cd-header-info h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: $text-primary;
}

.cd-header-info p {
  margin: 4px 0 0;
  font-size: 0.8rem;
  color: $text-muted;
}

.cd-close {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba($accent-secondary, 0.1);
  border-radius: 8px;
  color: $text-muted;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(239, 68, 68, 0.2);
    color: $accent-red;
  }
}

.cd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.cd-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-secondary, 0.08);
  margin-bottom: 16px;
  font-size: 0.85rem;
}

.cd-summary-label {
  color: $text-muted;
}

.cd-summary-value {
  color: $text-primary;
  font-weight: 500;
}

.cd-summary-count {
  color: $accent-primary;
  font-weight: 600;
}

.cd-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: $text-muted;
}

.cd-chunks-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cd-chunk-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  background: rgba($accent-secondary, 0.04);
  border-radius: 10px;
  border: 1px solid rgba($accent-secondary, 0.06);
  transition: all 0.2s;
  cursor: pointer;
  &:hover {
    border-color: rgba($accent-primary, 0.15);
    background: rgba($accent-primary, 0.04);
  }
}

.cd-chunk-index {
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 700;
  color: $accent-primary;
  background: rgba($accent-primary, 0.12);
  padding: 3px 8px;
  border-radius: 6px;
  min-width: 32px;
  text-align: center;
}

.cd-chunk-content {
  flex: 1;
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
  word-break: break-word;
}

.cd-empty {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 0.9rem;
}

.cd-footer {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid rgba($accent-secondary, 0.1);
  background: rgba($bg-primary, 0.6);
}

.cd-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.cd-btn-cancel {
  background: rgba($accent-secondary, 0.15);
  color: $text-primary;
  &:hover {
    background: rgba($accent-secondary, 0.25);
  }
}

/* ===== 知识块详情弹窗 ===== */
.chunk-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  animation: fadeIn 0.2s ease;
}

.chunk-detail-dialog {
  width: 90%;
  max-width: 650px;
  max-height: 80vh;
  background: rgba($bg-primary, 0.95);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

.cdd-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba($accent-secondary, 0.1);
  background: rgba($accent-primary, 0.04);
}

.cdd-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cdd-icon {
  font-size: 1.8rem;
}

.cdd-header-info h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: $text-primary;
}

.cdd-header-info p {
  margin: 4px 0 0;
  font-size: 0.8rem;
  color: $text-muted;
}

.cdd-close {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba($accent-secondary, 0.1);
  border-radius: 8px;
  color: $text-muted;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(239, 68, 68, 0.2);
    color: $accent-red;
  }
}

.cdd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.cdd-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-secondary, 0.08);
}

.cdd-meta-item {
  font-size: 0.8rem;
  color: $text-muted;
}

.cdd-content {
  font-size: 0.9rem;
  color: #d0d0f0;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 400px;
  overflow-y: auto;
  padding: 12px 14px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  border: 1px solid rgba($accent-secondary, 0.08);
}

.cdd-footer {
  display: flex;
  justify-content: center;
  padding: 14px 20px;
  border-top: 1px solid rgba($accent-secondary, 0.1);
  background: rgba($bg-primary, 0.6);
}

.cdd-btn {
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba($accent-primary, 0.2);
  background: rgba($accent-primary, 0.08);
  color: $accent-primary;
  &:hover {
    background: rgba($accent-primary, 0.15);
    transform: translateY(-1px);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 相关知识图谱 ===== */
.related-knowledge-graph {
  margin-top: 16px;
  padding: 16px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(16, 185, 129, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(8px);
}

.graph-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(16, 185, 129, 0.1);
}

.graph-icon {
  font-size: 1rem;
}

.graph-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #10b981;
}

.graph-content {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
}

.graph-center {
  flex-shrink: 0;
}

.center-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(6, 182, 212, 0.1));
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 10px;
  max-width: 140px;
}

.center-icon {
  font-size: 1.2rem;
}

.center-text {
  font-size: 0.8rem;
  font-weight: 600;
  color: #10b981;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-connections {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  padding-left: 20px;
}

.connection-line {
  position: absolute;
  left: 0;
  top: 50%;
  width: 20px;
  height: 2px;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.4), rgba(16, 185, 129, 0.1));
  transform: translateY(-50%);
}

.related-node {
  flex: 1;
  min-width: 0;
  animation: nodeSlideIn 0.3s ease forwards;
  animation-delay: var(--delay);
  opacity: 0;
  cursor: pointer;
}

.node-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 12px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(16, 185, 129, 0.12);
  border-radius: 10px;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(16, 185, 129, 0.3);
    background: rgba(16, 185, 129, 0.08);
    transform: translateY(-2px);
  }
}

.node-icon {
  font-size: 1.2rem;
}

.node-title {
  font-size: 0.75rem;
  font-weight: 500;
  color: $text-primary;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.node-meta {
  font-size: 0.65rem;
  color: #10b981;
}

@keyframes nodeSlideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>