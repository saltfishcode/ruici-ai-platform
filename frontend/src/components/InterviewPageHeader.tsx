import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface InterviewPageHeaderProps {
  title: string;
  subtitle: string;
  icon: ReactNode;
}

export default function InterviewPageHeader({
  title,
  subtitle,
  icon,
}: InterviewPageHeaderProps) {
  return (
    <motion.div
      className="text-center mb-8"
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <h1 className="text-3xl font-bold text-primary-800 dark:text-[#f3f4f6] mb-2 flex items-center justify-center gap-3">
        <div className="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center">
          {icon}
        </div>
        {title}
      </h1>
      <p className="text-primary-400 dark:text-[#9ca3af]">{subtitle}</p>
    </motion.div>
  );
}
