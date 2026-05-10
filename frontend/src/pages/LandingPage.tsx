import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useEffect } from 'react';
import {
  FileSearch,
  Zap,
  Library,
  AudioWaveform,
  ArrowRight,
  Github,
  Cpu,
  Database,
  Radio,
  Layers,
} from 'lucide-react';

const CAPABILITIES = [
  {
    icon: FileSearch,
    title: '泛职业文档分析',
    description: '支持 PDF、DOCX、DOC、TXT、Markdown 等多格式输入。内容清洗、结构抽取、AI 评估建议、PDF 报告导出。',
    tags: ['多格式解析', 'AI 评分', '报告导出'],
  },
  {
    icon: Zap,
    title: '多场景情景模拟',
    description: '三大默认方向：求职面试、专业答疑、职业沟通表达。Skill 驱动题目生成、多轮追问、评估报告。',
    tags: ['求职面试', '专业答疑', '沟通表达'],
  },
  {
    icon: Library,
    title: '知识库驱动能力编排',
    description: '文档向量化（pgvector）、RAG 检索增强问答、流式回答、会话式聊天，快速组合垂类能力。',
    tags: ['RAG', '向量检索', '流式问答'],
  },
];

const TECH_HIGHLIGHTS = [
  {
    icon: Cpu,
    title: '多 Provider 架构',
    description: 'OpenAI-compatible 中转优先，运行时动态配置，模型热切换与 last-known-good 兜底',
  },
  {
    icon: Radio,
    title: '实时语音交互',
    description: 'WebSocket + Qwen ASR/TTS，低延迟语音对话模拟，会话级 LLM 快照锚定',
  },
  {
    icon: Database,
    title: '异步任务引擎',
    description: 'Redis Stream 模板化生产/消费，4 条管道覆盖文档分析、向量化、模拟评估、语音评估',
  },
  {
    icon: Layers,
    title: '现代 Java 全栈',
    description: 'Java 21 虚拟线程 + Spring Boot 4.0 + Spring AI 2.0，模型感知缓存与统一 Resolver',
  },
];

const TECH_BADGES = [
  'Java 21', 'Spring Boot 4.0', 'Spring AI 2.0', 'React 18',
  'PostgreSQL + pgvector', 'Redis Stream', 'TailwindCSS 4',
];

const floatAnimation = {
  y: [0, -8, 0],
  transition: { duration: 3, repeat: Infinity, ease: 'easeInOut' as const },
};

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.08 } },
};

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.23, 1, 0.32, 1] as [number, number, number, number] } },
};

export default function LandingPage() {
  useEffect(() => {
    const root = document.documentElement;
    const wasDark = root.classList.contains('dark');
    if (wasDark) {
      root.classList.remove('dark');
    }
    return () => {
      if (wasDark) {
        root.classList.add('dark');
      }
    };
  }, []);

  return (
    <div className="min-h-screen bg-white text-primary-800">
      <nav className="fixed top-0 left-0 right-0 z-50 bg-white/90 backdrop-blur-sm border-b border-stone-200/60">
        <div className="max-w-7xl mx-auto px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <motion.div
              animate={floatAnimation}
              className="w-9 h-9 bg-primary-800 rounded-lg flex items-center justify-center"
            >
              <Zap className="w-5 h-5 text-white" />
            </motion.div>
            <span className="font-display font-bold text-lg text-primary-800 tracking-tight">Ruici AI</span>
          </div>
          <div className="flex items-center gap-4">
            <a
              href="https://github.com/saltFishCode/Ruici-AI-Platform"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-ghost text-sm"
            >
              <Github className="w-4 h-4" />
              <span className="hidden sm:inline">GitHub</span>
            </a>
            <Link to="/app" className="btn-primary text-sm">
              进入工作台
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </nav>

      <section className="pt-32 pb-20 lg:pt-44 lg:pb-32 px-6 lg:px-8 relative overflow-hidden">
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-20 left-[10%] w-72 h-72 bg-blue-100/40 rounded-full blur-3xl" />
          <div className="absolute bottom-10 right-[15%] w-64 h-64 bg-coral-100/30 rounded-full blur-3xl" />
        </div>
        <div className="max-w-4xl mx-auto text-center relative">
          <motion.div
            initial="hidden"
            animate="visible"
            variants={staggerContainer}
          >
            <motion.p variants={fadeUp} className="text-sm font-medium text-coral-500 tracking-wide uppercase mb-6">
              Enterprise AI Platform · v1.3.1
            </motion.p>
            <motion.h1 variants={fadeUp} className="text-5xl lg:text-7xl font-extrabold text-primary-800 leading-[0.95] tracking-[-0.03em] mb-6">
              泛职业文档分析
              <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-600 to-primary-400">多场景情景模拟</span>
            </motion.h1>
            <motion.p variants={fadeUp} className="text-lg lg:text-xl text-primary-500 leading-relaxed max-w-2xl mx-auto mb-10">
              基于大语言模型、知识库与实时语音能力构建。围绕文档理解、情景训练、知识复用、语音交互形成完整闭环，帮助职业者持续提升实战能力。
            </motion.p>
            <motion.div variants={fadeUp} className="flex items-center justify-center gap-4 flex-wrap">
              <Link to="/app" className="btn-primary text-base px-8 py-3.5 group">
                进入工作台
                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
              </Link>
              <a href="#capabilities" className="btn-secondary text-base px-8 py-3.5">
                了解更多
              </a>
            </motion.div>
          </motion.div>
        </div>
      </section>

      <section className="pb-20 px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="max-w-4xl mx-auto flex flex-wrap items-center justify-center gap-3"
        >
          {TECH_BADGES.map((badge, i) => (
            <motion.span
              key={badge}
              initial={{ opacity: 0, scale: 0.8 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.05 }}
              whileHover={{ scale: 1.05, y: -2 }}
              className="pill text-xs cursor-default"
            >
              {badge}
            </motion.span>
          ))}
        </motion.div>
      </section>

      <section id="capabilities" className="py-20 lg:py-32 px-6 lg:px-8 bg-stone-50">
        <div className="max-w-6xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl lg:text-4xl font-bold text-primary-800 mb-4">核心能力</h2>
            <p className="text-primary-500 text-lg">三大支柱，覆盖从文档分析到实战训练的完整链路</p>
          </motion.div>
          <div className="grid gap-6 lg:grid-cols-3">
            {CAPABILITIES.map((cap, i) => (
              <motion.article
                key={cap.title}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.12, duration: 0.5 }}
                whileHover={{ y: -4, transition: { duration: 0.2 } }}
                className="card card-hover p-8 group"
              >
                <motion.div
                  animate={floatAnimation}
                  className="w-12 h-12 rounded-xl bg-stone-100 group-hover:bg-primary-800 flex items-center justify-center mb-5 transition-colors duration-300"
                >
                  <cap.icon className="w-6 h-6 text-primary-700 group-hover:text-white transition-colors duration-300" />
                </motion.div>
                <h3 className="text-xl font-bold text-primary-800 mb-3">{cap.title}</h3>
                <p className="text-sm text-primary-500 leading-relaxed mb-5">{cap.description}</p>
                <div className="flex flex-wrap gap-2">
                  {cap.tags.map(tag => (
                    <span key={tag} className="badge badge-blue text-[11px]">{tag}</span>
                  ))}
                </div>
              </motion.article>
            ))}
          </div>
        </div>
      </section>

      <section className="py-20 lg:py-32 px-6 lg:px-8">
        <div className="max-w-6xl mx-auto rounded-[22px] bg-gradient-to-br from-primary-800 via-primary-900 to-primary-950 p-12 lg:p-20 relative overflow-hidden">
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl" />
            <div className="absolute bottom-0 left-0 w-80 h-80 bg-coral-500/8 rounded-full blur-3xl" />
          </div>
          <div className="relative">
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-center mb-14"
            >
              <h2 className="text-3xl lg:text-4xl font-bold text-white mb-4">技术亮点</h2>
              <p className="text-primary-300 text-lg">现代架构，为企业级 AI 应用而设计</p>
            </motion.div>
            <div className="grid gap-8 sm:grid-cols-2">
              {TECH_HIGHLIGHTS.map((item, i) => (
                <motion.div
                  key={item.title}
                  initial={{ opacity: 0, x: i % 2 === 0 ? -16 : 16 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1, duration: 0.5 }}
                  whileHover={{ x: 4, transition: { duration: 0.2 } }}
                  className="flex gap-4 group"
                >
                  <div className="w-10 h-10 rounded-lg bg-white/10 group-hover:bg-white/20 flex items-center justify-center flex-shrink-0 transition-colors">
                    <item.icon className="w-5 h-5 text-blue-300" />
                  </div>
                  <div>
                    <h4 className="font-semibold text-white mb-1.5">{item.title}</h4>
                    <p className="text-sm text-primary-300 leading-relaxed">{item.description}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="py-20 lg:py-32 px-6 lg:px-8 bg-stone-50 relative overflow-hidden">
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-coral-100/20 rounded-full blur-3xl" />
        </div>
        <div className="max-w-4xl mx-auto text-center relative">
          <motion.div
            animate={floatAnimation}
            className="w-16 h-16 rounded-2xl bg-coral-50 border border-coral-100 flex items-center justify-center mx-auto mb-6"
          >
            <AudioWaveform className="w-8 h-8 text-coral-500" />
          </motion.div>
          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl lg:text-4xl font-bold text-primary-800 mb-4"
          >
            实时语音交互
          </motion.h2>
          <p className="text-primary-500 text-lg leading-relaxed max-w-2xl mx-auto mb-8">
            WebSocket 实时链路，Qwen ASR 语音识别 + LLM 推理 + TTS 语音合成，端到端低延迟。
            适合面试模拟、口语问答、角色扮演等需要即时反馈的场景。
          </p>
          <Link to="/app/voice" className="btn-coral group">
            体验语音模拟
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>
      </section>

      <section className="py-20 lg:py-32 px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <h2 className="text-3xl lg:text-4xl font-bold text-primary-800 mb-4">关于项目</h2>
          </motion.div>
          <div className="grid gap-6 sm:grid-cols-2">
            <motion.div
              initial={{ opacity: 0, x: -16 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              whileHover={{ y: -3, transition: { duration: 0.2 } }}
              className="card p-8"
            >
              <h3 className="text-lg font-bold text-primary-800 mb-3">立项初心</h3>
              <p className="text-sm text-primary-500 leading-relaxed">
                把 AI 能力真正落到业务场景里：不是只做单点问答，而是围绕"文档理解、情景训练、知识复用、语音交互"形成完整闭环，帮助不同职业用户持续提升实战能力。
              </p>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 16 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              whileHover={{ y: -3, transition: { duration: 0.2 } }}
              className="card p-8"
            >
              <h3 className="text-lg font-bold text-primary-800 mb-3">默认模拟方向</h3>
              <ul className="space-y-2.5 text-sm text-primary-500">
                <li className="flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-coral-400 mt-1.5 flex-shrink-0" />
                  <span><strong className="text-primary-700">求职面试</strong> — 面试官角度，模拟问答与文档评估</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-400 mt-1.5 flex-shrink-0" />
                  <span><strong className="text-primary-700">专业答疑</strong> — 提问者角度，专业问答与场景化解释</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-400 mt-1.5 flex-shrink-0" />
                  <span><strong className="text-primary-700">职业沟通表达</strong> — 汇报、邮件、会议发言与反馈沟通</span>
                </li>
              </ul>
            </motion.div>
          </div>
        </div>
      </section>

      <footer className="bg-primary-800 text-white py-12 px-6 lg:px-8">
        <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-white/10 rounded-lg flex items-center justify-center">
              <Zap className="w-4 h-4 text-white" />
            </div>
            <span className="font-display font-bold text-white">Ruici AI Platform</span>
          </div>
          <div className="flex items-center gap-6 text-sm text-primary-300">
            <Link to="/app" className="hover:text-white transition-colors">工作台</Link>
            <Link to="/app/about" className="hover:text-white transition-colors">关于</Link>
            <a href="https://github.com/saltFishCode/Ruici-AI-Platform" target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors">
              GitHub
            </a>
          </div>
          <p className="text-xs text-primary-400">
            © 2024-2026 Ruici AI. Built with Spring AI & React.
          </p>
        </div>
      </footer>
    </div>
  );
}
