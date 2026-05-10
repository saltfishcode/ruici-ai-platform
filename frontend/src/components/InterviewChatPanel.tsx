import {useMemo, useRef} from 'react';
import {motion} from 'framer-motion';
import {Virtuoso, type VirtuosoHandle} from 'react-virtuoso';
import type {InterviewQuestion, InterviewSession} from '../types/simulation';
import {Send} from 'lucide-react';
import InterviewMessageBubble from './InterviewMessageBubble';
import { getSimulationRoleLabel } from '../utils/simulation';

interface Message {
  type: 'interviewer' | 'user';
  content: string;
  category?: string;
  questionIndex?: number;
}

interface InterviewChatPanelProps {
  session: InterviewSession;
  currentQuestion: InterviewQuestion | null;
  messages: Message[];
  answer: string;
  onAnswerChange: (answer: string) => void;
  onSubmit: () => void;
  onCompleteEarly: () => void;
  isSubmitting: boolean;
  showCompleteConfirm: boolean;
  onShowCompleteConfirm: (show: boolean) => void;
}

/**
 * 面试聊天面板组件
 */
export default function InterviewChatPanel({
  session,
  currentQuestion,
  messages,
  answer,
  onAnswerChange,
  onSubmit,
  // onCompleteEarly, // 暂时未使用
  isSubmitting,
  // showCompleteConfirm, // 暂时未使用
  onShowCompleteConfirm
}: InterviewChatPanelProps) {
  const virtuosoRef = useRef<VirtuosoHandle>(null);

  const currentMainQuestionNumber = useMemo(() => {
    if (!session || !currentQuestion) return 0;
    return session.questions
      .slice(0, currentQuestion.questionIndex + 1)
      .filter(question => !question.isFollowUp)
      .length;
  }, [session, currentQuestion]);

  const progress = useMemo(() => {
    if (!session || session.totalQuestions <= 0) return 0;
    return (Math.min(currentMainQuestionNumber, session.totalQuestions) / session.totalQuestions) * 100;
  }, [currentMainQuestionNumber, session]);

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      onSubmit();
    }
  };

  const aiRoleLabel = getSimulationRoleLabel(session.simulationDirection);

  return (
    <div className="flex flex-col h-[calc(100vh-200px)] max-w-4xl mx-auto">
      {/* 进度条 */}
        <div
            className="bg-white dark:bg-[#1f2937] rounded-2xl p-6 mb-4 shadow-sm dark:shadow-primary-900/50 border border-stone-100 dark:border-[#2d3548]">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-semibold text-primary-600 dark:text-[#d1d5db]">
            题目 {currentMainQuestionNumber} / {session.totalQuestions}
          </span>
            <span className="text-sm text-primary-400 dark:text-[#9ca3af]">
            {Math.round(progress)}%
          </span>
        </div>
            <div className="h-2 bg-stone-200 dark:bg-[#374151] rounded-full overflow-hidden">
          <motion.div
            className="h-full bg-gradient-to-r from-primary-500 to-primary-600 rounded-full"
            initial={{ width: 0 }}
            animate={{ width: `${progress}%` }}
            transition={{ duration: 0.3 }}
          />
        </div>
      </div>

      {/* 聊天区域 */}
        <div
            className="flex-1 bg-white dark:bg-[#1f2937] rounded-2xl shadow-sm dark:shadow-primary-900/50 overflow-hidden flex flex-col min-h-0 border border-stone-100 dark:border-[#2d3548]">
        <Virtuoso
          ref={virtuosoRef}
          data={messages}
          initialTopMostItemIndex={messages.length - 1}
          followOutput="smooth"
          className="flex-1"
          itemContent={(_index, msg) => (
            <div className="pb-4 px-6 first:pt-6">
              <InterviewMessageBubble
                role={msg.type === 'interviewer' ? 'interviewer' : 'user'}
                text={msg.content}
                roleLabel={msg.type === 'interviewer' ? aiRoleLabel : undefined}
                category={msg.category}
              />
            </div>
          )}
        />

        {/* 输入区域 */}
            <div className="border-t border-stone-200 dark:border-[#4b5563] p-4 bg-stone-50 dark:bg-[#374151]/50">
          <div className="flex gap-3">
            <textarea
              value={answer}
              onChange={(e) => onAnswerChange(e.target.value)}
              onKeyDown={handleKeyPress}
              placeholder="输入你的回答... (Ctrl/Cmd + Enter 提交)"
              className="flex-1 px-4 py-3 border border-stone-300 dark:border-[#4b5563] rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none bg-white dark:bg-[#1f2937] text-primary-800 dark:text-[#f3f4f6] placeholder-primary-300 dark:placeholder-primary-400"
              rows={3}
              disabled={isSubmitting}
            />
            <div className="flex flex-col gap-2">
              <motion.button
                onClick={onSubmit}
                disabled={!answer.trim() || isSubmitting}
                className="px-6 py-3 bg-primary-800 text-white rounded-xl font-medium hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                whileHover={{ scale: isSubmitting || !answer.trim() ? 1 : 1.02 }}
                whileTap={{ scale: isSubmitting || !answer.trim() ? 1 : 0.98 }}
              >
                {isSubmitting ? (
                  <>
                    <motion.div
                      className="w-4 h-4 border-2 border-white border-t-transparent rounded-full"
                      animate={{ rotate: 360 }}
                      transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                    />
                    提交中
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    提交
                  </>
                )}
              </motion.button>
              <motion.button
                onClick={() => onShowCompleteConfirm(true)}
                disabled={isSubmitting}
                className="px-6 py-3 bg-stone-200 dark:bg-[#374151] text-primary-600 dark:text-[#e5e7eb] rounded-xl font-medium hover:bg-stone-300 dark:hover:bg-stone-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                whileHover={{ scale: isSubmitting ? 1 : 1.02 }}
                whileTap={{ scale: isSubmitting ? 1 : 0.98 }}
              >
                提前交卷
              </motion.button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
