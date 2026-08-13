// ESLint 配置（CommonJS 格式，兼容 "type": "module"）
// 前端代码规范：仅 JavaScript（.js），禁止 TypeScript
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    // ===== 格式类规则（遵循项目既有风格，不做强制） =====
    'indent': 'off',
    'semi': 'off',
    'quotes': 'off',
    'comma-dangle': 'off',
    'no-multiple-empty-lines': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-self-closing': 'off',
    'vue/html-indent': 'off',
    'vue/multi-word-component-names': 'off',

    // ===== 质量类规则（保留实质检查） =====
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    'no-console': 'off', // 项目调试阶段允许 console
    // v-html 已统一经 utils/markdown.js 中的 DOMPurify 净化（JsonResult 等本地转义后高亮），关闭静态提示
    'vue/no-v-html': 'off',
    'no-empty': ['error', { allowEmptyCatch: true }], // 允许 catch {} 有意忽略（localStorage 容错）
    'vue/no-unused-components': 'warn',
  },
  ignorePatterns: ['dist/', 'node_modules/', 'public/'],
}
