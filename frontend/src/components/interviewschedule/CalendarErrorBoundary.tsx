// frontend/src/components/interviewschedule/CalendarErrorBoundary.tsx

import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class CalendarErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Calendar Error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="bg-white dark:bg-[#1a1f2e]/50 backdrop-blur-xl rounded-2xl border border-stone-200/50 dark:border-[#2d3548]/50 p-6 shadow-xl">
          <div className="text-center py-12">
            <div className="text-red-500 text-6xl mb-4">📅</div>
            <h3 className="text-xl font-semibold text-primary-800 dark:text-[#f3f4f6] mb-2">
              日历渲染出错
            </h3>
            <p className="text-primary-500 dark:text-[#9ca3af] mb-4">
              {this.state.error?.message || '未知错误'}
            </p>
            <button
              onClick={() => {
                this.setState({ hasError: false, error: null });
                window.location.reload();
              }}
              className="px-4 py-2 bg-primary-800 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              刷新页面
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
