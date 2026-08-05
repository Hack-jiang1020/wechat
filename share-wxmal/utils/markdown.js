/**
 * 轻量 Markdown -> HTML 转换器（供小程序 rich-text 渲染）
 * 支持标题、粗体、斜体、行内代码、代码块、列表、引用、链接、图片、分割线
 */

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function inline(text) {
  let html = escapeHtml(text);
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  // 图片
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1"/>');
  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  // 加粗
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  // 斜体
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  return html;
}

function md2html(md) {
  if (!md) return '';
  let text = String(md).replace(/\r\n/g, '\n');
  const blocks = [];

  // 代码块
  text = text.replace(/```(\w*)\n([\s\S]*?)```/g, (match, lang, code) => {
    blocks.push('<pre><code class="language-' + (lang || '') + '">' + escapeHtml(code) + '</code></pre>');
    return '\u0000CODE' + (blocks.length - 1) + '\u0000';
  });

  const lines = text.split('\n');
  const result = [];
  let listType = null;
  let inParagraph = [];

  function flushParagraph() {
    if (inParagraph.length) {
      result.push('<p>' + inline(inParagraph.join(' ')) + '</p>');
      inParagraph = [];
    }
  }

  function flushList() {
    if (listType) {
      result.push('</' + listType + '>');
      listType = null;
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    if (/^\u0000CODE\d+\u0000$/.test(trimmed)) {
      flushParagraph();
      flushList();
      const idx = parseInt(trimmed.replace(/\D/g, ''), 10);
      result.push(blocks[idx]);
      continue;
    }

    if (!trimmed) {
      flushParagraph();
      flushList();
      continue;
    }

    if (/^#{1,6}\s/.test(trimmed)) {
      flushParagraph();
      flushList();
      const level = trimmed.match(/^#+/)[0].length;
      const title = inline(trimmed.replace(/^#+\s*/, ''));
      result.push('<h' + level + '>' + title + '</h' + level + '>');
      continue;
    }

    if (/^---+$/.test(trimmed)) {
      flushParagraph();
      flushList();
      result.push('<hr/>');
      continue;
    }

    if (/^>\s?/.test(trimmed)) {
      flushParagraph();
      flushList();
      result.push('<blockquote>' + inline(trimmed.replace(/^>\s?/, '')) + '</blockquote>');
      continue;
    }

    if (/^[-*+]\s/.test(trimmed)) {
      flushParagraph();
      if (listType !== 'ul') {
        flushList();
        result.push('<ul>');
        listType = 'ul';
      }
      result.push('<li>' + inline(trimmed.replace(/^[-*+]\s/, '')) + '</li>');
      continue;
    }

    if (/^\d+[.、]\s/.test(trimmed)) {
      flushParagraph();
      if (listType !== 'ol') {
        flushList();
        result.push('<ol>');
        listType = 'ol';
      }
      result.push('<li>' + inline(trimmed.replace(/^\d+[.、]\s/, '')) + '</li>');
      continue;
    }

    inParagraph.push(trimmed);
  }

  flushParagraph();
  flushList();
  return result.join('');
}

module.exports = {
  md2html: md2html
};
