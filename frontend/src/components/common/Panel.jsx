export function Panel({ title, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <h2>{title}</h2>
      {children}
    </section>
  );
}
export function Message({ children, error = false }) {
  return children ? (
    <p className={error ? "message error" : "message"}>{children}</p>
  ) : null;
}
