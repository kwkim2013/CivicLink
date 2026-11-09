import { useState } from "react";
import { logout } from "../../api/client";
import { Icon } from "../common/Icon";
const menus = [
  ["dashboard", "업무 대시보드", "grid"],
  ["complaints", "민원 목록", "file"],
  ["create", "민원 등록", "plus"],
  ["ai", "AI 분석 결과", "spark"],
  ["decisions", "결정례 검색", "book"],
];
export function Layout({ user, page, onNavigate, onLogout, children }) {
  const [collapsed, setCollapsed] = useState(false),
    [error, setError] = useState("");
  async function signOut() {
    try {
      await logout();
      onLogout();
    } catch (e) {
      setError(e.message);
    }
  }
  return (
    <div className={`workspace ${collapsed ? "sidebar-collapsed" : ""}`}>
      <header className="topbar">
        <button
          className="icon-button"
          aria-label="메뉴 접기 또는 펼치기"
          aria-expanded={!collapsed}
          onClick={() => setCollapsed(!collapsed)}
        >
          <Icon name="menu" />
        </button>
        <button className="brand" onClick={() => onNavigate("dashboard")}>
          <span>
            <Icon name="spark" />
          </span>
          CivicLink
          <span className="brand-description">공공민원 AI 업무지원</span>
        </button>
        <div className="account">
          <span className="avatar">
            <Icon name="user" size={18} />
          </span>
          <button className="account-name" onClick={() => onNavigate("mypage")}>{user.username}</button>
          <button
            className="icon-button"
            title="로그아웃"
            aria-label="로그아웃"
            onClick={signOut}
          >
            <Icon name="logout" size={18} />
          </button>
        </div>
      </header>
      <aside className="sidebar">
        <div className="nav-caption">WORKSPACE</div>
        <nav aria-label="업무 메뉴">
          {menus.map(([key, label, icon]) => (
            <button
              title={label}
              aria-current={page === key ? "page" : undefined}
              className={page === key ? "nav-active" : ""}
              onClick={() => onNavigate(key)}
              key={key}
            >
              <Icon name={icon} />
              <span>{label}</span>
              {page === key && <i />}
            </button>
          ))}
        </nav>
        <div className="sidebar-note">
          <Icon name="spark" />
          <b>더 나은 민원 응대의 시작</b>
          <p>
            근거를 찾고, 내용을 검토하고,
            <br />
            시민에게 정확한 답변을 전하세요.
          </p>
        </div>
        <div className="sidebar-footer">CivicLink · 업무지원 공간</div>
      </aside>
      <main className="shell">
        {error && (
          <p role="alert" className="notice error">
            로그아웃 실패: {error}
          </p>
        )}
        {children}
        <footer className="page-footer">
          CivicLink <span>공공민원 AI 업무지원 시스템</span>
        </footer>
      </main>
    </div>
  );
}
