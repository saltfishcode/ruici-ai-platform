import {useCallback, useEffect, useRef, useState} from 'react';
import {motion} from 'framer-motion';
import {
  ChevronRight,
  Database,
  Download,
  Edit3,
  FileText,
  HardDrive,
  Loader2,
  MessageSquare,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
  Check,
} from 'lucide-react';
import {knowledgeBaseApi, KnowledgeBaseItem, KnowledgeBaseStats, SortOption, VectorStatus,} from '../api/knowledgebase';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';

interface KnowledgeBaseManagePageProps {
  onUpload: () => void;
  onChat: () => void;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  });
}

function StatusIcon({ status }: { status: VectorStatus }) {
  switch (status) {
    case 'COMPLETED': return <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]" />;
    case 'PROCESSING': return <Loader2 className="w-3 h-3 text-primary-500 animate-spin" />;
    case 'PENDING': return <div className="w-2 h-2 rounded-full bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.5)]" />;
    case 'FAILED': return <div className="w-2 h-2 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.5)]" />;
    default: return <div className="w-2 h-2 rounded-full bg-stone-300" />;
  }
}

function getStatusText(status: VectorStatus): string {
  switch (status) {
    case 'COMPLETED': return '已就绪';
    case 'PROCESSING': return '分析中';
    case 'PENDING': return '待排队';
    case 'FAILED': return '异常';
    default: return '未知';
  }
}

function StatCard({ icon: Icon, label, value, color }: { icon: any, label: string, value: number, color: string }) {
  return (
    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="glass-panel rounded-2xl p-6 flex items-center gap-5 card-hover">
      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center text-white shadow-lg ${color}`}><Icon className="w-7 h-7" /></div>
      <div>
          <p className="text-xs font-bold text-primary-300 dark:text-[#9ca3af] uppercase tracking-widest mb-1">{label}</p>
          <p className="text-3xl font-extrabold text-primary-800 dark:text-[#f3f4f6] tabular-nums tracking-tight">{value.toLocaleString()}</p>
      </div>
    </motion.div>
  );
}

export default function KnowledgeBaseManagePage({ onUpload, onChat }: KnowledgeBaseManagePageProps) {
  const [stats, setStats] = useState<KnowledgeBaseStats | null>(null);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [sortBy, setSortBy] = useState<SortOption>('time');
  const [deleteItem, setDeleteItem] = useState<KnowledgeBaseItem | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null);
  const [editingCategoryValue, setEditingCategoryValue] = useState('');
  const [savingCategory, setSavingCategory] = useState(false);
  const [revectorizing, setRevectorizing] = useState<number | null>(null);
  const categoryInputRef = useRef<HTMLInputElement>(null);

  const loadDataSilent = useCallback(async () => {
    try {
      const [statsData, kbList] = await Promise.all([
        knowledgeBaseApi.getStatistics(),
        searchKeyword ? knowledgeBaseApi.search(searchKeyword) : knowledgeBaseApi.getAllKnowledgeBases(sortBy),
      ]);
      setStats(statsData);
      setKnowledgeBases(kbList);
    } catch (error) { console.error('Silent load failed:', error); }
  }, [searchKeyword, sortBy]);

  const loadData = useCallback(async () => {
    try { setLoading(true); await loadDataSilent(); } finally { setLoading(false); }
  }, [loadDataSilent]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    const hasPending = knowledgeBases.some(kb => kb.vectorStatus === 'PENDING' || kb.vectorStatus === 'PROCESSING');
    if (hasPending && !loading) {
      const timer = setInterval(() => loadDataSilent(), 5000);
      return () => clearInterval(timer);
    }
  }, [knowledgeBases, loading, loadDataSilent]);

  const handleDelete = async () => {
    if (!deleteItem) return;
    try {
      setDeleting(true);
      await knowledgeBaseApi.deleteKnowledgeBase(deleteItem.id);
      setDeleteItem(null);
      await loadData();
    } finally { setDeleting(false); }
  };

  const handleDownload = async (kb: KnowledgeBaseItem) => {
    try {
      const blob = await knowledgeBaseApi.downloadKnowledgeBase(kb.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url; link.download = kb.originalFilename;
      document.body.appendChild(link); link.click();
      document.body.removeChild(link); window.URL.revokeObjectURL(url);
    } catch (error) { console.error('Download failed:', error); }
  };

  const handleStartEditCategory = (kb: KnowledgeBaseItem) => {
    setEditingCategoryId(kb.id);
    setEditingCategoryValue(kb.category || '');
    setTimeout(() => categoryInputRef.current?.focus(), 50);
  };

  const handleSaveCategory = async (id: number) => {
    try {
      setSavingCategory(true);
      await knowledgeBaseApi.updateCategory(id, editingCategoryValue.trim() || null);
      setEditingCategoryId(null);
      await loadData();
    } finally { setSavingCategory(false); }
  };

  const handleRevectorize = async (id: number) => {
    try {
      setRevectorizing(id);
      await knowledgeBaseApi.revectorize(id);
      await loadDataSilent();
    } finally { setRevectorizing(null); }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-20">
      <header className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <h1 className="text-3xl font-extrabold text-primary-800 dark:text-[#f3f4f6] flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-primary-500 flex items-center justify-center text-white shadow-medium"><Database className="w-7 h-7" /></div>
            企业知识库
          </h1>
          <p className="text-primary-400 dark:text-[#9ca3af] mt-3 font-medium max-w-lg">管理并索引你的私有知识文档。AI 将基于这些资料在问答助手中为你提供精准支持。</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={onChat} className="btn-secondary flex items-center gap-2"><MessageSquare className="w-4 h-4 text-primary-500" />进入问答</button>
          <button onClick={onUpload} className="btn-primary flex items-center gap-2"><Plus className="w-4 h-4" />上传文档</button>
        </div>
      </header>

      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard icon={FileText} label="总文档数" value={stats?.totalCount || 0} color="bg-indigo-500 shadow-indigo-500/20" />
        <StatCard icon={HardDrive} label="累计访问" value={stats?.totalAccessCount || 0} color="bg-teal-500 shadow-teal-500/20" />
        <StatCard icon={RefreshCw} label="已向量化" value={stats?.completedCount || 0} color="bg-amber-500 shadow-amber-500/20" />
        <StatCard icon={MessageSquare} label="累计问答" value={stats?.totalQuestionCount || 0} color="bg-primary-500 shadow-primary-500/20" />
      </section>

      <section className="space-y-6">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-white/40 dark:bg-[#1a1f2e]/40 p-2 rounded-2xl border border-stone-200/50 dark:border-[#2d3548]/50 backdrop-blur-sm">
          <div className="flex-1 w-full relative group">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-primary-300 group-focus-within:text-primary-500 transition-colors" />
            <input type="text" placeholder="搜索文档名称、关键词..." value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && loadData()} className="w-full pl-12 pr-4 py-3 bg-transparent border-none focus:ring-0 text-primary-800 dark:text-[#f3f4f6] font-medium" />
          </div>
          <div className="flex items-center gap-2 px-2">
            <select value={sortBy} onChange={(e) => setSortBy(e.target.value as SortOption)} className="bg-white dark:bg-[#1f2937] border border-stone-200 dark:border-[#2d3548] rounded-xl px-4 py-2 text-sm font-semibold outline-none focus:ring-2 focus:ring-blue-500/20 transition-all">
              <option value="time">按上传时间</option><option value="name">按名称排序</option><option value="size">按文件大小</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {loading ? (Array.from({ length: 6 }).map((_, i) => (<div key={i} className="h-48 dark-card animate-pulse bg-stone-100/50 dark:bg-[#1f2937]/50" />))) : knowledgeBases.length === 0 ? (
            <div className="col-span-full py-24 flex flex-col items-center justify-center dark-card border-dashed">
              <div className="w-20 h-20 rounded-3xl bg-stone-50 dark:bg-[#1f2937]/50 flex items-center justify-center mb-6"><FileText className="w-10 h-10 text-primary-200" /></div>
              <p className="text-primary-400 font-bold uppercase tracking-widest text-sm">空空如也</p>
              <button onClick={onUpload} className="mt-6 text-primary-500 font-bold flex items-center gap-2 hover:gap-3 transition-all">立即上传第一份文档 <Plus className="w-5 h-5" /></button>
            </div>
          ) : (knowledgeBases.map((kb) => (
              <motion.div key={kb.id} layout className="dark-card p-6 flex flex-col group card-hover relative overflow-hidden">
                <div className="flex items-start justify-between mb-5">
                  <div className="w-12 h-12 rounded-xl bg-stone-50 dark:bg-[#1f2937] flex items-center justify-center text-primary-300 dark:text-[#9ca3af] group-hover:bg-primary-500 group-hover:text-white transition-all duration-300 shadow-sm"><FileText className="w-6 h-6" /></div>
                  <div className="flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button onClick={() => handleDownload(kb)} className="p-2 hover:bg-stone-100 dark:hover:bg-[#1f2937] rounded-lg text-primary-300 hover:text-primary-500 transition-colors" title="下载"><Download className="w-4 h-4" /></button>
                    <button onClick={() => setDeleteItem(kb)} className="p-2 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg text-primary-300 hover:text-red-500 transition-colors" title="删除"><Trash2 className="w-4 h-4" /></button>
                  </div>
                </div>
                <h3 className="text-lg font-bold text-primary-800 dark:text-[#f3f4f6] truncate mb-2 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">{kb.name}</h3>
                <div className="flex flex-wrap gap-2 mb-6">
                  <span className="px-2.5 py-1 rounded-lg bg-stone-50 dark:bg-[#1f2937] text-[10px] font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-tight">{formatFileSize(kb.fileSize)}</span>
                  <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-primary-50 dark:bg-[#0f1117]/20 text-[10px] font-bold text-primary-600 dark:text-[#9ca3af] uppercase tracking-tight"><StatusIcon status={kb.vectorStatus} />{getStatusText(kb.vectorStatus)}</div>
                  {kb.category && <span className="px-2.5 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-900/20 text-[10px] font-bold text-emerald-600 dark:text-emerald-400 uppercase tracking-tight">{kb.category}</span>}
                </div>
                <div className="mt-auto pt-5 border-t border-stone-100 dark:border-[#2d3548] flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-[10px] font-bold text-primary-300 dark:text-[#9ca3af] uppercase tracking-widest">{formatDate(kb.uploadedAt)}</span>
                    <button onClick={() => handleStartEditCategory(kb)} className="text-primary-200 hover:text-primary-500 transition-colors" title="编辑分类"><Edit3 className="w-3.5 h-3.5" /></button>
                    {kb.vectorStatus === 'COMPLETED' && <button onClick={() => handleRevectorize(kb.id)} className="text-primary-200 hover:text-primary-500 transition-colors" title="重新索引"><RefreshCw className={`w-3.5 h-3.5 ${revectorizing === kb.id ? 'animate-spin' : ''}`} /></button>}
                  </div>
                  <button onClick={() => onChat()} className="p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/30 text-primary-500 transition-colors"><ChevronRight className="w-5 h-5" /></button>
                </div>
                {editingCategoryId === kb.id && (
                  <div className="absolute inset-0 z-10 bg-white/95 dark:bg-[#1f2937]/95 backdrop-blur-sm flex flex-col items-center justify-center p-6 animate-in fade-in zoom-in-95 duration-200">
                    <p className="text-xs font-bold text-primary-300 uppercase tracking-widest mb-4">修改分类名称</p>
                    <input ref={categoryInputRef} type="text" value={editingCategoryValue} onChange={(e) => setEditingCategoryValue(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSaveCategory(kb.id)} className="w-full text-center bg-transparent border-b-2 border-primary-500 py-2 mb-6 font-bold text-lg focus:outline-none" placeholder="未分类" />
                    <div className="flex gap-2">
                      <button onClick={() => setEditingCategoryId(null)} className="p-2 rounded-xl hover:bg-stone-100 dark:hover:bg-[#374151] text-primary-300"><X className="w-5 h-5" /></button>
                      <button onClick={() => handleSaveCategory(kb.id)} disabled={savingCategory} className="p-2 rounded-xl bg-primary-800 text-white shadow-lg shadow-primary-500/30">{savingCategory ? <Loader2 className="w-5 h-5 animate-spin" /> : <Check className="w-5 h-5" />}</button>
                    </div>
                  </div>
                )}
              </motion.div>
            ))
          )}
        </div>
      </section>
      <DeleteConfirmDialog open={deleteItem !== null} item={deleteItem} itemType="知识库" loading={deleting} onConfirm={handleDelete} onCancel={() => setDeleteItem(null)} />
    </div>
  );
}
