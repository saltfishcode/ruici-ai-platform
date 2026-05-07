import {Link, Outlet, useLocation, useNavigate} from 'react-router-dom';
import {AnimatePresence, motion} from 'framer-motion';
import {
  AudioWaveform,
  Bot,
  CalendarDays,
  FileSearch,
  History,
  Library,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  Sparkles,
  Info,
  Sun,
  Zap,
  Brain,
} from 'lucide-react';
import {useTheme} from '../hooks/useTheme';
import {useState} from 'react';
import UnifiedInterviewModal, {type UnifiedInterviewConfig} from './UnifiedInterviewModal';

interface NavItem {
  id: string;
  path: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  description?: string;
}

interface NavGroup {
  id: string;
  title: string;
  items: NavItem[];
}

export default function Layout() {
  const location = useLocation();
  const currentPath = location.pathname;
  const {theme, toggleTheme} = useTheme();
  const navigate = useNavigate();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [interviewModalPreset, setInterviewModalPreset] = useState<{
    defaultMode: 'text' | 'voice';
    defaultResumeId?: number;
    title: string;
    subtitle: string;
    startButtonText: string;
  } | null>(null);

  const openInterviewModalWithResume = (resumeId: number) => {
    setInterviewModalPreset({
      defaultMode: 'text',
      defaultResumeId: resumeId,
      title: '开始情景模拟',
      subtitle: '配置模拟参数，开始练习',
      startButtonText: '开始模拟',
    });
  };

  const handleInterviewStart = (config: UnifiedInterviewConfig) => {
    setInterviewModalPreset(null);
    if (config.mode === 'text') {
      navigate('/simulation/session', {
        state: {
          documentId: config.basedOnDocument ? config.resumeId : undefined,
          interviewConfig: {
            simulationDirection: config.simulationDirection,
            skillId: config.skillId,
            difficulty: config.difficulty,
            simulationDifficulty: config.simulationDifficulty,
            questionCount: config.questionCount,
            basedOnDocument: config.basedOnDocument,
            llmProvider: config.llmProvider,
            customCategories: config.customCategories,
            jdText: config.customJdText,
          },
        },
      });
      return;
    }

    const params = new URLSearchParams({
      skillId: config.skillId,
      difficulty: config.difficulty,
    });
    navigate(`/voice?${params.toString()}`, {
      state: {
        voiceConfig: {
          skillId: config.skillId,
          difficulty: config.difficulty,
          techEnabled: true,
            projectEnabled: true,
            hrEnabled: true,
            plannedDuration: config.plannedDuration,
            resumeId: config.basedOnDocument ? config.resumeId : undefined,
            llmProvider: config.llmProvider,
          },
        },
    });
  };

  // 按业务模块组织的导航项
  const navGroups: NavGroup[] = [
    {
      id: 'workspace',
      title: '主工作台',
      items: [
        { id: 'documents', path: '/documents', label: '文档分析', icon: FileSearch, description: '管理文档，AI 分析' },
        { id: 'simulation', path: '/simulation', label: '情景模拟', icon: Zap, description: '多场景交互演练' },
      ],
    },
    {
      id: 'knowledge',
      title: '知识与问答',
      items: [
        { id: 'kb-manage', path: '/knowledgebase', label: '知识库', icon: Library, description: '管理企业知识文档' },
        { id: 'chat', path: '/knowledgebase/chat', label: '问答助手', icon: Bot, description: '基于知识库智能问答' },
      ],
    },
    {
      id: 'tools',
      title: '辅助工具',
      items: [
        { id: 'simulation-history', path: '/simulation/history', label: '练习历史', icon: History, description: '查看往期练习记录' },
        { id: 'schedule', path: '/schedule', label: '日程管理', icon: CalendarDays, description: '管理日程安排' },
        { id: 'voice-simulation', path: '/voice', label: '语音面试', icon: AudioWaveform, description: '实时语音对话模拟' },
        { id: 'ai-config', path: '/ai-config', label: '模型配置', icon: Brain, description: '管理 AI 模型选择' },
        { id: 'about', path: '/about', label: '关于', icon: Info, description: '了解项目初心与定位' },
      ],
    },
  ];

  // 判断当前页面是否匹配导航项
  const isActive = (path: string) => {
    if (path.startsWith('#')) return false;
    if (path === '/documents') {
      return currentPath === '/documents'
        || currentPath === '/'
        || currentPath.startsWith('/documents/')
        || currentPath === '/documents/upload';
    }
    if (path === '/simulation') {
      return currentPath === '/simulation'
        || currentPath === '/simulation/session'
        || currentPath.startsWith('/simulation/session/');
    }
    if (path === '/simulation/history') {
      return currentPath.startsWith('/simulation/history');
    }
    if (path === '/voice') {
      return currentPath.startsWith('/voice');
    }
    if (path === '/ai-config') {
      return currentPath === '/ai-config';
    }
    if (path === '/knowledgebase') {
      return (currentPath === '/knowledgebase' || currentPath === '/knowledgebase/upload') && !currentPath.includes('/chat');
    }
    return currentPath.startsWith(path);
  };

  return (
    <div className="flex min-h-screen bg-[#f8fafc] dark:bg-[#020617] transition-colors duration-500 overflow-hidden">
      {/* 侧边栏 */}
      <aside 
        className={`fixed h-screen left-0 top-0 z-50 flex flex-col bg-white dark:bg-slate-900 border-r border-slate-200/60 dark:border-slate-800/60 transition-all duration-300 ease-in-out shadow-[0_0_20px_rgba(0,0,0,0.03)] dark:shadow-none ${
          isCollapsed ? 'w-20' : 'w-64'
        }`}
      >
        {/* Logo 区 */}
        <div className={`p-6 mb-2 flex items-center justify-between ${isCollapsed ? 'px-4 justify-center' : ''}`}>
          <Link to="/documents" className="flex items-center gap-3.5 group">
            <div className="flex-shrink-0 w-11 h-11 bg-gradient-to-tr from-primary-600 to-primary-400 rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary-500/30 group-hover:scale-105 transition-transform duration-300">
              <Sparkles className="w-6 h-6" />
            </div>
            {!isCollapsed && (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex flex-col"
              >
                <span className="text-xl font-extrabold text-slate-900 dark:text-white tracking-tight leading-none mb-1">Ruici AI</span>
                <span className="text-[10px] text-primary-600 dark:text-primary-400 font-bold tracking-[0.15em] uppercase">Enterprise</span>
              </motion.div>
            )}
          </Link>
        </div>

        {/* 导航 */}
        <nav className="flex-1 px-4 py-2 overflow-y-auto scrollbar-none space-y-8">
          {navGroups.map((group) => (
            <div key={group.id} className="space-y-2">
              {!isCollapsed && (
                <div className="px-3">
                  <span className="text-[11px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest block mb-3">
                    {group.title}
                  </span>
                </div>
              )}
              <div className="space-y-1">
                {group.items.map((item) => {
                  const active = isActive(item.path);

                  return (
                    <Link
                      key={item.id}
                      to={item.path}
                      title={isCollapsed ? item.label : undefined}
                      className={`group flex items-center gap-3.5 px-3 py-3 rounded-xl transition-all duration-300 relative
                        ${active
                          ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400'
                          : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-white'
                        }`}
                    >
                      <div className={`flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300
                        ${active
                          ? 'bg-primary-500 text-white shadow-glow shadow-primary-500/40'
                          : 'bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-500 group-hover:bg-white dark:group-hover:bg-slate-700 group-hover:text-primary-500 dark:group-hover:text-primary-400 group-hover:shadow-sm'
                        }`}
                      >
                        <item.icon className={`w-5 h-5 ${active ? 'animate-pulse' : ''}`} />
                      </div>
                      
                      {!isCollapsed && (
                        <div className="flex-1 min-w-0">
                          <span className={`text-[14px] block transition-colors ${active ? 'font-bold' : 'font-semibold'}`}>
                            {item.label}
                          </span>
                          {item.description && (
                            <span className="text-[10px] text-slate-400 dark:text-slate-500 truncate block mt-0.5 opacity-80 font-medium">
                              {item.description}
                            </span>
                          )}
                        </div>
                      )}

                      {active && (
                        <motion.div 
                          layoutId="active-pill"
                          className="absolute right-2 w-1 h-5 bg-primary-500 rounded-full"
                          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                        />
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* 底部功能区 */}
        <div className="p-4 mt-auto border-t border-slate-100 dark:border-slate-800/50">
          <div className={`flex items-center gap-3 ${isCollapsed ? 'flex-col' : 'justify-between'}`}>
            {!isCollapsed && (
              <div className="flex-1 min-w-0">
                <p className="text-[11px] font-bold text-slate-800 dark:text-white truncate uppercase tracking-tight">System Online</p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                  <span className="text-[10px] text-slate-500 dark:text-slate-500 font-bold uppercase">v1.2.2 Stability</span>
                </div>
              </div>
            )}
            
            <button
              onClick={toggleTheme}
              className="w-11 h-11 rounded-xl bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-700 hover:text-primary-600 dark:hover:text-primary-400 border border-slate-100 dark:border-slate-800/50 transition-all flex items-center justify-center shadow-sm group active:scale-95"
            >
              {theme === 'dark' ? <Sun className="w-5 h-5 group-hover:rotate-45 transition-transform" /> : <Moon className="w-5 h-5 group-hover:-rotate-12 transition-transform" />}
            </button>
          </div>
        </div>

        {/* 折叠按钮 */}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="absolute -right-4 top-1/2 -translate-y-1/2 w-8 h-8 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-full flex items-center justify-center text-slate-400 dark:text-slate-600 shadow-md hover:text-primary-500 transition-colors z-50 group"
        >
          {isCollapsed ? <PanelLeftOpen className="w-4 h-4" /> : <PanelLeftClose className="w-4 h-4" />}
        </button>
      </aside>

      {/* 内容区 */}
      <main className={`flex-1 transition-all duration-300 min-h-screen flex flex-col relative ${isCollapsed ? 'ml-20' : 'ml-64'}`}>
        {/* 顶部背景装饰 */}
        <div className="absolute top-0 left-0 right-0 h-96 bg-gradient-to-b from-primary-50/40 to-transparent dark:from-primary-950/20 pointer-events-none -z-10" />
        
        <div className="flex-1 p-6 lg:p-10 relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentPath}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.3, ease: [0.23, 1, 0.32, 1] }}
              className="h-full"
            >
              <Outlet context={{ openInterviewModalWithResume }} />
            </motion.div>
          </AnimatePresence>
        </div>
      </main>

      {/* 弹窗 */}
      <UnifiedInterviewModal
        isOpen={interviewModalPreset !== null}
        onClose={() => setInterviewModalPreset(null)}
        onStart={handleInterviewStart}
        defaultMode={interviewModalPreset?.defaultMode || 'text'}
        defaultResumeId={interviewModalPreset?.defaultResumeId}
        hideModeSwitch={interviewModalPreset?.defaultResumeId == null}
        title={interviewModalPreset?.title || '开始情景模拟'}
        subtitle={interviewModalPreset?.subtitle || '选择模拟模式和主题，快速开始'}
        startButtonText={interviewModalPreset?.startButtonText || '开始模拟'}
      />
    </div>
  );
}
