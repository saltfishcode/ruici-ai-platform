import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import Layout from './components/Layout';
import { useEffect, useState, Suspense, lazy } from 'react';
import { documentApi } from './api/document';
import { simulationApi } from './api/simulation';
import type { UploadKnowledgeBaseResponse } from './api/knowledgebase';
import type { Difficulty } from './components/UnifiedInterviewModal';
import type { CategoryDTO } from './api/skill';
import type { InterviewDetail } from './types/simulation';
import type { SimulationDirection } from './hooks/useInterviewConfig';
import { Loader2 } from 'lucide-react';
import { ROUTES } from './constants/routes';

const LandingPage = lazy(() => import('./pages/LandingPage'));
const UploadPage = lazy(() => import('./pages/UploadPage'));
const DocumentListPage = lazy(() => import('./pages/HistoryPage'));
const DocumentDetailPage = lazy(() => import('./pages/ResumeDetailPage'));
const SimulationSession = lazy(() => import('./pages/InterviewPage'));
const SimulationHistoryPage = lazy(() => import('./pages/InterviewHistoryPage'));
const KnowledgeBaseQueryPage = lazy(() => import('./pages/KnowledgeBaseQueryPage'));
const KnowledgeBaseUploadPage = lazy(() => import('./pages/KnowledgeBaseUploadPage'));
const KnowledgeBaseManagePage = lazy(() => import('./pages/KnowledgeBaseManagePage'));
const VoiceSimulationPage = lazy(() => import('./pages/VoiceInterviewPage'));
const VoiceEvaluationPage = lazy(() => import('./pages/VoiceInterviewEvaluationPage'));
const SchedulePage = lazy(() => import('./pages/InterviewSchedulePage'));
const SimulationHubPage = lazy(() => import('./pages/InterviewHubPage'));
const SimulationDetailPanel = lazy(() => import('./components/InterviewDetailPanel'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const AiRuntimeConfigPage = lazy(() => import('./pages/AiRuntimeConfigPage'));

const Loading = () => (
  <div className="flex items-center justify-center min-h-[50vh]">
    <div className="w-10 h-10 border-3 border-stone-200 border-t-primary-800 rounded-full animate-spin" />
  </div>
);

function DocumentUploadWrapper() {
  const navigate = useNavigate();
  const handleUploadComplete = (documentId: number) => {
    navigate(ROUTES.documents, { state: { newDocumentId: documentId } });
  };
  return <UploadPage onUploadComplete={handleUploadComplete} />;
}

function DocumentListWrapper() {
  const navigate = useNavigate();
  const handleSelectDocument = (id: number) => {
    navigate(ROUTES.documentDetail(id));
  };
  return <DocumentListPage onSelectResume={handleSelectDocument} />;
}

function DocumentDetailWrapper() {
  const { documentId } = useParams<{ documentId: string }>();
  const navigate = useNavigate();
  const { openInterviewModalWithResume } = useOutletContext<{ openInterviewModalWithResume: (resumeId: number) => void }>();

  if (!documentId) {
    return <Navigate to={ROUTES.documents} replace />;
  }

  const handleBack = () => {
    navigate(ROUTES.documents);
  };

  const handleStartSimulation = (id: number) => {
    openInterviewModalWithResume(id);
  };

  return (
    <DocumentDetailPage
      resumeId={parseInt(documentId, 10)}
      onBack={handleBack}
      onStartInterview={handleStartSimulation}
    />
  );
}

interface SimulationEntryState {
  documentId?: number;
  resumeId?: number;
  resumeText?: string;
  sessionIdToResume?: string;
  interviewConfig?: {
    simulationDirection?: SimulationDirection;
    skillId?: string;
    difficulty?: Difficulty;
    simulationDifficulty?: 'EASY' | 'NORMAL' | 'SHARP';
    questionCount?: number;
    basedOnDocument?: boolean;
    llmProvider?: string;
    customCategories?: CategoryDTO[];
    jdText?: string;
  };
}

function SimulationSessionWrapper() {
  const { documentId } = useParams<{ documentId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const entryState = (location.state as SimulationEntryState | undefined) ?? {};
  const [resumeText, setResumeText] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const effectiveDocumentId = documentId
    ? parseInt(documentId, 10)
    : (entryState.documentId ?? entryState.resumeId);

  useEffect(() => {
    const stateText = entryState.resumeText;
    if (stateText) {
      setResumeText(stateText);
      setLoading(false);
    } else if (effectiveDocumentId) {
      documentApi.getDocumentDetail(effectiveDocumentId)
        .then(resume => {
          setResumeText(resume.resumeText);
          setLoading(false);
        })
        .catch(err => {
          console.error('获取简历文本失败', err);
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [effectiveDocumentId, entryState.resumeText]);

  const handleBack = () => {
    if (effectiveDocumentId) {
      navigate(ROUTES.documentDetail(effectiveDocumentId), { replace: false });
      return;
    }
    navigate(ROUTES.documents, { replace: false });
  };

  const handleSimulationComplete = () => {
    navigate(ROUTES.simulationHistory);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-10 h-10 border-3 border-stone-200 border-t-primary-800 rounded-full mx-auto mb-4 animate-spin" />
          <p className="text-primary-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <SimulationSession
      resumeText={resumeText}
      resumeId={effectiveDocumentId}
      sessionIdToResume={entryState.sessionIdToResume}
      initialConfig={entryState.interviewConfig}
      onBack={handleBack}
      onInterviewComplete={handleSimulationComplete}
    />
  );
}

function SimulationHistoryWrapper() {
  const navigate = useNavigate();
  const { openInterviewModalWithResume } = useOutletContext<{ openInterviewModalWithResume: (resumeId: number) => void }>();

  const handleBack = () => {
    navigate(ROUTES.documents);
  };

  const handleViewSimulation = async (sessionId: string, _documentId?: number) => {
    navigate(`${ROUTES.simulationHistory}/${sessionId}`);
  };

  const handleRestartSimulation = (documentId: number) => {
    openInterviewModalWithResume(documentId);
  };

  const handleContinueSimulation = (sessionId: string) => {
    navigate(ROUTES.simulationSession, { state: { sessionIdToResume: sessionId } });
  };

  return <SimulationHistoryPage onBack={handleBack} onViewInterview={handleViewSimulation} onRestartInterview={handleRestartSimulation} onContinueInterview={handleContinueSimulation} />;
}

function SimulationDetailPageWrapper() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [interview, setInterview] = useState<InterviewDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!sessionId) {
      navigate(ROUTES.simulationHistory);
      return;
    }
    simulationApi.getSessionDetails(sessionId)
      .then(detail => {
        setInterview(detail);
        setLoading(false);
      })
      .catch(() => {
        setError('加载模拟详情失败');
        setLoading(false);
      });
  }, [sessionId, navigate]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
      </div>
    );
  }

  if (error || !interview) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-center">
          <p className="text-accent-danger mb-4">{error || '模拟记录不存在'}</p>
          <button
            type="button"
            onClick={() => navigate(ROUTES.simulationHistory)}
            className="btn-primary"
          >
            返回模拟记录
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center gap-4 mb-8">
        <button
          type="button"
          onClick={() => navigate(ROUTES.simulationHistory)}
          className="w-9 h-9 flex items-center justify-center rounded-lg border border-stone-200 dark:border-[#2d3548] text-primary-400 dark:text-[#9ca3af] hover:text-primary-700 dark:hover:text-[#f3f4f6] hover:border-primary-300 dark:hover:border-[#4b5563] transition-colors"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <title>返回模拟记录</title>
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 className="text-lg font-semibold text-primary-800 dark:text-[#f3f4f6] tracking-tight">
            模拟详情
          </h1>
          <p className="text-xs text-primary-400 dark:text-[#6b7280] font-mono tracking-wide">
            #{sessionId!.slice(-8)}
          </p>
        </div>
      </div>
      <SimulationDetailPanel interview={interview} />
    </div>
  );
}

function KnowledgeBaseManagePageWrapper() {
  const navigate = useNavigate();
  const handleUpload = () => {
    navigate(ROUTES.knowledgebaseUpload);
  };
  const handleChat = () => {
    navigate(ROUTES.knowledgebaseChat);
  };
  return <KnowledgeBaseManagePage onUpload={handleUpload} onChat={handleChat} />;
}

function KnowledgeBaseQueryPageWrapper() {
  const navigate = useNavigate();
  const location = useLocation();
  const isChatMode = location.pathname === ROUTES.knowledgebaseChat;

  const handleBack = () => {
    if (isChatMode) {
      navigate(ROUTES.knowledgebase);
    } else {
      navigate(ROUTES.documents);
    }
  };
  const handleUpload = () => {
    navigate(ROUTES.knowledgebaseUpload);
  };
  return <KnowledgeBaseQueryPage onBack={handleBack} onUpload={handleUpload} />;
}

function KnowledgeBaseUploadPageWrapper() {
  const navigate = useNavigate();
  const handleUploadComplete = (_result: UploadKnowledgeBaseResponse) => {
    navigate(ROUTES.knowledgebase);
  };
  const handleBack = () => {
    navigate(ROUTES.knowledgebase);
  };
  return <KnowledgeBaseUploadPage onUploadComplete={handleUploadComplete} onBack={handleBack} />;
}

function VoiceSimulationWrapper() {
  return <VoiceSimulationPage />;
}

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Loading />}>
        <Routes>
          {/* Landing Page — independent layout */}
          <Route path="/" element={<LandingPage />} />

          {/* App workspace — sidebar layout */}
          <Route path="/app" element={<Layout />}>
            <Route index element={<Navigate to={ROUTES.documents} replace />} />

            <Route path="documents/upload" element={<DocumentUploadWrapper />} />
            <Route path="documents" element={<DocumentListWrapper />} />
            <Route path="documents/:documentId" element={<DocumentDetailWrapper />} />

            <Route path="simulation" element={<SimulationHubPage />} />
            <Route path="simulation/history" element={<SimulationHistoryWrapper />} />
            <Route path="simulation/history/:sessionId" element={<SimulationDetailPageWrapper />} />
            <Route path="simulation/session" element={<SimulationSessionWrapper />} />
            <Route path="simulation/session/:documentId" element={<SimulationSessionWrapper />} />

            <Route path="voice" element={<VoiceSimulationWrapper />} />
            <Route path="voice/:sessionId/evaluation" element={<VoiceEvaluationPage />} />

            <Route path="knowledgebase" element={<KnowledgeBaseManagePageWrapper />} />
            <Route path="knowledgebase/upload" element={<KnowledgeBaseUploadPageWrapper />} />
            <Route path="knowledgebase/chat" element={<KnowledgeBaseQueryPageWrapper />} />

            <Route path="schedule" element={<SchedulePage />} />
            <Route path="about" element={<AboutPage />} />
            <Route path="ai-config" element={<AiRuntimeConfigPage />} />
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
