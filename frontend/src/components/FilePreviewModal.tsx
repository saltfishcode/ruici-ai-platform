import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Loader2, AlertTriangle } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { renderAsync } from 'docx-preview';
import { getPreviewStrategy, type PreviewStrategy } from '../utils/previewUtils';
import { documentApi } from '../api/document';

interface FilePreviewModalProps {
  open: boolean;
  onClose: () => void;
  resumeId: number;
  contentType?: string | null;
  filename?: string | null;
}

export default function FilePreviewModal({ open, onClose, resumeId, contentType, filename }: FilePreviewModalProps) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [textContent, setTextContent] = useState<string>('');
  const [blobUrl, setBlobUrl] = useState<string>('');
  const docxContainerRef = useRef<HTMLDivElement>(null);
  const strategy = getPreviewStrategy(contentType, filename);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    if (open) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, handleKeyDown]);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    setError(null);
    setTextContent('');
    if (blobUrl) {
      URL.revokeObjectURL(blobUrl);
      setBlobUrl('');
    }

    documentApi.getOriginalFile(resumeId, 'inline')
      .then(async (blob) => {
        if (strategy === 'markdown' || strategy === 'html') {
          const text = await blob.text();
          setTextContent(text);
          setLoading(false);
        } else if (strategy === 'docx') {
          const buffer = await blob.arrayBuffer();
          if (docxContainerRef.current) {
            docxContainerRef.current.innerHTML = '';
            await renderAsync(buffer, docxContainerRef.current, undefined, {
              className: 'docx-preview-wrapper',
              inWrapper: true,
              ignoreWidth: false,
              ignoreHeight: true,
              ignoreFonts: false,
              breakPages: true,
            });
          }
          setLoading(false);
        } else if (strategy === 'iframe') {
          const url = URL.createObjectURL(blob);
          setBlobUrl(url);
          setLoading(false);
        }
      })
      .catch(err => {
        setError(`加载失败: ${err.message || '未知错误'}`);
        setLoading(false);
      });

    return () => {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, resumeId, strategy]);

  if (!open) return null;

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

          <motion.div
            className="relative w-[90vw] h-[85vh] max-w-6xl bg-white dark:bg-[#1a1f2e] rounded-2xl shadow-2xl flex flex-col overflow-hidden"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          >
            <div className="flex items-center justify-between px-6 py-4 border-b border-stone-200 dark:border-[#2d3548] shrink-0">
              <h3 className="text-lg font-semibold text-primary-800 dark:text-[#f3f4f6] truncate max-w-[70%]">
                {filename || '文件预览'}
              </h3>
              <button
                type="button"
                onClick={onClose}
                className="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-stone-100 dark:hover:bg-[#1f2937] transition-colors"
              >
                <X className="w-5 h-5 text-primary-400" />
              </button>
            </div>

            <div className="flex-1 overflow-auto p-4">
              {loading && (
                <div className="flex items-center justify-center h-full">
                  <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
                </div>
              )}

              {error && (
                <div className="flex flex-col items-center justify-center h-full gap-3">
                  <AlertTriangle className="w-10 h-10 text-amber-500" />
                  <p className="text-primary-500 dark:text-[#9ca3af]">{error}</p>
                </div>
              )}

              {!loading && !error && renderContent(strategy, blobUrl, textContent, docxContainerRef)}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function renderContent(
  strategy: PreviewStrategy,
  blobUrl: string,
  textContent: string,
  docxContainerRef: React.RefObject<HTMLDivElement>,
) {
  switch (strategy) {
    case 'iframe':
      return (
        <iframe
          src={blobUrl}
          title="文件预览"
          className="w-full h-full border-0 rounded-lg"
        />
      );

    case 'docx':
      return (
        <div
          ref={docxContainerRef}
          className="w-full min-h-[60vh] bg-white rounded-lg overflow-auto"
        />
      );

    case 'markdown':
      return (
        <div className="prose prose-neutral dark:prose-invert max-w-none p-6 bg-white dark:bg-[#1f2937] rounded-lg overflow-auto h-full">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {textContent}
          </ReactMarkdown>
        </div>
      );

    case 'html':
      return (
        <iframe
          srcDoc={textContent}
          title="HTML 预览"
          className="w-full h-full border-0 rounded-lg bg-white"
          sandbox="allow-same-origin"
        />
      );

    default:
      return (
        <div className="flex items-center justify-center h-full">
          <p className="text-primary-400">不支持预览此文件格式</p>
        </div>
      );
  }
}
