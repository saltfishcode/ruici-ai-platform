import { Link } from 'react-router-dom';
import {
  FileSearch,
  Zap,
  Library,
  AudioWaveform,
  CalendarDays,
  ArrowRight,
  Cpu,
  Database,
  Radio,
  Layers,
} from 'lucide-react';

const pillars = [
  {
    icon: FileSearch,
    title: '泛职业文档分析',
    description: '面向不同职业场景，完成文档理解、结构提取、评估建议与报告导出。支持 PDF、DOCX、TXT、Markdown 等多格式。',
    color: 'text-blue-600',
    bg: 'bg-blue-50 dark:bg-blue-950',
  },
  {
    icon: Zap,
    title: '多场景情景模拟',
    description: '以真实业务对话为核心，支持求职面试、专业答疑、职业沟通表达等训练方向。Skill 驱动题目生成与多轮追问。',
    color: 'text-coral-600',
    bg: 'bg-coral-50 dark:bg-coral-950',
  },
  {
    icon: Library,
    title: '知识库驱动能力编排',
    description: '结合知识库、提示词模板和 Skill 机制，快速组合和扩展垂类能力。支持 RAG 检索增强与流式问答。',
    color: 'text-green-600',
    bg: 'bg-green-50 dark:bg-green-950',
  },
];

const techStack = [
  { icon: Cpu, label: 'Java 21 虚拟线程 + Spring Boot 4.0' },
  { icon: Layers, label: 'Spring AI 2.0 + 多 Provider 架构' },
  { icon: Database, label: 'PostgreSQL + pgvector + Redis Stream' },
  { icon: Radio, label: 'WebSocket 实时语音 ASR/TTS' },
];

const modules = [
  { name: 'document', label: '文档分析', icon: FileSearch, path: '/app/documents' },
  { name: 'simulation', label: '情景模拟', icon: Zap, path: '/app/simulation' },
  { name: 'knowledgebase', label: '知识库', icon: Library, path: '/app/knowledgebase' },
  { name: 'schedule', label: '日程管理', icon: CalendarDays, path: '/app/schedule' },
  { name: 'voice', label: '语音交互', icon: AudioWaveform, path: '/app/voice' },
];

export default function AboutPage() {
  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <section>
        <p className="text-xs font-medium tracking-[0.15em] uppercase text-coral-500 mb-3">
          About Ruici AI Platform
        </p>
        <h1 className="text-3xl lg:text-4xl font-bold text-primary-800 dark:text-[#f3f4f6] leading-tight mb-4">
          立项初心
        </h1>
        <p className="text-base leading-7 text-primary-500 dark:text-[#d1d5db] max-w-3xl">
          Ruici-AI-Platform 希望把 AI 能力真正落到业务场景里：不是只做单点问答，而是围绕"文档理解、
          情景训练、知识复用、语音交互"形成完整闭环，帮助不同职业用户持续提升实战能力。
        </p>
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        {pillars.map((item) => (
          <article
            key={item.title}
            className="card p-6 card-hover"
          >
            <div className={`w-11 h-11 rounded-xl ${item.bg} flex items-center justify-center mb-4`}>
              <item.icon className={`w-5 h-5 ${item.color}`} />
            </div>
            <h2 className="text-lg font-bold text-primary-800 dark:text-[#f3f4f6] mb-2">{item.title}</h2>
            <p className="text-sm leading-6 text-primary-500 dark:text-[#d1d5db]">{item.description}</p>
          </article>
        ))}
      </section>

      <section className="rounded-[22px] bg-gradient-to-br from-primary-800 via-primary-900 to-primary-950 p-12 lg:p-16 relative overflow-hidden">
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-0 right-0 w-72 h-72 bg-blue-600/10 rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-coral-500/8 rounded-full blur-3xl" />
        </div>
        <div className="relative">
          <h2 className="text-2xl font-bold text-white mb-8">技术栈</h2>
          <div className="grid gap-6 sm:grid-cols-2">
            {techStack.map((item) => (
              <div key={item.label} className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center flex-shrink-0">
                  <item.icon className="w-5 h-5 text-blue-300" />
                </div>
                <span className="text-sm text-primary-200 font-medium">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section>
        <h2 className="text-xl font-bold text-primary-800 dark:text-[#f3f4f6] mb-5">能力模块</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {modules.map((mod) => (
            <Link
              key={mod.name}
              to={mod.path}
              className="card p-4 card-hover flex items-center gap-3 group"
            >
              <div className="w-9 h-9 rounded-lg bg-stone-100 dark:bg-[#1f2937] flex items-center justify-center group-hover:bg-primary-800 group-hover:text-white dark:group-hover:bg-white dark:group-hover:text-primary-800 transition-colors">
                <mod.icon className="w-4 h-4" />
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-sm font-medium text-primary-700 dark:text-[#e5e7eb] block truncate">{mod.label}</span>
                <span className="text-[10px] text-primary-400 font-mono">{mod.name}</span>
              </div>
              <ArrowRight className="w-3.5 h-3.5 text-primary-200 group-hover:text-primary-500 group-hover:translate-x-0.5 transition-all" />
            </Link>
          ))}
        </div>
      </section>

      <section className="card p-6 lg:p-8">
        <h2 className="text-lg font-bold text-primary-800 dark:text-[#f3f4f6] mb-3">版本信息</h2>
        <div className="flex flex-wrap gap-3 mb-4">
          {['v1.3.1', 'Java 21', 'Spring Boot 4.0', 'Spring AI 2.0', 'React 18', 'TailwindCSS 4'].map(tag => (
            <span key={tag} className="pill text-xs">{tag}</span>
          ))}
        </div>
        <p className="text-sm leading-6 text-primary-500 dark:text-[#d1d5db]">
          平台当前覆盖 document、simulation、knowledgebase、schedule、voice 五大模块，支持从上传分析到
          训练评估、从知识库检索到实时语音交互的端到端体验。采用多 Provider 架构，运行时动态配置，
          模型热切换与 last-known-good 兜底策略。
        </p>
      </section>
    </div>
  );
}
