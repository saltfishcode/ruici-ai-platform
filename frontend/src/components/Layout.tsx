import {Link, Outlet, useLocation, useNavigate} from 'react-router-dom';
import {AnimatePresence, motion} from 'framer-motion';
import {
  AudioWaveform,
  Bot,
  CalendarDays,
  FileSearch,
  History,
  Home,
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
      navigate('/app/simulation/session', {
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
    navigate(`/app/voice?${params.toString()}`, {
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

  const navGroups: NavGroup[] = [
    {
      id: 'workspace',
      title: '主工作台',
      items: [
        { id: 'home', path: '/', label: '返回首页', icon: Home, description: '项目介绍与入口' },
        { id: 'documents', path: '/app/documents', label: '文档分析', icon: FileSearch, description: '管理文档，AI 分析' },
        { id: 'simulation', path: '/app/simulation', label: '情景模拟', icon: Zap, description: '多场景交互演练' },
      ],
    },
    {
      id: 'knowledge',
      title: '知识与问答',
      items: [
        { id: 'kb-manage', path: '/app/knowledgebase', label: '知识库', icon: Library, description: '管理企业知识文档' },
        { id: 'chat', path: '/app/knowledgebase/chat', label: '问答助手', icon: Bot, description: '基于知识库智能问答' },
      ],
    },
    {
      id: 'tools',
      title: '辅助工具',
      items: [
        { id: 'simulation-history', path: '/app/simulation/history', label: '练习历史', icon: History, description: '查看往期练习记录' },
        { id: 'schedule', path: '/app/schedule', label: '日程管理', icon: CalendarDays, description: '管理日程安排' },
        { id: 'voice-simulation', path: '/app/voice', label: '语音面试', icon: AudioWaveform, description: '实时语音对话模拟' },
        { id: 'ai-config', path: '/app/ai-config', label: '模型配置', icon: Brain, description: '管理 AI 模型选择' },
        { id: 'about', path: '/app/about', label: '关于', icon: Info, description: '了解项目初心与定位' },
      ],
    },
  ];

  const isActive = (path: string) => {
    if (path === '/') return false;
    if (path.startsWith('#')) return false;
    if (path === '/app/documents') {
      return currentPath === '/app/documents'
        || currentPath === '/app'
        || currentPath.startsWith('/app/documents/')
        || currentPath === '/app/documents/upload';
    }
    if (path === '/app/simulation') {
      return currentPath === '/app/simulation'
        || currentPath === '/app/simulation/session'
        || currentPath.startsWith('/app/simulation/session/');
    }
    if (path === '/app/simulation/history') {
      return currentPath.startsWith('/app/simulation/history');
    }
    if (path === '/app/voice') {
      return currentPath.startsWith('/app/voice');
    }
    if (path === '/app/ai-config') {
      return currentPath === '/app/ai-config';
    }
    if (path === '/app/knowledgebase') {
      return (currentPath === '/app/knowledgebase' || currentPath === '/app/knowledgebase/upload') && !currentPath.includes('/chat');
    }
    return currentPath.startsWith(path);
  };

  return (
    <div className="flex min-h-screen bg-stone-50 dark:bg-[#0f1117] transition-colors duration-300 overflow-hidden">
      {/* Sidebar */}
      <aside 
        className={`fixed h-screen left-0 top-0 z-50 flex flex-col bg-white dark:bg-[#151b2b] border-r border-stone-200 dark:border-[#2d3548] transition-all duration-300 ease-in-out ${
          isCollapsed ? 'w-20' : 'w-64'
        }`}
      >
        {/* Logo 区 */}
        <div className={`p-6 mb-2 flex items-center justify-between ${isCollapsed ? 'px-4 justify-center' : ''}`}>
          <Link to="/app/documents" className="flex items-center gap-3.5 group">
            <div className="w-10 h-10 bg-primary-800 dark:bg-stone-100 rounded-lg flex items-center justify-center text-white dark:text-[#f3f4f6] group-hover:scale-105 transition-transform duration-200">
              <Sparkles className="w-5 h-5" />
            </div>
            {!isCollapsed && (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex flex-col"
              >
                <span className="text-lg font-bold text-primary-800 dark:text-[#f3f4f6] tracking-tight leading-none mb-0.5">Ruici AI</span>
                <span className="text-[10px] text-primary-400 dark:text-[#9ca3af] font-medium tracking-[0.12em] uppercase">Enterprise</span>
              </motion.div>
            )}
          </Link>
        </div>

        {/* 导航 */}
        <nav className="flex-1 px-4 py-2 overflow-y-auto scrollbar-thin space-y-8">
          {navGroups.map((group) => (
            <div key={group.id} className="space-y-1.5">
              {!isCollapsed && (
                <div className="px-3">
                  <span className="text-[11px] font-semibold text-primary-400 dark:text-[#6b7280] uppercase tracking-widest block mb-2.5">
                    {group.title}
                  </span>
                </div>
              )}
              <div className="space-y-0.5">
                {group.items.map((item) => {
                  const active = isActive(item.path);

                  return (
                    <Link
                      key={item.id}
                      to={item.path}
                      title={isCollapsed ? item.label : undefined}
                      className={`group flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 relative
                        ${active
                          ? 'bg-stone-100 dark:bg-[#1f2937] text-primary-800 dark:text-coral-400'
                          : 'text-primary-500 dark:text-[#9ca3af] hover:bg-stone-50 dark:hover:bg-[#1f2937] hover:text-primary-800 dark:hover:text-[#e5e7eb]'
                        }`}
                    >
                      <div className={`flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center transition-all duration-200
                        ${active
                          ? 'bg-primary-800 dark:bg-coral-500/15 text-white dark:text-coral-400'
                          : 'bg-stone-100 dark:bg-[#1f2937] text-primary-400 dark:text-[#6b7280] group-hover:text-primary-700 dark:group-hover:text-[#d1d5db]'
                        }`}
                      >
                        <item.icon className="w-[18px] h-[18px]" />
                      </div>
                      
                      {!isCollapsed && (
                        <div className="flex-1 min-w-0">
                          <span className={`text-[13px] block transition-colors ${active ? 'font-semibold' : 'font-medium'}`}>
                            {item.label}
                          </span>
                          {item.description && (
                            <span className="text-[10px] text-primary-400 dark:text-[#6b7280] truncate block mt-0.5">
                              {item.description}
                            </span>
                          )}
                        </div>
                      )}

                      {active && (
                        <motion.div 
                          layoutId="active-pill"
                          className="absolute right-2 w-1 h-4 bg-primary-800 dark:bg-coral-400 rounded-full"
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
        <div className="p-4 mt-auto border-t border-stone-100 dark:border-[#2d3548]">
          <div className={`flex items-center gap-3 ${isCollapsed ? 'flex-col' : 'justify-between'}`}>
            {!isCollapsed && (
              <div className="flex-1 min-w-0">
                <p className="text-[11px] font-semibold text-primary-700 dark:text-[#d1d5db] truncate uppercase tracking-tight">System Online</p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <div className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                  <span className="text-[10px] text-primary-400 dark:text-[#6b7280] font-medium uppercase">v1.3.1</span>
                </div>
              </div>
            )}
            
            <button
              onClick={toggleTheme}
              className="w-10 h-10 rounded-lg bg-stone-50 dark:bg-[#1f2937] text-primary-500 dark:text-[#9ca3af] hover:bg-stone-100 dark:hover:bg-[#374151] hover:text-primary-800 dark:hover:text-[#f3f4f6] border border-stone-200 dark:border-[#2d3548] transition-all flex items-center justify-center active:scale-95"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="absolute -right-3.5 top-1/2 -translate-y-1/2 w-7 h-7 bg-white dark:bg-[#1a1f2e] border border-stone-200 dark:border-[#2d3548] rounded-full flex items-center justify-center text-primary-400 dark:text-[#6b7280] hover:text-primary-800 dark:hover:text-[#f3f4f6] shadow-soft transition-colors z-50"
        >
          {isCollapsed ? <PanelLeftOpen className="w-3.5 h-3.5" /> : <PanelLeftClose className="w-3.5 h-3.5" />}
        </button>
      </aside>

      <main className={`flex-1 transition-all duration-300 min-h-screen flex flex-col relative ${isCollapsed ? 'ml-20' : 'ml-64'}`}>
        <div className="flex-1 p-6 lg:p-10 relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentPath}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.25, ease: [0.23, 1, 0.32, 1] }}
              className="h-full"
            >
              <Outlet context={{ openInterviewModalWithResume }} />
            </motion.div>
          </AnimatePresence>
        </div>
      </main>

      {/* Modal */}
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
