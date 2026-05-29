<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const html = computed(() => {
  if (!props.content) return ''
  return md.render(props.content)
})
</script>

<template>
  <div class="md-body" v-html="html" />
</template>

<style scoped>
.md-body {
  font-size: 15px;
  line-height: 1.75;
  color: var(--ds-text);
  word-break: break-word;
}

.md-body :deep(p) {
  margin: 0 0 10px;
}

.md-body :deep(p:last-child) {
  margin-bottom: 0;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ds-text);
}

.md-body :deep(h1) { font-size: 1.25em; }
.md-body :deep(h2) { font-size: 1.12em; }
.md-body :deep(h3) { font-size: 1.05em; }

.md-body :deep(ul),
.md-body :deep(ol) {
  margin: 8px 0 12px;
  padding-left: 1.4em;
}

.md-body :deep(li) {
  margin: 4px 0;
}

.md-body :deep(strong) {
  font-weight: 600;
  color: var(--ds-text);
}

.md-body :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: var(--ds-input-bg);
  font-size: 0.9em;
  font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
}

.md-body :deep(pre) {
  margin: 10px 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--ds-input-bg);
  overflow-x: auto;
}

.md-body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.md-body :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 3px solid var(--ds-accent);
  background: #f8f9ff;
  color: var(--ds-text-secondary);
}

.md-body :deep(a) {
  color: var(--ds-accent);
  text-decoration: none;
}

.md-body :deep(a:hover) {
  text-decoration: underline;
}

.md-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 14px;
}

.md-body :deep(th),
.md-body :deep(td) {
  border: 1px solid var(--ds-border);
  padding: 8px 10px;
  text-align: left;
}

.md-body :deep(th) {
  background: var(--ds-input-bg);
  font-weight: 600;
}
</style>
