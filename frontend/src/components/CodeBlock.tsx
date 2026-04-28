import { useState, lazy, Suspense } from 'react';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Check, Copy } from 'lucide-react';

// Lazy load SyntaxHighlighter
const SyntaxHighlighter = lazy(() =>
  import('react-syntax-highlighter/dist/esm/prism').then(module => ({ default: module.default }))
);

interface CodeBlockProps {
  language?: string;
  children: string;
}

export default function CodeBlock({ language, children }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(children);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('复制失败:', err);
    }
  };

  const code = children?.trim() || '';

  return (
    <div className="relative group my-6 rounded-2xl overflow-hidden border border-slate-200/50 dark:border-slate-800/50 shadow-medium">
      {/* 顶部工具栏 */}
      <div className="flex items-center justify-between px-5 py-2.5 bg-slate-50 dark:bg-slate-900 border-b border-slate-200/50 dark:border-slate-800/50">
        <div className="flex items-center gap-2">
          <div className="flex gap-1.5 mr-2">
            <div className="w-2.5 h-2.5 rounded-full bg-red-400/20 dark:bg-red-400/10 border border-red-400/30" />
            <div className="w-2.5 h-2.5 rounded-full bg-amber-400/20 dark:bg-amber-400/10 border border-amber-400/30" />
            <div className="w-2.5 h-2.5 rounded-full bg-emerald-400/20 dark:bg-emerald-400/10 border border-emerald-400/30" />
          </div>
          <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest font-mono">
            {language || 'plaintext'}
          </span>
        </div>
        
        <button
          onClick={handleCopy}
          className="flex items-center gap-2 px-3 py-1.5 text-[10px] font-bold text-slate-500 dark:text-slate-400 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-white dark:hover:bg-slate-800 rounded-lg transition-all duration-200 border border-transparent hover:border-slate-200 dark:hover:border-slate-700 active:scale-95"
          title="复制代码"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-emerald-500" />
              <span className="text-emerald-500">COPIED</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>COPY</span>
            </>
          )}
        </button>
      </div>

      {/* 代码内容区 */}
      <div className="bg-[#0d1117] text-sm leading-relaxed relative">
        <Suspense fallback={
          <div className="p-8 text-slate-500 font-mono text-xs animate-pulse flex items-center justify-center gap-3">
            <div className="w-4 h-4 border-2 border-slate-500/20 border-t-slate-500 rounded-full animate-spin" />
            Formatting Code...
          </div>
        }>
          <SyntaxHighlighter
            language={language || 'text'}
            style={oneDark}
            customStyle={{
              margin: 0,
              padding: '1.5rem',
              background: 'transparent',
              fontSize: '0.85rem',
              lineHeight: '1.6',
            }}
            showLineNumbers={code.split('\n').length > 3}
            lineNumberStyle={{
              minWidth: '2.5em',
              paddingRight: '1em',
              color: '#4b5563',
              textAlign: 'right',
              userSelect: 'none',
              opacity: 0.5,
            }}
            wrapLines
          >
            {code}
          </SyntaxHighlighter>
        </Suspense>
      </div>
    </div>
  );
}
