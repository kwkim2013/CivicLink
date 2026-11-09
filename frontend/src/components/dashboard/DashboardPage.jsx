import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { Panel, Message } from "../common/Panel";
import { Icon } from "../common/Icon";
export function DashboardPage({ onNavigate }) {
  const [complaints, setComplaints] = useState([]),
    [decisions, setDecisions] = useState([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    Promise.all([api("/api/complaints"), api("/api/decisions")])
      .then(([c, d]) => {
        setComplaints(c);
        setDecisions(d);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);
  const categories = Object.entries(
    complaints.reduce((a, c) => {
      const key = c.category || "미분류";
      a[key] = (a[key] || 0) + 1;
      return a;
    }, {}),
  ).sort((a, b) => b[1] - a[1]);
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">WORKSPACE OVERVIEW</div>
          <h1>업무 대시보드</h1>
          <p className="muted">
            민원 현황을 한눈에 확인하고, 오늘의 업무를 시작하세요.
          </p>
        </div>
        <button className="primary" onClick={() => onNavigate("create")}>
          <Icon name="plus" size={18} />
          민원 등록
        </button>
      </div>
      <Message error>{error}</Message>
      <div className="stats">
        {[
          [
            "내 민원",
            complaints.length,
            "file",
            "조회된 민원 기준 · 최대 100건",
          ],
          ["참고 결정례", decisions.length, "book", "검색에 활용할 지식"],
          [
            "미분류 민원",
            complaints.filter((c) => !c.category).length,
            "spark",
            "분류를 확인해 주세요",
          ],
        ].map(([label, count, icon, help]) => (
          <div key={label}>
            <span className={`stat-icon ${icon}`}>
              <Icon name={icon} />
            </span>
            <small>{label}</small>
            <strong>
              {loading || error ? "—" : count}
              <span>건</span>
            </strong>
            <p>{help}</p>
          </div>
        ))}
      </div>
      <div className="dashboard-grid">
        <Panel title="최근 등록 민원">
          <div className="section-toolbar">
            <span className="muted">등록된 민원의 진행 상태</span>
            <button
              className="link-button"
              onClick={() => onNavigate("complaints")}
            >
              전체 보기 <Icon name="arrow" size={16} />
            </button>
          </div>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>민원 제목</th>
                  <th>분류</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                {complaints.slice(0, 5).map((c) => (
                  <tr key={c.id}>
                    <td>
                      <button
                        className="table-link"
                        onClick={() => onNavigate("complaints")}
                      >
                        {c.title}
                      </button>
                    </td>
                    <td>{c.category || "미분류"}</td>
                    <td>
                      <span className="badge">{c.status || "접수"}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!complaints.length && (
            <div className="empty">
              <Icon name="file" size={32} />
              <b>
                {loading
                  ? "민원을 불러오고 있습니다"
                  : "아직 등록된 민원이 없습니다"}
              </b>
              <p>첫 민원을 등록하면 이곳에서 확인할 수 있어요.</p>
            </div>
          )}
        </Panel>
        <Panel title="빠른 업무 시작">
          <div className="quick-list">
            {[
              [
                "create",
                "민원 등록",
                "새로운 민원을 작성하고 관리하세요.",
                "plus",
              ],
              ["ai", "AI 분석", "분류 추천과 답변 초안을 확인하세요.", "spark"],
              [
                "decisions",
                "결정례 검색",
                "유사 사례에서 업무의 근거를 찾으세요.",
                "book",
              ],
            ].map(([key, label, desc, icon]) => (
              <button
                className="quick"
                onClick={() => onNavigate(key)}
                key={key}
              >
                <span className="quick-icon">
                  <Icon name={icon} />
                </span>
                <span>
                  <b>{label}</b>
                  <small>{desc}</small>
                </span>
                <Icon name="arrow" size={18} />
              </button>
            ))}
          </div>
        </Panel>
      </div>
      <div className="dashboard-grid">
        <Panel title="분야별 민원 현황 · 조회 기준">
          {categories.length ? (
            categories.slice(0, 5).map(([label, count]) => (
              <div className="chart-row" key={label}>
                <span>{label}</span>
                <div>
                  <i
                    style={{ width: `${(count / complaints.length) * 100}%` }}
                  />
                </div>
                <b>{count}건</b>
              </div>
            ))
          ) : (
            <p className="empty">민원이 등록되면 분야별 현황이 표시됩니다.</p>
          )}
        </Panel>
        <section className="insight-card">
          <div className="eyebrow">YOUR AI ASSISTANT</div>
          <h2>
            답변의 시작은,
            <br />
            신뢰할 수 있는 근거에서.
          </h2>
          <p>
            관련 결정례를 찾아 민원 내용을 분석하고,
            <br />
            담당자가 검토할 답변 초안을 작성합니다.
          </p>
          <button className="secondary" onClick={() => onNavigate("ai")}>
            AI 분석 시작하기 <Icon name="arrow" size={17} />
          </button>
          <Icon name="spark" size={130} />
        </section>
      </div>
    </>
  );
}
