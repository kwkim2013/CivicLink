import { useEffect, useState } from "react";
import { api, prepareCsrf } from "./api/client";
import { AuthPage } from "./components/auth/AuthPage";
import { Layout } from "./components/layout/Layout";
import { DashboardPage } from "./components/dashboard/DashboardPage";
import { ComplaintManager as ComplaintPage } from "./components/complaints/ComplaintManager";
import { DecisionPage } from "./components/decisions/DecisionPage";
import { AiPage } from "./components/ai/AiPage";
import { MyPage } from "./components/mypage/MyPage";

export default function App() {
  const [user, setUser] = useState(null);
  const [page, setPage] = useState("dashboard");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const expired = () => {
      setUser(null);
      setPage("dashboard");
    };
    window.addEventListener("auth-expired", expired);
    return () => window.removeEventListener("auth-expired", expired);
  }, []);
  useEffect(() => {
    prepareCsrf()
      .then(() => api("/api/auth/me"))
      .then(setUser)
      .catch((error) => {
        // 401은 아직 로그인하지 않은 초기 상태이므로 로그인 화면으로 이동한다.
        // 그 외 오류는 원인을 숨기지 않고 콘솔에 남긴다.
        if (error?.status !== 401) console.error("API 초기화 실패:", error);
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);
  if (loading)
    return <div className="loading">CivicLink을 준비하고 있습니다...</div>;
  if (!user) return <AuthPage onLogin={setUser} />;
  const pages = {
    dashboard: <DashboardPage onNavigate={setPage} />,
    complaints: <ComplaintPage key="list" onNavigate={setPage} />,
    create: <ComplaintPage key="create" mode="create" onNavigate={setPage} />,
    decisions: <DecisionPage />,
    ai: <AiPage />,
    mypage: <MyPage user={user} onNavigate={setPage} onUserUpdate={setUser} />,
  };
  return (
    <Layout
      user={user}
      page={page}
      onNavigate={setPage}
      onLogout={() => setUser(null)}
    >
      {pages[page]}
    </Layout>
  );
}
