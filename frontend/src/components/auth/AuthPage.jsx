import { useState } from "react";
import { login, register } from "../../api/client";
import { Message } from "../common/Panel";
import { Icon } from "../common/Icon";
export function AuthPage({ onLogin }) {
  const [mode, setMode] = useState("login"),
    [username, setUsername] = useState(""),
    [password, setPassword] = useState(""),
    [confirmation, setConfirmation] = useState(""),
    [message, setMessage] = useState(""),
    [show, setShow] = useState(false),
    [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setMessage("");
    if (mode === "register" && password !== confirmation)
      return setMessage("비밀번호가 일치하지 않습니다.");
    setBusy(true);
    try {
      if (mode === "register") {
        await register(username, password);
        setMode("login");
        setPassword("");
        setConfirmation("");
        setMessage("가입되었습니다. 로그인해 주세요.");
      } else onLogin(await login(username, password));
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth-page">
      <header className="auth-header">
        <div className="brand">
          <span>
            <Icon name="spark" />
          </span>
          CivicLink
        </div>
        <span>공공민원 AI 업무지원 시스템</span>
      </header>
      <div className="auth-layout">
        <section className="auth-intro">
          <div className="eyebrow">A BETTER WAY TO SERVE</div>
          <h1>
            복잡한 민원 업무,
            <br />
            명확한 답을 찾는
            <br />
            <em>새로운 방식.</em>
          </h1>
          <p>
            민원 분류부터 관련 결정례 검색까지.
            <br />
            담당자의 판단을 돕는 하나의 업무 공간입니다.
          </p>
          <div className="intro-features">
            {[
              ["file", "체계적인 민원 관리"],
              ["book", "결정례에 기반한 검색"],
              ["spark", "AI와 함께하는 답변 작성"],
            ].map(([icon, label]) => (
              <div key={icon}>
                <span>
                  <Icon name={icon} />
                </span>
                {label}
                <Icon name="check" size={16} />
              </div>
            ))}
          </div>
          <div className="intro-caption">
            사람의 판단에, AI의 가능성을 더합니다.
          </div>
        </section>
        <section className="auth-card">
          <span className="auth-symbol">
            <Icon name={mode === "login" ? "lock" : "user"} size={24} />
          </span>
          <h2>{mode === "login" ? "로그인" : "회원가입"}</h2>
          <p className="muted">
            {mode === "login"
              ? "업무 계정으로 로그인해 주세요."
              : "새로운 업무 공간을 시작해 보세요."}
          </p>
          <form onSubmit={submit} className="form-grid">
            <label>
              아이디
              <input
                autoComplete="username"
                placeholder="아이디를 입력하세요"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                minLength={3}
                maxLength={50}
                pattern="[a-zA-Z0-9_.\-]{3,50}"
                title="영문·숫자·밑줄·마침표·하이픈으로 3~50자"
              />
            </label>
            <label>
              비밀번호
              <div className="password-field">
                <input
                  autoComplete={
                    mode === "login" ? "current-password" : "new-password"
                  }
                  type={show ? "text" : "password"}
                  placeholder={
                    mode === "register"
                      ? "12자 이상 입력하세요"
                      : "비밀번호를 입력하세요"
                  }
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={12}
                  maxLength={128}
                />
                <button
                  type="button"
                  className="icon-button"
                  aria-label={show ? "비밀번호 숨기기" : "비밀번호 표시"}
                  aria-pressed={show}
                  onClick={() => setShow(!show)}
                >
                  <Icon name="eye" size={18} />
                </button>
              </div>
            </label>
            {mode === "register" && (
              <label>
                비밀번호 확인
                <input
                  type={show ? "text" : "password"}
                  autoComplete="new-password"
                  required
                  value={confirmation}
                  onChange={(e) => setConfirmation(e.target.value)}
                  placeholder="비밀번호를 다시 입력하세요"
                />
              </label>
            )}
            <button disabled={busy} className="primary auth-submit">
              {busy ? "처리 중…" : mode === "login" ? "로그인" : "계정 만들기"}
              <Icon name="arrow" size={18} />
            </button>
          </form>
          <Message>{message}</Message>
          <div className="auth-switch">
            {mode === "login"
              ? "계정이 없으신가요?"
              : "이미 계정이 있으신가요?"}
            <button
              disabled={busy}
              className="link-button"
              onClick={() => {
                setMode(mode === "login" ? "register" : "login");
                setMessage("");
                setPassword("");
                setConfirmation("");
              }}
            >
              {mode === "login" ? "회원가입" : "로그인"}
            </button>
          </div>
        </section>
      </div>
      <footer className="auth-footer">CivicLink · 공공민원 업무지원</footer>
    </main>
  );
}
