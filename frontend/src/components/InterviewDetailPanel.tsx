import {useMemo, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {getScoreColor} from '../utils/score';
import type {InterviewDetail} from '../types/simulation';

interface InterviewDetailPanelProps {
  interview: InterviewDetail;
}

/**
 * 面试详情面板组件
 */
export default function InterviewDetailPanel({ interview }: InterviewDetailPanelProps) {
  // 默认展开所有题目
  const [expandedQuestions, setExpandedQuestions] = useState<Set<number>>(() => {
    const allIndices = new Set<number>();
    if (interview.answers) {
      interview.answers.forEach((_, idx) => allIndices.add(idx));
    }
    return allIndices;
  });

  const toggleQuestion = (index: number) => {
    setExpandedQuestions(prev => {
      const newSet = new Set(prev);
      if (newSet.has(index)) {
        newSet.delete(index);
      } else {
        newSet.add(index);
      }
      return newSet;
    });
  };

  // 计算圆环进度
  const { scorePercent, circumference, strokeDashoffset } = useMemo(() => {
    const percent = interview.overallScore !== null ? (interview.overallScore / 100) * 100 : 0;
    const circ = 2 * Math.PI * 54; // r=54
    const offset = circ - (percent / 100) * circ;
    return { scorePercent: percent, circumference: circ, strokeDashoffset: offset };
  }, [interview.overallScore]);

  return (
      <motion.div
      className="space-y-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      {/* 评分卡片 */}
        <ScoreCard
        score={interview.overallScore}
        feedback={interview.overallFeedback}
        scorePercent={scorePercent}
        circumference={circumference}
        strokeDashoffset={strokeDashoffset}
      />

      {/* 表现优势 */}
      {interview.strengths && interview.strengths.length > 0 && (
        <StrengthsSection strengths={interview.strengths} />
      )}

      {/* 改进建议 */}
      {interview.improvements && interview.improvements.length > 0 && (
        <ImprovementsSection improvements={interview.improvements} />
      )}

      {/* 问答记录详情 */}
        <QuestionsSection
        answers={interview.answers || []}
        expandedQuestions={expandedQuestions}
        toggleQuestion={toggleQuestion}
      />
    </motion.div>
  );
}

// 评分卡片组件
function ScoreCard({
  score,
  feedback,
  // scorePercent, // 暂时未使用
  circumference,
  strokeDashoffset
}: {
  score: number | null;
  feedback: string | null;
  scorePercent: number;
  circumference: number;
  strokeDashoffset: number;
}) {
  return (
    <div className="bg-green-700 dark:bg-green-800 rounded-lg p-8 text-white">
      <div className="flex flex-col lg:flex-row lg:items-center gap-8">
        {/* 圆环进度条 */}
        <div className="relative w-28 h-28 mx-auto lg:mx-0 flex-shrink-0">
          <svg className="w-28 h-28 transform -rotate-90" viewBox="0 0 120 120">
            <circle
              cx="60"
              cy="60"
              r="54"
              stroke="rgba(255,255,255,0.15)"
              strokeWidth="6"
              fill="none"
            />
            <motion.circle
              cx="60"
              cy="60"
              r="54"
              stroke="white"
              strokeWidth="6"
              fill="none"
              strokeLinecap="round"
              strokeDasharray={circumference}
              initial={{ strokeDashoffset: circumference }}
              animate={{ strokeDashoffset }}
              transition={{ duration: 1.5, ease: "easeOut" }}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <motion.span
              className="text-3xl font-bold"
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.5 }}
            >
              {score ?? '-'}
            </motion.span>
            <span className="text-xs text-white/60 tracking-wide">总分</span>
          </div>
        </div>

        <div className="text-center lg:text-left flex-1">
          <h3 className="text-xl font-semibold mb-2 tracking-tight">模拟评估</h3>
          <p className="text-white/80 leading-relaxed text-sm">
            {feedback || '表现良好，展示了扎实的技术基础。'}
          </p>
        </div>
      </div>
    </div>
  );
}

// 优势部分组件
function StrengthsSection({ strengths }: { strengths: string[] }) {
  return (
      <motion.div
          className="bg-white dark:bg-[#1a1f2e] rounded-lg border border-stone-200 dark:border-[#2d3548] p-6"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 }}
    >
        <h4 className="text-sm font-medium text-accent-success mb-4 flex items-center gap-2 tracking-wide uppercase">
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          <polyline points="22,4 12,14.01 9,11.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        表现优势
      </h4>
      <ul className="space-y-2.5">
        {strengths.map((s: string, i: number) => (
            <li key={i} className="text-primary-600 dark:text-[#d1d5db] text-sm flex items-start gap-3 leading-relaxed">
            <span className="w-1.5 h-1.5 bg-accent-success rounded-full mt-2 flex-shrink-0"></span>
            <span>{s}</span>
          </li>
        ))}
      </ul>
    </motion.div>
  );
}

// 改进建议部分组件
function ImprovementsSection({ improvements }: { improvements: string[] }) {
  return (
      <motion.div
          className="bg-white dark:bg-[#1a1f2e] rounded-lg border border-stone-200 dark:border-[#2d3548] p-6"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
    >
        <h4 className="text-sm font-medium text-accent-warning mb-4 flex items-center gap-2 tracking-wide uppercase">
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
          <line x1="12" y1="8" x2="12" y2="12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
          <line x1="12" y1="16" x2="12.01" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
        </svg>
        改进建议
      </h4>
      <ul className="space-y-2.5">
        {improvements.map((s: string, i: number) => (
            <li key={i} className="text-primary-600 dark:text-[#d1d5db] text-sm flex items-start gap-3 leading-relaxed">
            <span className="w-1.5 h-1.5 bg-accent-warning rounded-full mt-2 flex-shrink-0"></span>
            <span>{s}</span>
          </li>
        ))}
      </ul>
    </motion.div>
  );
}

// 问答部分组件
function QuestionsSection({
  answers,
  expandedQuestions,
  toggleQuestion
}: {
  answers: any[];
  expandedQuestions: Set<number>;
  toggleQuestion: (index: number) => void;
}) {
  return (
    <div>
      <h4 className="text-sm font-medium text-primary-500 dark:text-[#9ca3af] mb-4 flex items-center gap-2 tracking-wide uppercase">
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none">
          <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        问答记录详情
      </h4>

      <div className="space-y-3">
        {answers.map((answer, idx) => (
          <QuestionCard
            key={idx}
            answer={answer}
            index={idx}
            isExpanded={expandedQuestions.has(idx)}
            onToggle={() => toggleQuestion(idx)}
          />
        ))}
      </div>
    </div>
  );
}

// 问题卡片组件
function QuestionCard({
  answer,
  index,
  isExpanded,
  onToggle
}: {
  answer: any;
  index: number;
  isExpanded: boolean;
  onToggle: () => void;
}) {
  return (
      <motion.div
          className="bg-white dark:bg-[#1a1f2e] rounded-lg border border-stone-200 dark:border-[#2d3548] overflow-hidden"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 + index * 0.03 }}
    >
      {/* 问题头部 */}
        <div
            className="px-5 py-3.5 flex items-center justify-between cursor-pointer hover:bg-stone-50 dark:hover:bg-[#1f2937] transition-colors"
        onClick={onToggle}
      >
        <div className="flex items-center gap-3">
          <span className="w-7 h-7 bg-primary-50 dark:bg-[#0f1117] text-primary-500 dark:text-[#9ca3af] rounded flex items-center justify-center text-xs font-medium">
            {answer.questionIndex + 1}
          </span>
          <span className="px-2.5 py-0.5 bg-stone-100 dark:bg-[#0f1117] text-primary-500 dark:text-[#9ca3af] text-xs font-medium rounded">
            {answer.category || '综合'}
          </span>
          <span className={`text-sm font-medium ${getScoreColor(answer.score, [80, 60])}`}>
            {answer.score} 分
          </span>
        </div>
          <motion.svg
          className="w-4 h-4 text-primary-300 dark:text-[#6b7280]"
          animate={{ rotate: isExpanded ? 180 : 0 }}
          transition={{ duration: 0.2 }}
          viewBox="0 0 24 24"
          fill="none"
        >
          <polyline points="6,9 12,15 18,9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </motion.svg>
      </div>

      {/* 问题内容 */}
      <div className="px-5 pb-3">
        <p className="text-sm text-primary-700 dark:text-[#f3f4f6] leading-relaxed">{answer.question}</p>
      </div>

      {/* 展开内容 */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 space-y-3 border-t border-stone-100 dark:border-[#2d3548] pt-3">
              {/* 你的回答 */}
              <div className="bg-stone-50 dark:bg-[#0f1117] rounded p-4">
                <p className="text-xs text-primary-400 dark:text-[#6b7280] mb-1.5 flex items-center gap-1 font-medium tracking-wide uppercase">
                  <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none">
                    <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  你的回答
                </p>
                <p className={`text-sm leading-relaxed ${
                  !answer.userAnswer || answer.userAnswer === '不知道' 
                    ? 'text-accent-danger font-medium'
                      : 'text-primary-600 dark:text-[#d1d5db]'
                }`}>
                  {answer.userAnswer || '(未回答)'}
                </p>
              </div>

              {/* AI 深度评价 */}
              {answer.feedback && (
                <div className="pl-4 border-l-2 border-primary-200 dark:border-[#2d3548]">
                  <p className="text-xs text-primary-400 dark:text-[#6b7280] mb-1 font-medium tracking-wide uppercase">
                    AI 评价
                  </p>
                  <p className="text-sm text-primary-600 dark:text-[#d1d5db] leading-relaxed">{answer.feedback}</p>
                </div>
              )}

              {/* 参考答案 */}
              {answer.referenceAnswer && (
                  <div className="bg-green-50 dark:bg-green-900/10 rounded p-4 border border-green-100 dark:border-green-900/30">
                    <p className="text-xs text-green-700 dark:text-green-400 mb-1.5 font-medium tracking-wide uppercase">
                    参考答案
                  </p>
                    <div className="text-sm text-primary-600 dark:text-[#d1d5db] leading-relaxed whitespace-pre-line">{answer.referenceAnswer}</div>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
