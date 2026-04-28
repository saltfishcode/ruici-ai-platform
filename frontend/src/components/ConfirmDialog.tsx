import {AnimatePresence, motion} from 'framer-motion';
import {Loader2} from 'lucide-react';

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string | React.ReactNode;
  confirmText?: string;
  cancelText?: string;
  confirmVariant?: 'danger' | 'primary' | 'warning';
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  customContent?: React.ReactNode;
  hideButtons?: boolean;
}

export default function ConfirmDialog({
  open,
  title,
  message,
  confirmText = '确定',
  cancelText = '取消',
  confirmVariant = 'primary',
  onConfirm,
  onCancel,
  loading = false,
  customContent,
  hideButtons = false
}: ConfirmDialogProps) {
  if (!open) return null;

  const variantStyles = {
    danger: 'bg-red-600 dark:bg-red-500 hover:bg-red-700 dark:hover:bg-red-400 shadow-red-500/20',
    primary: 'bg-primary-600 dark:bg-primary-500 hover:bg-primary-700 dark:hover:bg-primary-400 shadow-primary-500/20',
    warning: 'bg-amber-500 hover:bg-amber-600 shadow-amber-500/20'
  };

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          {/* 遮罩层 - 使用更细腻的模糊和暗度 */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onCancel}
            className="absolute inset-0 bg-slate-950/40 backdrop-blur-md"
          />

          {/* 弹窗主体 */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 10 }}
            transition={{ type: 'spring', damping: 25, stiffness: 400 }}
            onClick={(e) => e.stopPropagation()}
            className="relative bg-white dark:bg-slate-900 rounded-[2rem] shadow-2xl max-w-md w-full overflow-hidden border border-slate-200/50 dark:border-slate-800/50"
          >
            {/* 装饰条 */}
            <div className={`h-1.5 w-full ${confirmVariant === 'danger' ? 'bg-red-500' : 'bg-primary-500'}`} />

            <div className="p-8">
              {/* 标题 */}
              <h3 className="text-2xl font-extrabold text-slate-900 dark:text-white mb-3 tracking-tight">
                {title}
              </h3>

              {/* 内容 */}
              <div className="text-slate-500 dark:text-slate-400 font-medium leading-relaxed mb-8 text-[15px]">
                {typeof message === 'string' ? (
                  message && <p className="whitespace-pre-line">{message}</p>
                ) : (
                  message
                )}
                {customContent}
              </div>

              {/* 动作区 */}
              {!hideButtons && (
                <div className="flex gap-3">
                  <button
                    onClick={onCancel}
                    disabled={loading}
                    className="flex-1 btn-secondary py-3 text-sm"
                  >
                    {cancelText}
                  </button>
                  <button
                    onClick={onConfirm}
                    disabled={loading}
                    className={`flex-[1.5] btn-primary py-3 text-sm flex items-center justify-center gap-2 ${variantStyles[confirmVariant]}`}
                  >
                    {loading ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      confirmText
                    )}
                  </button>
                </div>
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

