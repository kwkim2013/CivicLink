import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { Panel, Message } from "../common/Panel";
import { Icon } from "../common/Icon";
const emptyForm = { title: "", content: "", category: "" };
export function ComplaintManager({ mode = "list", onNavigate }) {
  const [items, setItems] = useState([]),
    [form, setForm] = useState(emptyForm),
    [editingId, setEditingId] = useState(null),
    [message, setMessage] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true),
    [query, setQuery] = useState(""),
    [category, setCategory] = useState(""),
    [page, setPage] = useState(1);
  async function load() {
    try {
      setItems(await api("/api/complaints"));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    load();
  }, []);
  const update = (name, value) => setForm((v) => ({ ...v, [name]: value }));
  const edit = (c) => {
    setEditingId(c.id);
    setForm({
      title: c.title || "",
      content: c.content || "",
      category: c.category || "",
    });
    setMessage("");
  };
  const cancel = () => {
    setEditingId(null);
    setForm(emptyForm);
  };
  async function submit(e) {
    e.preventDefault();
    setMessage("");
    setError("");
    setBusy(true);
    try {
      await api(
        editingId ? `/api/complaints/${editingId}` : "/api/complaints",
        { method: editingId ? "PUT" : "POST", body: JSON.stringify(form) },
      );
      setMessage(
        editingId ? "민원이 수정되었습니다." : "민원이 등록되었습니다.",
      );
      cancel();
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function remove(c) {
    if (!confirm(`'${c.title}' 민원을 삭제할까요?`)) return;
    setBusy(true);
    try {
      await api(`/api/complaints/${c.id}`, { method: "DELETE" });
      setMessage("민원이 삭제되었습니다.");
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  const filtered = items.filter(
    (c) =>
      (!category || (c.category || "미분류") === category) &&
      `${c.title} ${c.id} ${c.content}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const pages = Math.max(1, Math.ceil(filtered.length / 8)),
    current = Math.min(page, pages),
    creating = mode === "create" || editingId !== null;
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">COMPLAINT MANAGEMENT</div>
          <h1>
            {editingId !== null
              ? "민원 수정"
              : creating
                ? "민원 등록"
                : "민원 목록"}
          </h1>
          <p className="muted">
            {creating
              ? "민원 내용을 입력해 체계적인 업무 관리를 시작하세요."
              : "등록한 민원을 조회하고 내용을 관리하세요."}
          </p>
        </div>
        <button
          className={creating ? "secondary" : "primary"}
          onClick={() => {
            cancel();
            onNavigate(creating ? "complaints" : "create");
          }}
        >
          <Icon name={creating ? "file" : "plus"} size={18} />
          {creating ? "목록으로" : "민원 등록"}
        </button>
      </div>
      <Message>{message}</Message>
      <Message error>{error}</Message>
      {creating ? (
        <div className="create-grid">
          <Panel title="민원 기본 정보">
            <form onSubmit={submit} className="form-grid">
              <label>
                민원 제목{" "}
                <input
                  maxLength={300}
                  placeholder="민원의 핵심 내용을 간단히 입력하세요"
                  value={form.title}
                  onChange={(e) => update("title", e.target.value)}
                  required
                />
              </label>
              <label>
                민원 내용
                <textarea
                  rows="10"
                  maxLength={4000}
                  placeholder="발생 상황과 요청 사항을 구체적으로 작성해 주세요."
                  value={form.content}
                  onChange={(e) => update("content", e.target.value)}
                  required
                />
                <span className="field-count">
                  {form.content.length.toLocaleString()} / 4,000자
                </span>
              </label>
              <label>
                분류 <span className="optional">선택</span>
                <input
                  maxLength={100}
                  placeholder="예: 교통 · 주정차"
                  value={form.category}
                  onChange={(e) => update("category", e.target.value)}
                />
              </label>
              <div className="form-actions">
                <button
                  type="button"
                  className="ghost"
                  disabled={busy}
                  onClick={() => {
                    cancel();
                    onNavigate("complaints");
                  }}
                >
                  취소
                </button>
                <button disabled={busy} className="primary">
                  {busy
                    ? "저장 중…"
                    : editingId !== null
                      ? "수정 저장"
                      : "민원 등록"}
                  <Icon name="arrow" size={16} />
                </button>
              </div>
            </form>
          </Panel>
          <aside className="panel guidance">
            <span className="auth-symbol">
              <Icon name="file" />
            </span>
            <h2>좋은 민원 기록의 시작</h2>
            <p className="muted">
              정확한 내용은 더 나은 분석과 응대로 이어집니다.
            </p>
            <ol>
              <li>
                <b>핵심 내용을 담아 주세요</b>
                <p>제목에는 요청 사항이 드러나도록 작성하세요.</p>
              </li>
              <li>
                <b>상황을 구체적으로 작성하세요</b>
                <p>발생 장소와 상황, 필요한 조치를 함께 기록하세요.</p>
              </li>
              <li>
                <b>개인정보를 확인해 주세요</b>
                <p>업무에 필요한 정보만 입력해 주세요.</p>
              </li>
            </ol>
          </aside>
        </div>
      ) : (
        <>
          <div className="tabs">
            <span className="active-tab">
              전체 민원 <b>{items.length}</b>
            </span>
          </div>
          <div className="filter-panel">
            <div className="search-input">
              <Icon name="search" />
              <input
                aria-label="민원 제목 또는 번호 검색"
                placeholder="제목, 민원번호, 내용으로 검색"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setPage(1);
                }}
              />
            </div>
            <select
              aria-label="분류 필터"
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setPage(1);
              }}
            >
              <option value="">전체 분류</option>
              {[...new Set(items.map((c) => c.category || "미분류"))].map(
                (c) => (
                  <option key={c}>{c}</option>
                ),
              )}
            </select>
            <button
              className="secondary"
              onClick={() => {
                setQuery("");
                setCategory("");
                setPage(1);
              }}
            >
              초기화
            </button>
          </div>
          <Panel title={`전체 민원 · ${filtered.length}건`}>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>민원번호</th>
                    <th>민원 제목</th>
                    <th>분류</th>
                    <th>상태</th>
                    <th>관리</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.slice((current - 1) * 8, current * 8).map((c) => (
                    <tr key={c.id}>
                      <td className="muted">#{c.id}</td>
                      <td>
                        <button className="table-link" onClick={() => edit(c)}>
                          {c.title}
                        </button>
                        <div className="cell-summary">
                          {c.content?.slice(0, 65)}
                        </div>
                      </td>
                      <td>{c.category || "미분류"}</td>
                      <td>
                        <span className="badge">{c.status || "접수"}</span>
                      </td>
                      <td>
                        <div className="item-actions">
                          <button
                            className="small-button"
                            onClick={() => edit(c)}
                          >
                            수정
                          </button>
                          <button
                            disabled={busy}
                            className="small-button danger"
                            onClick={() => remove(c)}
                          >
                            삭제
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!filtered.length && (
              <div className="empty">
                <Icon name="search" size={32} />
                <b>{loading ? "불러오는 중…" : "표시할 민원이 없습니다"}</b>
                <p>검색 조건을 변경하거나 새 민원을 등록해 주세요.</p>
              </div>
            )}
            <div className="pagination">
              <span>총 {filtered.length}건</span>
              <div>
                <button
                  className="small-button"
                  disabled={current === 1}
                  onClick={() => setPage(current - 1)}
                >
                  이전
                </button>
                <b>
                  {current} / {pages}
                </b>
                <button
                  className="small-button"
                  disabled={current === pages}
                  onClick={() => setPage(current + 1)}
                >
                  다음
                </button>
              </div>
            </div>
          </Panel>
        </>
      )}
    </>
  );
}
