import {useEffect, useMemo, useRef, useState} from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {Virtuoso, type VirtuosoHandle} from 'react-virtuoso';
import {knowledgeBaseApi, type KnowledgeBaseItem} from '../api/knowledgebase';
import {ragChatApi, type RagChatSessionListItem} from '../api/ragChat';
import {formatDateOnly} from '../utils/date';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import CodeBlock from '../components/CodeBlock';
import {Bot, ChevronDown, ChevronLeft, ChevronRight, Edit, Loader2, MessageSquare, Plus, Sparkles, Trash2,} from 'lucide-react';

interface KnowledgeBaseQueryPageProps {
  onBack?: () => void;
  onUpload?: () => void;
}

interface Message {
  id?: number;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface CategoryGroup {
  name: string;
  items: KnowledgeBaseItem[];
  isExpanded: boolean;
}

export default function KnowledgeBaseQueryPage({ onUpload, onBack }: KnowledgeBaseQueryPageProps) {
  if (false) console.log(onUpload, onBack);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
  const [selectedKbIds, setSelectedKbIds] = useState<Set<number>>(new Set());
  const [loadingList, setLoadingList] = useState(true);
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set(['未分类']));
  const [leftPanelOpen, setLeftPanelOpen] = useState(true);
  const [sessions, setSessions] = useState<RagChatSessionListItem[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(null);
  const [currentSessionTitle, setCurrentSessionTitle] = useState<string>('');
  const [loadingSessions, setLoadingSessions] = useState(false);
  const [sessionDeleteConfirm, setSessionDeleteConfirm] = useState<{ id: number; title: string } | null>(null);
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);

  const virtuosoRef = useRef<VirtuosoHandle>(null);

  useEffect(() => {
    loadKnowledgeBases();
    loadSessions();
  }, []);

  const loadKnowledgeBases = async () => {
    setLoadingList(true);
    try {
      const list = await knowledgeBaseApi.getAllKnowledgeBases('time', 'COMPLETED');
      setKnowledgeBases(list);
    } catch (err) { console.error('Load KB failed:', err); }
    finally { setLoadingList(false); }
  };

  const groupedKnowledgeBases = useMemo((): CategoryGroup[] => {
    const groups: Map<string, KnowledgeBaseItem[]> = new Map();
    knowledgeBases.forEach(kb => {
      const category = kb.category || '未分类';
      if (!groups.has(category)) groups.set(category, []);
      groups.get(category)!.push(kb);
    });
    const result: CategoryGroup[] = [];
    const sortedCategories = Array.from(groups.keys()).sort((a, b) => {
      if (a === '未分类') return 1;
      if (b === '未分类') return -1;
      return a.localeCompare(b);
    });
    sortedCategories.forEach(name => {
      result.push({ name, items: groups.get(name)!, isExpanded: expandedCategories.has(name) });
    });
    return result;
  }, [knowledgeBases, expandedCategories]);

  const toggleCategory = (category: string) => {
    setExpandedCategories(prev => {
      const next = new Set(prev);
      if (next.has(category)) next.delete(category);
      else next.add(category);
      return next;
    });
  };

  const loadSessions = async () => {
    setLoadingSessions(true);
    try {
      const list = await ragChatApi.listSessions();
      setSessions(list);
      if (currentSessionId) {
        const currentSession = list.find(session => session.id === currentSessionId);
        if (currentSession) setCurrentSessionTitle(currentSession.title);
      }
    } catch (err) { console.error('Load sessions failed:', err); }
    finally { setLoadingSessions(false); }
  };

  const handleToggleKb = (kbId: number) => {
    setSelectedKbIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(kbId)) newSet.delete(kbId);
      else newSet.add(kbId);
      if (newSet.size !== prev.size && currentSessionId) {
        setCurrentSessionId(null);
        setCurrentSessionTitle('');
        setMessages([]);
      }
      return newSet;
    });
  };

  const handleNewSession = () => {
    setCurrentSessionId(null);
    setCurrentSessionTitle('');
    setMessages([]);
  };

  const handleLoadSession = async (sessionId: number) => {
    try {
      const detail = await ragChatApi.getSessionDetail(sessionId);
      setCurrentSessionId(detail.id);
      setCurrentSessionTitle(detail.title);
      setSelectedKbIds(new Set(detail.knowledgeBases.map(kb => kb.id)));
      setMessages(detail.messages.map(m => ({
        id: m.id, type: m.type, content: m.content, timestamp: new Date(m.createdAt),
      })));
    } catch (err) { console.error('Load session detail failed:', err); }
  };

  const handleDeleteSession = async () => {
    if (!sessionDeleteConfirm) return;
    try {
      await ragChatApi.deleteSession(sessionDeleteConfirm.id);
      await loadSessions();
      if (currentSessionId === sessionDeleteConfirm.id) handleNewSession();
      setSessionDeleteConfirm(null);
    } catch (err) { console.error('Delete session failed:', err); }
  };

  const handleSend = async () => {
    if (!question.trim() || selectedKbIds.size === 0 || loading) return;
    const userQuestion = question.trim();
    setQuestion('');
    setLoading(true);
    const userMessage: Message = { type: 'user', content: userQuestion, timestamp: new Date() };
    setMessages(prev => [...prev, userMessage]);
    const assistantMessage: Message = { type: 'assistant', content: '', timestamp: new Date() };
    setMessages(prev => [...prev, assistantMessage]);
    try {
      let sessionId = currentSessionId;
      if (!sessionId) {
        const session = await ragChatApi.createSession(Array.from(selectedKbIds));
        sessionId = session.id;
        setCurrentSessionId(sessionId);
        setCurrentSessionTitle(session.title);
        await loadSessions();
      }
      let fullContent = '';
      await ragChatApi.sendMessageStream(
        sessionId,
        userQuestion,
        (chunk) => {
          fullContent += chunk;
          setMessages(prev => {
            const next = [...prev];
            next[next.length - 1] = { ...next[next.length - 1], content: fullContent };
            return next;
          });
        },
        () => {
          setLoading(false);
          loadSessions();
        },
        (err) => {
          console.error('Stream error:', err);
          setLoading(false);
          setMessages(prev => {
            const next = [...prev];
            next[next.length - 1] = { ...next[next.length - 1], content: fullContent + '\n\n*(发送错误，请重试)*' };
            return next;
          });
        }
      );
    } catch (err) {
      console.error('Send failed:', err);
      setLoading(false);
    }
  };

  return (
    <div className="h-[calc(100vh-80px)] flex flex-col lg:flex-row gap-6">
      <aside className={`flex flex-col gap-4 transition-all duration-300 ${leftPanelOpen ? 'w-full lg:w-80' : 'w-full lg:w-20'}`}>
        <div className="flex-1 dark-card flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
            {leftPanelOpen && <h3 className="font-bold text-sm text-slate-800 dark:text-white uppercase tracking-wider">历史会话</h3>}
            <button onClick={handleNewSession} className="p-2 rounded-lg bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors group">
              <Plus className="w-4 h-4 group-hover:rotate-90 transition-transform" />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
            {loadingSessions ? (
              <div className="flex justify-center p-8"><Loader2 className="w-5 h-5 animate-spin text-slate-300" /></div>
            ) : (
              sessions.map(s => (
                <button key={s.id} onClick={() => handleLoadSession(s.id)} className={`w-full group flex items-center gap-3 p-3 rounded-xl transition-all ${currentSessionId === s.id ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300 shadow-sm' : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400'}`}>
                  <MessageSquare className={`w-4 h-4 flex-shrink-0 ${currentSessionId === s.id ? 'text-primary-500' : 'text-slate-400'}`} />
                  {leftPanelOpen && (
                    <div className="flex-1 text-left min-w-0">
                      <p className="text-sm font-medium truncate">{s.title}</p>
                      <p className="text-[10px] text-slate-400">{formatDateOnly(s.updatedAt)}</p>
                    </div>
                  )}
                  {currentSessionId === s.id && leftPanelOpen && (
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={(e) => { e.stopPropagation(); setSessionDeleteConfirm({ id: s.id, title: s.title }); }} className="p-1 hover:text-red-500"><Trash2 className="w-3.5 h-3.5" /></button>
                    </div>
                  )}
                </button>
              ))
            )}
          </div>
        </div>

        <div className="h-64 dark-card flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
            {leftPanelOpen && <h3 className="font-bold text-sm text-slate-800 dark:text-white uppercase tracking-wider">选用资料</h3>}
            <button onClick={() => setLeftPanelOpen(!leftPanelOpen)} className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-400 transition-colors">
              {leftPanelOpen ? <ChevronLeft className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-4 custom-scrollbar">
            {loadingList ? <div className="p-4 text-center"><Loader2 className="w-4 h-4 animate-spin mx-auto text-slate-300" /></div> : groupedKnowledgeBases.map(group => (
              <div key={group.name} className="space-y-1">
                {leftPanelOpen && (
                  <button onClick={() => toggleCategory(group.name)} className="w-full flex items-center justify-between px-2 py-1 text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest hover:text-slate-600 transition-colors">
                    {group.name}
                    {group.isExpanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
                  </button>
                )}
                {group.isExpanded && group.items.map(kb => (
                  <button key={kb.id} onClick={() => handleToggleKb(kb.id)} className={`w-full flex items-center gap-3 p-2.5 rounded-xl transition-all ${selectedKbIds.has(kb.id) ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-300' : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-500'}`}>
                    <div className={`w-2 h-2 rounded-full flex-shrink-0 ${selectedKbIds.has(kb.id) ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-300 dark:bg-slate-700'}`} />
                    {leftPanelOpen && <span className="text-xs font-semibold truncate flex-1 text-left">{kb.name}</span>}
                  </button>
                ))}
              </div>
            ))}
          </div>
        </div>
      </aside>

      <section className="flex-1 flex flex-col dark-card overflow-hidden relative">
        <header className="px-6 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-white/50 dark:bg-slate-900/50 backdrop-blur-md sticky top-0 z-10">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center text-primary-600 dark:text-primary-400">
              <Bot className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <h2 className="font-bold text-slate-900 dark:text-white truncate">{currentSessionTitle || '新会话'}</h2>
              <p className="text-[10px] text-slate-400 dark:text-slate-500 font-bold uppercase tracking-tight">
                {selectedKbIds.size > 0 ? `已激活 ${selectedKbIds.size} 份知识资料` : '请选择知识资料以开始'}
              </p>
            </div>
          </div>
        </header>

        <div className="flex-1 relative">
          {messages.length === 0 ? (
            <div className="absolute inset-0 flex flex-col items-center justify-center p-8 text-center">
              <div className="w-20 h-20 rounded-3xl bg-slate-50 dark:bg-slate-800/50 flex items-center justify-center mb-6 border border-slate-100 dark:border-slate-700/50 shadow-sm">
                <Sparkles className="w-10 h-10 text-primary-400/50" />
              </div>
              <h3 className="text-xl font-bold text-slate-800 dark:text-white mb-2">准备好探索了吗？</h3>
              <p className="max-w-xs text-slate-500 dark:text-slate-400 text-sm leading-relaxed">选择左侧的知识库资料，然后在下方输入问题。AI 将精准检索资料并为你提供深度见解。</p>
            </div>
          ) : (
            <Virtuoso
              ref={virtuosoRef}
              data={messages}
              className="custom-scrollbar"
              initialTopMostItemIndex={messages.length - 1}
              itemContent={(_index, message) => (
                <div className={`p-6 md:px-10 ${message.type === 'assistant' ? 'bg-slate-50/50 dark:bg-slate-900/30' : ''}`}>
                  <div className="max-w-4xl mx-auto flex gap-6">
                    <div className={`w-9 h-9 rounded-xl flex-shrink-0 flex items-center justify-center shadow-sm ${message.type === 'user' ? 'bg-slate-800 text-white' : 'bg-primary-500 text-white shadow-glow shadow-primary-500/20'}`}>
                      {message.type === 'user' ? <Edit className="w-4 h-4" /> : <Bot className="w-5 h-5" />}
                    </div>
                    <div className="flex-1 min-w-0 space-y-2">
                      <div className="flex items-center gap-3">
                        <span className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">{message.type === 'user' ? 'YOU' : 'RUICI AI'}</span>
                      </div>
                      <div className="prose prose-slate dark:prose-invert max-w-none prose-p:leading-relaxed prose-pre:p-0 prose-pre:bg-transparent">
                        <ReactMarkdown 
                          remarkPlugins={[remarkGfm]}
                          components={{
                            code({ inline, className, children, ...props }: any) {
                              const match = /language-(\w+)/.exec(className || '');
                              return !inline ? (
                                <CodeBlock language={match ? match[1] : undefined}>
                                  {String(children).replace(/\n$/, '')}
                                </CodeBlock>
                              ) : (
                                <code className="px-1.5 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-primary-600 dark:text-primary-400 font-mono text-[0.9em]" {...props}>
                                  {children}
                                </code>
                              );
                            }
                          }}
                        >
                          {message.content}
                        </ReactMarkdown>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              followOutput="smooth"
            />
          )}
        </div>

        <footer className="p-6 border-t border-slate-100 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md">
          <div className="max-w-4xl mx-auto relative group">
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
              placeholder="输入你的问题，按 Enter 发送..."
              className="w-full pl-6 pr-16 py-4 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-2xl text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-4 focus:ring-primary-500/10 focus:border-primary-500 transition-all resize-none min-h-[60px] max-h-48 custom-scrollbar shadow-inner"
              rows={1}
            />
            <div className="absolute right-3 bottom-3">
              <button
                onClick={handleSend}
                disabled={loading || !question.trim() || selectedKbIds.size === 0}
                className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all ${loading || !question.trim() || selectedKbIds.size === 0 ? 'bg-slate-100 dark:bg-slate-800 text-slate-400' : 'bg-primary-600 text-white shadow-lg shadow-primary-500/30 hover:shadow-xl hover:shadow-primary-500/40 hover:-translate-y-0.5 active:scale-95'}`}
              >
                {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <ChevronRight className="w-6 h-6" />}
              </button>
            </div>
          </div>
        </footer>
      </section>

      <DeleteConfirmDialog open={sessionDeleteConfirm !== null} item={sessionDeleteConfirm} itemType="会话" onConfirm={handleDeleteSession} onCancel={() => setSessionDeleteConfirm(null)} />
    </div>
  );
}
