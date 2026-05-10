import { useCallback, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Brain,
  Loader2,
  Plus,
  RefreshCw,
  Save,
  X,
  Check,
  SlidersHorizontal,
} from 'lucide-react';
import { aiRuntimeConfigApi, DOMAIN_LABELS, SCENE_LABELS, DOMAIN_OPTIONS, SCENE_OPTIONS } from '../api/aiRuntimeConfig';
import type { AiRuntimeConfigListItem, SaveAiRuntimeConfigRequest } from '../api/aiRuntimeConfig';

export default function AiRuntimeConfigPage() {
  const [items, setItems] = useState<AiRuntimeConfigListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [filterDomain, setFilterDomain] = useState('');
  const [filterScene, setFilterScene] = useState('');
  const [editItem, setEditItem] = useState<SaveAiRuntimeConfigRequest | null>(null);

  const fetchList = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await aiRuntimeConfigApi.list(
        filterDomain || undefined,
        filterScene || undefined,
      );
      setItems(data ?? []);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [filterDomain, filterScene]);

  useEffect(() => { fetchList(); }, [fetchList]);

  const handleToggle = async (id: number, enabled: boolean) => {
    try {
      await aiRuntimeConfigApi.toggleEnabled(id, enabled);
      setItems(prev => prev.map(i => i.id === id ? { ...i, enabled } : i));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败');
    }
  };

  const handleSave = async () => {
    if (!editItem) return;
    setSaving(true);
    try {
      await aiRuntimeConfigApi.save(editItem);
      setEditItem(null);
      setSuccessMsg('配置已保存');
      setTimeout(() => setSuccessMsg(''), 2000);
      await fetchList();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleRefresh = async () => {
    try {
      const resp = await aiRuntimeConfigApi.refresh();
      setSuccessMsg(`缓存已刷新，最新版本: v${resp.latestConfigVersion}`);
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '刷新失败');
    }
  };

  const openNew = () => {
    setEditItem({
      configKey: 'THIRD_PARTY_MODEL',
      domain: 'chat',
      scene: 'global',
      providerId: '',
      modelName: '',
      fallbackModelName: '',
      enabled: true,
      priority: 100,
    });
  };

  const openEdit = (item: AiRuntimeConfigListItem) => {
    setEditItem({
      id: item.id,
      configKey: item.configKey,
      domain: item.domain,
      scene: item.scene,
      providerId: item.providerId ?? '',
      modelName: item.modelName,
      fallbackModelName: item.fallbackModelName ?? '',
      enabled: item.enabled,
      priority: item.priority,
    });
  };

  return (
    <div className="max-w-6xl mx-auto">
      {/* 顶部标题区 */}
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="mb-8">
        <div className="flex items-center gap-4 mb-2">
          <div className="w-12 h-12 rounded-2xl bg-primary-800 dark:bg-[#1f2937] flex items-center justify-center text-white dark:text-[#f3f4f6]">
            <Brain className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-primary-800 dark:text-[#f3f4f6] tracking-tight">AI 模型配置</h1>
            <p className="text-sm text-primary-400 dark:text-[#9ca3af] mt-0.5">管理各业务模块使用的 AI 模型与提供商</p>
          </div>
        </div>
      </motion.div>

      {/* 全局消息 */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 rounded-xl flex items-center gap-3 text-red-700 dark:text-red-400 text-sm">
          <X className="w-4 h-4 shrink-0" />
          {error}
          <button onClick={() => setError('')} className="ml-auto hover:opacity-70"><X className="w-4 h-4" /></button>
        </div>
      )}
      {successMsg && (
        <div className="mb-6 p-4 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 rounded-xl flex items-center gap-3 text-emerald-700 dark:text-emerald-400 text-sm">
          <Check className="w-4 h-4 shrink-0" />
          {successMsg}
        </div>
      )}

      {/* 工具栏 */}
      <div className="mb-6 flex flex-wrap items-center gap-3">
        {/* 过滤器 */}
        <div className="flex items-center gap-2 bg-white dark:bg-[#1a1f2e] border border-stone-200 dark:border-[#2d3548] rounded-xl px-4 py-2.5 shadow-sm">
          <SlidersHorizontal className="w-4 h-4 text-primary-300" />
          <select
            value={filterDomain}
            onChange={e => setFilterDomain(e.target.value)}
            className="text-sm bg-transparent border-0 outline-none text-primary-600 dark:text-[#d1d5db] pr-6 cursor-pointer"
          >
            <option value="">所有域</option>
            {DOMAIN_OPTIONS.map(d => (
              <option key={d} value={d}>{DOMAIN_LABELS[d] ?? d}</option>
            ))}
          </select>
          <span className="text-primary-200 dark:text-[#9ca3af]">|</span>
          <select
            value={filterScene}
            onChange={e => setFilterScene(e.target.value)}
            className="text-sm bg-transparent border-0 outline-none text-primary-600 dark:text-[#d1d5db] pr-6 cursor-pointer"
          >
            <option value="">所有场景</option>
            {SCENE_OPTIONS.map(s => (
              <option key={s} value={s}>{SCENE_LABELS[s] ?? s}</option>
            ))}
          </select>
        </div>

        <button onClick={openNew} className="flex items-center gap-2 px-4 py-2.5 bg-primary-800 dark:bg-[#1f2937] hover:bg-primary-700 dark:hover:bg-stone-100 text-white dark:text-[#f3f4f6] rounded-[32px] font-semibold text-sm shadow-soft transition-all">
          <Plus className="w-4 h-4" />新增
        </button>
        <button onClick={handleRefresh} className="flex items-center gap-2 px-4 py-2.5 bg-white dark:bg-[#1a1f2e] border border-stone-200 dark:border-[#2d3548] hover:bg-stone-50 dark:hover:bg-[#1f2937] text-primary-600 dark:text-[#d1d5db] rounded-xl font-semibold text-sm shadow-sm transition-all">
          <RefreshCw className="w-4 h-4" />刷新缓存
        </button>
      </div>

      {/* 表格 */}
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="bg-white dark:bg-[#1a1f2e] rounded-2xl border border-stone-200 dark:border-[#2d3548] shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-24"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
        ) : items.length === 0 ? (
          <div className="text-center py-24 text-primary-300 dark:text-[#9ca3af]">
            <Brain className="w-12 h-12 mx-auto mb-4 opacity-30" />
            <p className="font-semibold">暂无配置</p>
            <p className="text-sm mt-1">点击「新增」创建第一条 AI 模型配置</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-stone-100 dark:border-[#2d3548] bg-stone-50/50 dark:bg-[#1f2937]/30">
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">域</th>
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">场景</th>
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">配置键</th>
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">Provider</th>
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">模型</th>
                  <th className="text-left px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">回退模型</th>
                  <th className="text-center px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">状态</th>
                  <th className="text-right px-5 py-3.5 font-bold text-primary-400 dark:text-[#9ca3af] text-xs uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <motion.tr
                    key={item.id}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.03 }}
                    className="border-b border-stone-100 dark:border-[#2d3548]/50 hover:bg-stone-50/80 dark:hover:bg-[#1f2937]/30 transition-colors"
                  >
                    <td className="px-5 py-4">
                      <span className="px-2.5 py-1 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 rounded-lg text-xs font-bold">
                        {DOMAIN_LABELS[item.domain] ?? item.domain}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-primary-600 dark:text-[#d1d5db] font-medium">{SCENE_LABELS[item.scene] ?? item.scene}</td>
                    <td className="px-5 py-4 font-mono text-xs text-primary-500 dark:text-[#9ca3af]">{item.configKey}</td>
                    <td className="px-5 py-4 text-primary-600 dark:text-[#d1d5db]">{item.providerId}</td>
                    <td className="px-5 py-4 font-mono text-xs text-primary-700 dark:text-[#e5e7eb] font-semibold">{item.modelName}</td>
                    <td className="px-5 py-4 font-mono text-xs text-primary-400 dark:text-[#9ca3af]">{item.fallbackModelName ?? '-'}</td>
                    <td className="px-5 py-4 text-center">
                      <button
                        onClick={() => handleToggle(item.id, !item.enabled)}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                          item.enabled ? 'bg-emerald-500' : 'bg-stone-300 dark:bg-[#374151]'
                        }`}
                      >
                        <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform shadow-sm ${
                          item.enabled ? 'translate-x-6' : 'translate-x-1'
                        }`} />
                      </button>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <button onClick={() => openEdit(item)} className="px-3 py-1.5 text-xs font-bold text-primary-600 dark:text-[#9ca3af] hover:bg-primary-50 dark:hover:bg-primary-950/40 rounded-lg transition-colors">
                        编辑
                      </button>
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </motion.div>

      {/* 编辑弹窗 */}
      {editItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => !saving && setEditItem(null)}>
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white dark:bg-[#1a1f2e] rounded-2xl shadow-2xl border border-stone-200 dark:border-[#2d3548] w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-6 border-b border-stone-100 dark:border-[#2d3548]">
              <h2 className="text-lg font-bold text-primary-800 dark:text-[#f3f4f6]">
                {editItem.id ? '编辑配置' : '新增配置'}
              </h2>
              <button onClick={() => !saving && setEditItem(null)} className="p-2 text-primary-300 hover:text-primary-500 dark:hover:text-[#e5e7eb] rounded-lg hover:bg-stone-100 dark:hover:bg-[#1f2937]">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-5">
              {/* configKey */}
              <div>
                <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">配置键</label>
                <input value={editItem.configKey} onChange={e => setEditItem({ ...editItem, configKey: e.target.value })}
                  className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none" />
              </div>

              {/* domain + scene */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">能力域</label>
                  <select value={editItem.domain} onChange={e => setEditItem({ ...editItem, domain: e.target.value })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none">
                    {DOMAIN_OPTIONS.map(d => <option key={d} value={d}>{DOMAIN_LABELS[d]}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">业务场景</label>
                  <select value={editItem.scene} onChange={e => setEditItem({ ...editItem, scene: e.target.value })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none">
                    {SCENE_OPTIONS.map(s => <option key={s} value={s}>{SCENE_LABELS[s]}</option>)}
                  </select>
                </div>
              </div>

              {/* provider + model */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">Provider</label>
                  <input value={editItem.providerId ?? ''} onChange={e => setEditItem({ ...editItem, providerId: e.target.value })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
                    placeholder="如 third-party / dashscope" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">模型名</label>
                  <input value={editItem.modelName} onChange={e => setEditItem({ ...editItem, modelName: e.target.value })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
                    placeholder="如 gpt-5.2 / text-embedding-v3" />
                </div>
              </div>

              {/* fallback + priority */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">回退模型</label>
                  <input value={editItem.fallbackModelName ?? ''} onChange={e => setEditItem({ ...editItem, fallbackModelName: e.target.value })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-primary-400 dark:text-[#9ca3af] uppercase tracking-wider mb-1.5">优先级</label>
                  <input type="number" value={editItem.priority ?? 100} onChange={e => setEditItem({ ...editItem, priority: parseInt(e.target.value) || 100 })}
                    className="w-full px-3.5 py-2.5 border border-stone-200 dark:border-[#2d3548] rounded-xl bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none" />
                </div>
              </div>

              {/* enabled */}
              <div className="flex items-center gap-3">
                <button onClick={() => setEditItem({ ...editItem, enabled: !editItem.enabled })}
                  className={`relative inline-flex h-7 w-12 items-center rounded-full transition-colors ${editItem.enabled ? 'bg-emerald-500' : 'bg-stone-300 dark:bg-[#374151]'}`}>
                  <span className={`inline-block h-5 w-5 transform rounded-full bg-white transition-transform shadow-sm ${editItem.enabled ? 'translate-x-6' : 'translate-x-1'}`} />
                </button>
                <span className="text-sm text-primary-500 dark:text-[#9ca3af] font-medium">{editItem.enabled ? '已启用' : '已禁用'}</span>
              </div>

              {/* 域提示 */}
              <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 rounded-xl text-xs text-amber-700 dark:text-amber-400">
                {editItem.domain === 'chat'
                  ? '通用对话域可使用任意已配置的 Provider。当前默认使用第三方 GPT 代理。'
                  : '向量化与语音域当前仅支持 dashscope 提供商，切换 provider 会被拒绝。'}
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 p-6 border-t border-stone-100 dark:border-[#2d3548]">
              <button onClick={() => setEditItem(null)} disabled={saving}
                className="px-5 py-2.5 text-sm font-bold text-primary-500 dark:text-[#9ca3af] hover:bg-stone-100 dark:hover:bg-[#1f2937] rounded-xl transition-colors">
                取消
              </button>
              <button onClick={handleSave} disabled={saving}
                className="flex items-center gap-2 px-5 py-2.5 bg-primary-800 dark:bg-[#1f2937] hover:bg-primary-700 dark:hover:bg-stone-100 disabled:bg-primary-300 text-white dark:text-[#f3f4f6] rounded-[32px] font-bold text-sm shadow-soft transition-all">
                {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                保存
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
