import { useState } from "react";
import { api } from "../../api/client";
import { Panel, Message } from "../common/Panel";
import { Icon } from "../common/Icon";
export function DecisionPage() {
  const [query, setQuery] = useState(""),
    [results, setResults] = useState([]),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false),
    [searched, setSearched] = useState(false),
    [selected, setSelected] = useState(null);
  async function submit(e) {
    e.preventDefault();
    setMessage("");
    setBusy(true);
    setSelected(null);
    try {
      const data = await api("/api/decisions/search", {
        method: "POST",
        body: JSON.stringify({ query, topK: 5 }),
      });
      setResults(data);
      setSearched(true);
      setSelected(data[0] || null);
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
    }
  }
  const detail = selected?.decision || selected;
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">KNOWLEDGE SEARCH</div>
          <h1>결정례 검색</h1>
          <p className="muted">
            유사한 고충민원 사례를 찾아 업무 판단의 근거를 확인하세요.
          </p>
        </div>
        <span className="badge">의미 기반 검색</span>
      </div>
      <div className="search-hero">
        <Icon name="book" size={28} />
        <h2>어떤 사례를 찾고 계신가요?</h2>
        <p>단어뿐 아니라 민원의 상황과 맥락을 함께 검색합니다.</p>
        <form onSubmit={submit} className="searchbar">
          <div className="search-input">
            <Icon name="search" />
            <input
              aria-label="결정례 검색어"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder=""
              required
            />
          </div>
          <button disabled={busy} className="primary">
            {busy ? "검색 중…" : "검색"}
          </button>
        </form>
      </div>
      <Message error>{message}</Message>
      <div className="content-grid">
        <Panel
          title={searched ? `검색 결과 · ${results.length}건` : "관련 결정례"}
        >
          {results.map((hit, i) => (
            <button
              className={`decision-result ${selected === hit ? "selected" : ""}`}
              key={i}
              onClick={() => setSelected(hit)}
            >
              <span className="result-index">
                {String(i + 1).padStart(2, "0")}
              </span>
              <div>
                <b>{hit.decision?.title || hit.title}</b>
                <small>유사도 {Number(hit.score || 0).toFixed(3)}</small>
                <p>
                  {(
                    hit.decision?.requestContent ||
                    hit.decision?.body ||
                    ""
                  ).slice(0, 140)}
                </p>
              </div>
              <Icon name="arrow" size={16} />
            </button>
          ))}
          {!results.length && (
            <div className="empty">
              <Icon name="search" size={32} />
              <b>
                {searched
                  ? "검색 결과가 없습니다"
                  : "필요한 사례를 검색해 보세요"}
              </b>
              <p>
                {searched
                  ? "다른 표현이나 상황으로 다시 검색해 주세요."
                  : "관련성이 높은 결정례를 최대 5개까지 보여드립니다."}
              </p>
            </div>
          )}
        </Panel>
        <Panel title="결정례 상세">
          {detail ? (
            <>
              <span className="badge">{detail.category || "참고 결정례"}</span>
              <h3>{detail.title}</h3>
              <div className="result-label">신청 내용</div>
              <div className="read-box ai-output">
                {detail.requestContent ||
                  detail.body ||
                  "본문 정보가 없습니다."}
              </div>
              {detail.decisionContent && (
                <>
                  <div className="result-label">결정 내용</div>
                  <div className="read-box ai-output">
                    {detail.decisionContent}
                  </div>
                </>
              )}
              {/^https?:\/\//i.test(detail.sourceUrl || "") && (
                <a
                  className="source-link"
                  href={detail.sourceUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  출처 보기 ↗
                </a>
              )}
            </>
          ) : (
            <div className="empty ai-empty">
              <span className="empty-symbol">
                <Icon name="book" size={32} />
              </span>
              <b>사례의 맥락까지 확인하세요</b>
              <p>
                검색 결과를 선택하면 신청 내용과
                <br />
                결정 내용을 이곳에서 확인할 수 있습니다.
              </p>
            </div>
          )}
        </Panel>
      </div>
    </>
  );
}
