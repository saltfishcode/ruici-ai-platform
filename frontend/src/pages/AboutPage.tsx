const pillars = [
  {
    title: '泛职业文档分析',
    description: '面向不同职业场景，完成文档理解、结构提取、评估建议与报告导出。',
  },
  {
    title: '多场景情景模拟',
    description: '以真实业务对话为核心，支持求职面试、专业答疑、职业沟通表达等训练方向。',
  },
  {
    title: '知识库驱动能力编排',
    description: '结合知识库、提示词模板和 Skill 机制，快速组合和扩展垂类能力。',
  },
];

export default function AboutPage() {
  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <section className="rounded-2xl border border-slate-200/70 dark:border-slate-700 bg-white/90 dark:bg-slate-900/70 p-6 lg:p-8 shadow-sm">
        <p className="text-xs font-semibold tracking-[0.18em] uppercase text-primary-600 dark:text-primary-400 mb-3">
          About Ruici AI Platform
        </p>
        <h1 className="text-2xl lg:text-3xl font-extrabold text-slate-900 dark:text-white leading-tight">
          我们的立项初心
        </h1>
        <p className="mt-4 text-sm lg:text-base leading-7 text-slate-600 dark:text-slate-300">
          Ruici-AI-Platform 希望把 AI 能力真正落到业务场景里：不是只做单点问答，而是围绕“文档理解、
          情景训练、知识复用、语音交互”形成完整闭环，帮助不同职业用户持续提升实战能力。
        </p>
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        {pillars.map((item) => (
          <article
            key={item.title}
            className="rounded-2xl border border-slate-200/70 dark:border-slate-700 bg-white/90 dark:bg-slate-900/70 p-5 shadow-sm"
          >
            <h2 className="text-base font-bold text-slate-900 dark:text-white">{item.title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{item.description}</p>
          </article>
        ))}
      </section>

      <section className="rounded-2xl border border-slate-200/70 dark:border-slate-700 bg-white/90 dark:bg-slate-900/70 p-6 shadow-sm">
        <h2 className="text-base font-bold text-slate-900 dark:text-white">当前能力版图</h2>
        <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
          平台当前覆盖 document、simulation、knowledgebase、schedule、voice 五大模块，支持从上传分析到
          训练评估、从知识库检索到实时语音交互的端到端体验。
        </p>
      </section>
    </div>
  );
}
