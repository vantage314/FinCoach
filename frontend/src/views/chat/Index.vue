<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <div class="new-chat-btn" @click="clearChat">
        <el-icon><Plus /></el-icon> 新对话
      </div>
      
      <div class="history-list">
        <div class="history-label">快捷指令</div>
        <div class="history-item" @click="usePrompt('请帮我分析一下 宁德时代 (300750) 的近期走势')">
          <el-icon><TrendCharts /></el-icon> 个股分析
        </div>
        <div class="history-item" @click="usePrompt('最近市场上有什么关于半导体的大新闻？')">
          <el-icon><Reading /></el-icon> 市场热点
        </div>
        <div class="history-item" @click="usePrompt('根据我的持仓，给一些投资建议')">
          <el-icon><Wallet /></el-icon> 持仓诊断
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <span class="title">FinCoach AI 智能投顾</span>
        <span class="model-tag">Model: GPT-4o-Mini (Powered by API)</span>
      </div>

      <div class="messages-area" ref="scrollRef">
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="logo-icon">🤖</div>
          <h2>我是您的智能理财助手</h2>
          <p>我可以帮您分析个股、解读新闻、提供资产配置建议。</p>
        </div>

        <div v-else v-for="(msg, index) in messages" :key="index" :class="['message-row', msg.role]">
          <div class="avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="bubble">
            <div v-if="msg.loading" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            <div v-else-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)" class="markdown-body"></div>
            <div v-else>{{ msg.content }}</div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputContent"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="问点什么吧，例如：宁德时代现在值得买吗？"
            @keydown.enter.prevent="sendMessage"
            resize="none"
            class="chat-input"
          />
          <el-button type="primary" :loading="loading" @click="sendMessage" circle class="send-btn">
            <el-icon><Position /></el-icon>
          </el-button>
        </div>
        <div class="footer-tip">AI 生成内容仅供参考，不构成投资建议。</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue';
import MarkdownIt from 'markdown-it';
import { sendChatRequest } from '@/api/ai';

// 🔥 引入 Element Plus 图标
import { 
  Plus, 
  TrendCharts, 
  Reading, 
  Wallet, 
  Position 
} from '@element-plus/icons-vue';

const md = new MarkdownIt();
const inputContent = ref('');
const loading = ref(false);
const scrollRef = ref<HTMLElement | null>(null);

interface Message {
  role: 'user' | 'assistant';
  content: string;
  loading?: boolean;
}

const messages = ref<Message[]>([
  { role: 'assistant', content: '你好！我是 FinCoach AI。虽然我只是一个演示模型，但我能读取你本地数据库里的行情和新闻。请问有什么可以帮您？' }
]);

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
    }
  });
};

const renderMarkdown = (text: string) => {
  return md.render(text);
};

const clearChat = () => {
  messages.value = [];
};

const usePrompt = (text: string) => {
  inputContent.value = text;
};

const sendMessage = async () => {
  const content = inputContent.value.trim();
  if (!content) return;
  if (loading.value) return;

  // 1. 添加用户消息
  messages.value.push({ role: 'user', content });
  inputContent.value = '';
  loading.value = true;
  scrollToBottom();

  // 2. 添加 AI 占位符
  const aiMsgIndex = messages.value.push({ role: 'assistant', content: '', loading: true }) - 1;
  scrollToBottom();

  try {
    // 3. 调用后端 API
    const res: any = await sendChatRequest({ message: content });
    
    // 4. 更新 UI
    if (res.code === 200) {
      messages.value[aiMsgIndex].loading = false;
      messages.value[aiMsgIndex].content = res.data; // 后端返回的 Markdown 文本
    } else {
      messages.value[aiMsgIndex].loading = false;
      messages.value[aiMsgIndex].content = '❌ ' + (res.message || '服务暂时不可用');
    }
  } catch (error) {
    messages.value[aiMsgIndex].loading = false;
    messages.value[aiMsgIndex].content = '❌ 网络请求失败，请检查后端服务。';
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};
</script>

<style scoped>
.chat-container { display: flex; height: calc(100vh - 80px); background: #14161a; color: #fff; overflow: hidden; }

/* 侧边栏 */
.chat-sidebar { width: 260px; background: #1d212b; border-right: 1px solid #2c3038; display: flex; flex-direction: column; padding: 15px; }
.new-chat-btn { border: 1px solid #4c4d4f; border-radius: 6px; padding: 10px; text-align: center; cursor: pointer; transition: 0.3s; margin-bottom: 20px; display: flex; align-items: center; justify-content: center; gap: 8px; }
.new-chat-btn:hover { background: #2b303c; border-color: #409eff; color: #409eff; }
.history-label { font-size: 12px; color: #909399; margin-bottom: 10px; padding-left: 5px; }
.history-item { padding: 10px; border-radius: 6px; cursor: pointer; color: #dcdfe6; display: flex; align-items: center; gap: 10px; font-size: 14px; }
.history-item:hover { background: #2b303c; }

/* 主区域 */
.chat-main { flex: 1; display: flex; flex-direction: column; background: #14161a; position: relative; }
.chat-header { height: 60px; border-bottom: 1px solid #2c3038; display: flex; flex-direction: column; justify-content: center; align-items: center; background: #1d212b; }
.chat-header .title { font-weight: bold; font-size: 16px; }
.chat-header .model-tag { font-size: 12px; color: #67c23a; margin-top: 2px; }

/* 消息区 */
.messages-area { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 20px; }
.welcome-screen { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #909399; opacity: 0.8; }
.logo-icon { font-size: 60px; margin-bottom: 20px; }

.message-row { display: flex; gap: 15px; max-width: 800px; margin: 0 auto; width: 100%; }
.message-row.user { flex-direction: row-reverse; }
.message-row .avatar { width: 36px; height: 36px; border-radius: 50%; background: #2b303c; display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid #4c4d4f; }
.message-row.user .avatar { background: #409eff; border: none; }
.message-row .bubble { background: #2b303c; padding: 12px 16px; border-radius: 12px; line-height: 1.6; position: relative; min-height: 40px; }
.message-row.user .bubble { background: #409eff; color: #fff; border-radius: 12px 0 12px 12px; }
.message-row.assistant .bubble { background: #1d212b; border: 1px solid #2c3038; border-radius: 0 12px 12px 12px; }

/* 正在输入动画 */
.typing-indicator span { display: inline-block; width: 6px; height: 6px; background: #909399; border-radius: 50%; margin: 0 2px; animation: bounce 1.4s infinite ease-in-out both; }
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* 输入区 */
.input-area { padding: 20px; background: #1d212b; border-top: 1px solid #2c3038; display: flex; flex-direction: column; align-items: center; }
.input-wrapper { width: 100%; max-width: 800px; position: relative; background: #2b303c; border-radius: 12px; border: 1px solid #4c4d4f; padding: 5px; display: flex; align-items: flex-end; }
.chat-input :deep(.el-textarea__inner) { background: transparent; border: none; box-shadow: none; color: #fff; padding: 10px; max-height: 150px; }
.send-btn { margin: 0 5px 5px 0; }
.footer-tip { font-size: 12px; color: #606266; margin-top: 10px; }

/* Markdown 样式适配 */
.markdown-body :deep(p) { margin-bottom: 10px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { color: #409eff; }
.markdown-body :deep(ul) { padding-left: 20px; margin-bottom: 10px; }
.markdown-body :deep(li) { margin-bottom: 5px; }
.markdown-body :deep(code) { background: #14161a; padding: 2px 4px; border-radius: 4px; color: #e6a23c; font-family: monospace; }
</style>
