// frontend/src/components/interviewschedule/ScheduleHeader.tsx

import React from 'react';
import { motion } from 'framer-motion';
import { Plus, ChevronLeft, ChevronRight, Calendar, List, LayoutGrid } from 'lucide-react';
import dayjs from 'dayjs';

interface ScheduleHeaderProps {
  view: 'day' | 'week' | 'month' | 'list';
  onViewChange: (view: 'day' | 'week' | 'month' | 'list') => void;
  date: Date;
  onDateChange: (date: Date) => void;
  onAddClick: () => void;
}

export const ScheduleHeader: React.FC<ScheduleHeaderProps> = ({
  view,
  onViewChange,
  date,
  onDateChange,
  onAddClick,
}) => {
  const handlePrevious = () => {
    const newDate = new Date(date);
    if (view === 'day') {
      newDate.setDate(newDate.getDate() - 1);
    } else if (view === 'week') {
      newDate.setDate(newDate.getDate() - 7);
    } else if (view === 'month') {
      newDate.setMonth(newDate.getMonth() - 1);
    }
    onDateChange(newDate);
  };

  const handleNext = () => {
    const newDate = new Date(date);
    if (view === 'day') {
      newDate.setDate(newDate.getDate() + 1);
    } else if (view === 'week') {
      newDate.setDate(newDate.getDate() + 7);
    } else if (view === 'month') {
      newDate.setMonth(newDate.getMonth() + 1);
    }
    onDateChange(newDate);
  };

  const handleToday = () => {
    onDateChange(new Date());
  };

  const getTitle = () => {
    if (view === 'list') {
      return '面试列表';
    }
    return dayjs(date).format(view === 'month' ? 'YYYY年MM月' : 'YYYY年MM月DD日');
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="bg-white/80 dark:bg-[#1a1f2e]/80 backdrop-blur-xl rounded-2xl border border-stone-200/50 dark:border-[#2d3548]/50 p-6 mb-6 shadow-xl shadow-stone-200/50 dark:shadow-primary-900/50"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-6">
          <motion.h2
            key={getTitle()}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            className="text-2xl font-display font-bold text-primary-800 dark:text-[#f3f4f6] tracking-tight"
          >
            {getTitle()}
          </motion.h2>

          {view !== 'list' && (
            <div className="flex items-center gap-2">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handlePrevious}
                className="p-2.5 rounded-xl text-primary-500 dark:text-[#d1d5db] hover:bg-stone-100 dark:hover:bg-[#374151] transition-colors"
                title="上一页"
              >
                <ChevronLeft className="w-5 h-5" />
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleToday}
                className="px-4 py-2 text-sm font-medium rounded-xl bg-primary-100/80 dark:bg-primary-500/20 text-primary-700 dark:text-[#9ca3af] hover:bg-primary-200/90 dark:hover:bg-[#4b5563]/30 border border-primary-200/50 dark:border-[#4b5563]/30 backdrop-blur-sm transition-all"
              >
                今天
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleNext}
                className="p-2.5 rounded-xl text-primary-500 dark:text-[#d1d5db] hover:bg-stone-100 dark:hover:bg-[#374151] transition-colors"
                title="下一页"
              >
                <ChevronRight className="w-5 h-5" />
              </motion.button>
            </div>
          )}
        </div>

        <div className="flex items-center gap-3">
          <div className="flex bg-stone-100/80 dark:bg-[#1f2937]/80 backdrop-blur-sm rounded-xl p-1.5 gap-1">
            {[
              { key: 'day', icon: Calendar, label: '日视图' },
              { key: 'week', icon: Calendar, label: '周视图' },
              { key: 'month', icon: LayoutGrid, label: '月视图' },
              { key: 'list', icon: List, label: '列表' },
            ].map(({ key, icon: Icon, label }) => (
              <motion.button
                key={key}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => onViewChange(key as any)}
                className={`px-4 py-2 rounded-lg flex items-center gap-2 font-medium text-sm transition-all ${
                  view === key
                    ? 'bg-white/95 dark:bg-[#374151]/80 backdrop-blur-sm shadow-md text-primary-700 dark:text-[#6b7280] border border-stone-200/50 dark:border-[#4b5563]/50'
                    : 'text-primary-500 dark:text-[#d1d5db] hover:text-primary-800 dark:hover:text-[#f3f4f6] hover:bg-stone-100/50 dark:hover:bg-[#374151]/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                {label}
              </motion.button>
            ))}
          </div>

          <motion.button
            whileHover={{ scale: 1.05, y: -1 }}
            whileTap={{ scale: 0.95 }}
            onClick={onAddClick}
            className="px-5 py-2.5 bg-primary-800 dark:bg-[#1f2937] text-white dark:text-[#f3f4f6] rounded-[32px] font-medium shadow-soft hover:shadow-medium hover:-translate-y-0.5 flex items-center gap-2 transition-all"
          >
            <Plus className="w-4 h-4" />
            添加面试
          </motion.button>
        </div>
      </div>
    </motion.div>
  );
};
