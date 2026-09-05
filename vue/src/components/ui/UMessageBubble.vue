<template>
  <div class="u-msg" :class="[`u-msg--${role}`]">
    <div v-if="role === 'assistant'" class="u-msg-avatar">
      <span class="u-msg-dot"></span>
    </div>
    <div class="u-msg-content">
      <span v-if="role === 'assistant'" class="u-msg-sender">AI</span>
      <div class="u-msg-bubble">
        <slot />
      </div>
      <span v-if="time" class="u-msg-time">{{ time }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  role: { type: String, default: 'user' },     // user / assistant / system
  time: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-msg {
  display: flex;
  gap: $space-2;
  max-width: 80%;
  animation: msg-in 0.2s ease both;

  &--user { margin-left: auto; flex-direction: row-reverse; }
  &--assistant { margin-right: auto; }
  &--system {
    max-width: 100%;
    margin: $space-2 auto;
    justify-content: center;
  }
}

.u-msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.u-msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: white;
}

.u-msg-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.u-msg-sender {
  font-size: $text-xs;
  font-weight: 600;
  color: $accent-indigo;
}

.u-msg-bubble {
  padding: $space-3 $space-4;
  border-radius: $radius-lg;
  font-size: $text-sm;
  line-height: $leading-relaxed;
  word-break: break-word;

  .u-msg--user & {
    background: rgba($accent-indigo, 0.15);
    color: $text-primary;
    border: 1px solid rgba($accent-indigo, 0.2);
    border-bottom-right-radius: $radius-xs;
  }

  .u-msg--assistant & {
    background: $bg-elevated;
    color: $text-primary;
    border: 1px solid $border-subtle;
    border-bottom-left-radius: $radius-xs;
  }

  .u-msg--system & {
    background: rgba($text-muted, 0.08);
    color: $text-muted;
    font-size: $text-xs;
    text-align: center;
    border-radius: $radius-full;
    padding: $space-1 $space-3;
  }
}

.u-msg-time {
  font-size: 11px;
  color: $text-muted;
}

@keyframes msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
