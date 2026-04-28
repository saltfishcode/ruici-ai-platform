import {type ChangeEvent, type DragEvent, type ReactNode, useCallback, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {AlertCircle, FileText, Loader2, Upload, X} from 'lucide-react';

export interface FileUploadCardProps {
  /** 标题 */
  title: string;
  /** 副标题 */
  subtitle: string;
  /** 接受的文件类型 */
  accept: string;
  /** 支持的格式说明 */
  formatHint: string;
  /** 最大文件大小说明 */
  maxSizeHint: string;
  /** 是否正在上传 */
  uploading?: boolean;
  /** 上传按钮文字 */
  uploadButtonText?: string;
  /** 选择按钮文字 */
  selectButtonText?: string;
  /** 是否显示名称输入框 */
  showNameInput?: boolean;
  /** 名称输入框占位符 */
  namePlaceholder?: string;
  /** 名称输入框标签 */
  nameLabel?: string;
  /** 错误信息 */
  error?: string;
  /** 文件选择回调 */
  onFileSelect?: (file: File) => void;
  /** 上传回调 */
  onUpload: (file: File, name?: string) => void;
  /** 返回回调 */
  onBack?: () => void;
  /** 额外内容 */
  extraContent?: ReactNode;
}

export default function FileUploadCard({
  title,
  subtitle,
  accept,
  formatHint,
  maxSizeHint,
  uploading = false,
  uploadButtonText = '开始上传',
  selectButtonText = '选择文件',
  showNameInput = false,
  namePlaceholder = '留空则使用文件名',
  nameLabel = '名称（可选）',
  error,
  onFileSelect,
  onUpload,
  onBack,
  extraContent,
}: FileUploadCardProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [name, setName] = useState('');

  const handleDragOver = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleDrop = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      setSelectedFile(files[0]);
      onFileSelect?.(files[0]);
    }
  }, [onFileSelect]);

  const handleFileChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      setSelectedFile(files[0]);
      onFileSelect?.(files[0]);
    }
  }, [onFileSelect]);

  const handleUpload = () => {
    if (!selectedFile) return;
    onUpload(selectedFile, name.trim() || undefined);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <motion.div
      className="max-w-4xl mx-auto pt-10 pb-20 px-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* 头部标题区 */}
      <div className="text-center mb-12">
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 text-[10px] font-bold uppercase tracking-widest mb-6 border border-primary-100 dark:border-primary-800"
        >
          <Upload className="w-3 h-3" /> File Processor
        </motion.div>
        <h1 className="text-4xl md:text-5xl font-extrabold text-slate-900 dark:text-white mb-4 tracking-tight leading-tight">
          {title}
        </h1>
        <p className="text-slate-500 dark:text-slate-400 max-w-2xl mx-auto text-lg font-medium">
          {subtitle}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-8">
        {extraContent && (
          <motion.div
            className="dark-card p-8"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
          >
            {extraContent}
          </motion.div>
        )}

        {/* 主上传卡片 */}
        <motion.div
          className={`relative group h-96 dark-card overflow-hidden cursor-pointer transition-all duration-500 flex flex-col items-center justify-center border-2 border-dashed
          ${dragOver ? 'border-primary-500 bg-primary-50/30 dark:bg-primary-900/10 scale-[1.01]' : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 hover:bg-slate-50/50 dark:hover:bg-slate-900/50'}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => !uploading && document.getElementById('file-upload-input')?.click()}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <input
            type="file"
            id="file-upload-input"
            className="hidden"
            accept={accept}
            onChange={handleFileChange}
            disabled={uploading}
          />

          <AnimatePresence mode="wait">
            {selectedFile ? (
              <motion.div
                key="selected"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="w-full max-w-md px-8 text-center flex flex-col items-center"
              >
                <div className="w-24 h-24 rounded-3xl bg-primary-500 text-white flex items-center justify-center mb-6 shadow-glow transition-transform group-hover:scale-110 duration-500">
                  <FileText className="w-12 h-12" />
                </div>
                <div className="w-full bg-white dark:bg-slate-950 p-4 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm mb-6 relative">
                  <p className="font-bold text-slate-900 dark:text-white truncate pr-6">{selectedFile.name}</p>
                  <p className="text-[10px] text-slate-400 dark:text-slate-500 font-bold mt-1 uppercase">
                    {formatFileSize(selectedFile.size)} · Ready to Index
                  </p>
                  <button
                    onClick={(e) => { e.stopPropagation(); setSelectedFile(null); }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-300 hover:text-red-500 transition-colors"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                
                {showNameInput && (
                  <div className="w-full mb-8 text-left">
                    <label className="block text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest mb-2 ml-1">{nameLabel}</label>
                    <input
                      type="text"
                      value={name}
                      onClick={(e) => e.stopPropagation()}
                      onChange={(e) => setName(e.target.value)}
                      placeholder={namePlaceholder}
                      className="dark-input"
                    />
                  </div>
                )}

                <button
                  onClick={(e) => { e.stopPropagation(); handleUpload(); }}
                  disabled={uploading}
                  className="btn-primary w-full py-4 text-lg flex items-center justify-center gap-3"
                >
                  {uploading ? <Loader2 className="w-6 h-6 animate-spin" /> : <><Upload className="w-6 h-6" /> {uploadButtonText}</>}
                </button>
              </motion.div>
            ) : (
              <motion.div
                key="empty"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex flex-col items-center text-center px-10"
              >
                <div className="w-20 h-20 rounded-2xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 mb-8 transition-all duration-500 group-hover:bg-primary-50 dark:group-hover:bg-primary-900/20 group-hover:text-primary-500 group-hover:-translate-y-2">
                  <Upload className="w-10 h-10" />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 dark:text-white mb-3 tracking-tight">
                  点击或拖拽文件至此处
                </h3>
                <p className="text-slate-400 dark:text-slate-500 font-medium mb-8 max-w-xs">
                  {formatHint} · {maxSizeHint}
                </p>
                <button className="btn-secondary group-hover:border-primary-500 group-hover:text-primary-600 transition-all duration-300">
                  {selectButtonText}
                </button>
              </motion.div>
            )}
          </AnimatePresence>

          {/* 进度装饰 */}
          {uploading && (
             <motion.div 
              className="absolute bottom-0 left-0 h-1 bg-primary-500"
              initial={{ width: 0 }}
              animate={{ width: '100%' }}
              transition={{ duration: 2, repeat: Infinity }}
             />
          )}
        </motion.div>
      </div>

      {error && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-8 p-4 bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-900/30 rounded-2xl flex items-center gap-3 text-red-600 dark:text-red-400 font-semibold"
        >
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </motion.div>
      )}

      {onBack && (
        <div className="mt-12 text-center">
          <button onClick={onBack} className="btn-ghost text-sm font-bold uppercase tracking-widest">
            返回上一页
          </button>
        </div>
      )}
    </motion.div>
  );
}
