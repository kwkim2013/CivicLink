import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { Panel, Message } from "../common/Panel";
export function ComplaintPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ title: "", content: "", category: "" });
  const [message, setMessage] = useState("");
  const load = () =>
    api("/api/complaints")
      .then(setItems)
      .catch((e) => setMessage(e.message));
  useEffect(() => {
    load();
  }, []);
  async function submit(e) {
    e.preventDefault();
    try {
      await api("/api/complaints", {
        method: "POST",
        body: JSON.stringify(form),
      });
      setForm({ title: "", content: "", category: "" });
      setMessage("민원이 저장되었습니다.");
      load();
    } catch (e) {
      setMessage(e.message);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">MY COMPLAINTS</div>
          <h1>내 민원</h1>
          <p className="muted">민원을 등록하고 처리 상태를 확인합니다.</p>
        </div>
      </div>
      <div className="content-grid">
        <Panel title="민원 등록">
          <form onSubmit={submit} className="form-grid">
            <label>
              제목
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </label>
            <label>
              내용
              <textarea
                rows="9"
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                required
              />
            </label>
            <label>
              분류
              <input
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
              />
            </label>
            <button className="primary">민원 저장</button>
          </form>
          <Message error>{message}</Message>
        </Panel>
        <Panel title="등록 목록">
          {items.map((c) => (
            <div className="list-item" key={c.id}>
              <b>{c.title}</b>
              <p>{c.content?.slice(0, 160)}</p>
              <span className="meta">
                {c.category || "분류 전"} · {c.status}
              </span>
            </div>
          ))}
        </Panel>
      </div>
    </>
  );
}
