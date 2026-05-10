/**
 * 文件预览工具函数
 *
 * 定义可预览文件类型白名单，以及按 contentType 判断预览渲染策略。
 * 不可预览的类型应隐藏预览按钮，只保留下载。
 */

/** 预览渲染策略枚举 */
export type PreviewStrategy =
  | 'iframe'      // PDF、图片、纯文本 — 直接用 iframe/img 加载对象存储 URL
  | 'docx'        // DOCX — fetch blob 后用 docx-preview 渲染
  | 'markdown'    // Markdown — fetch 文本后用 react-markdown 渲染
  | 'html'        // HTML — fetch 文本后用 sandbox iframe srcdoc 渲染
  | 'unsupported'; // 不支持预览

/**
 * 根据 contentType 和文件名判断预览策略。
 * 优先按 contentType 匹配，文件名扩展名作为兜底。
 */
export function getPreviewStrategy(contentType?: string | null, filename?: string | null): PreviewStrategy {
  const ct = (contentType ?? '').toLowerCase();
  const fn = (filename ?? '').toLowerCase();

  // PDF — 浏览器原生支持
  if (ct === 'application/pdf' || fn.endsWith('.pdf')) {
    return 'iframe';
  }

  // 图片 — 浏览器原生支持
  if (ct.startsWith('image/') && !ct.includes('svg')) {
    return 'iframe';
  }

  // DOCX（新格式 Word）— docx-preview 库渲染
  if (
    ct.includes('application/vnd.openxmlformats-officedocument.wordprocessingml') ||
    fn.endsWith('.docx')
  ) {
    return 'docx';
  }

  // Markdown — react-markdown 渲染
  if (
    ct === 'text/markdown' ||
    ct === 'text/x-markdown' ||
    fn.endsWith('.md') ||
    fn.endsWith('.markdown') ||
    fn.endsWith('.mdown')
  ) {
    return 'markdown';
  }

  // HTML — sandbox iframe srcdoc 渲染（安全隔离）
  if (
    ct === 'text/html' ||
    ct === 'application/xhtml+xml' ||
    fn.endsWith('.html') ||
    fn.endsWith('.htm')
  ) {
    return 'html';
  }

  // 纯文本 — pre 标签或 iframe 展示
  if (ct === 'text/plain' || fn.endsWith('.txt')) {
    return 'iframe';
  }

  // 其他格式不支持预览
  return 'unsupported';
}

/**
 * 判断文件是否支持预览（用于控制预览按钮可见性）
 */
export function isPreviewable(contentType?: string | null, filename?: string | null): boolean {
  return getPreviewStrategy(contentType, filename) !== 'unsupported';
}
