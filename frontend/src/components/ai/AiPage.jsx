import { useState } from "react";
import { api } from "../../api/client";
import { Panel, Message } from "../common/Panel";
import { Icon } from "../common/Icon";
export function AiPage() {
  const [content, setContent] = useState(""),
    [result, setResult] = useState(null),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  async function run(path) {
    if (!content.trim()) return setMessage("민원 내용을 입력하세요.");
    setMessage("");
    setResult(null);
    setBusy(true);
    try {
      setResult(
        await api(path, { method: "POST", body: JSON.stringify({ content }) }),
      );
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">AI REVIEW WORKSPACE</div>
          <h1>AI 분석 결과</h1>
          <p className="muted">
            민원 내용을 분석하고, 관련 결정례를 바탕으로 답변을 준비하세요.
          </p>
        </div>
        <span className="badge amber">
          <Icon name="user" size={14} />
          담당자 검토 필요
        </span>
      </div>
      <div className="notice">
        <Icon name="spark" />
        AI는 업무 판단을 돕습니다. 추천 내용과 근거를 확인한 뒤 활용해 주세요.
      </div>
      <div className="content-grid">
        <Panel title="민원 원문">
          <label className="field-label" htmlFor="ai-content">
            분석할 민원 내용
          </label>
          <textarea
            id="ai-content"
            rows="14"
            disabled={busy}
            value={content}
            onChange={(e) => {
              setContent(e.target.value);
              setResult(null);
            }}
            placeholder="민원 내용을 입력하세요. 구체적인 상황과 요청 사항이 포함되면 더 유용한 분석을 받을 수 있습니다."
          />
          <div className="field-count">{content.length.toLocaleString()}자</div>
          <div className="form-actions">
            <button
              disabled={busy}
              className="secondary"
              onClick={() => run("/api/ai/classify")}
            >
              <Icon name="file" size={17} />
              자동분류
            </button>
            <button
              disabled={busy}
              className="primary"
              onClick={() => run("/api/ai/answer")}
            >
              <Icon name="spark" size={17} />
              {busy ? "분석 중…" : "답변 초안 생성"}
            </button>
          </div>
          <Message error>{message}</Message>
        </Panel>
        <Panel title="분석 및 답변">
          {busy ? (
            <div className="empty ai-empty" role="status">
              <span className="spinner" />
              <b>관련 근거를 찾고 내용을 분석하고 있습니다.</b>
              <p>잠시만 기다려 주세요.</p>
            </div>
          ) : result ? (
            <div className="ai-result">
              {result.category && (
                <>
                  <div className="result-label">추천 분류</div>
                  <h3>{result.category}</h3>
                  <div className="result-label">추천 이유</div>
                  <p className="read-box">{result.reason}</p>
                </>
              )}
              {result.answer && (
                <>
                  <div className="result-label">답변 초안</div>
                  <div className="read-box ai-output">{result.answer}</div>
                </>
              )}
              <div className="result-label">
                {result.references ? "참고 결정례" : "검토 안내"}
              </div>
              {result.references?.length ? (
                result.references.map((r, i) => (
                  <article className="reference-card" key={i}>
                    <Icon name="book" />
                    <div>
                      <b>{r.title}</b>
                      <small>문서번호 {r.documentNumber}</small>
                      {/^https?:\/\//i.test(r.sourceUrl || "") && (
                        <a href={r.sourceUrl} target="_blank" rel="noreferrer">
                          출처 보기 ↗
                        </a>
                      )}
                    </div>
                  </article>
                ))
              ) : (
                <p className="muted">
                  {result.references
                    ? "인용된 결정례가 없습니다."
                    : "추천 분류와 민원 내용을 함께 확인해 주세요."}
                </p>
              )}
            </div>
          ) : (
            <div className="empty ai-empty">
              <span className="empty-symbol">
                <Icon name="spark" size={34} />
              </span>
              <b>민원에서 답변의 실마리를 찾으세요</b>
              <p>
                왼쪽에 내용을 입력하고 분석을 시작하면
                <br />
                추천 분류 또는 답변 초안이 표시됩니다.
              </p>
              <div className="workflow">
                <span>민원 입력</span>
                <Icon name="arrow" size={14} />
                <span>AI 분석</span>
                <Icon name="arrow" size={14} />
                <span>담당자 검토</span>
              </div>
            </div>
          )}
        </Panel>
      </div>
    </>
  );
}
