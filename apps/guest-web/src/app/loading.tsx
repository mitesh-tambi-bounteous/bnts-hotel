export default function Loading() {
  return (
    <div className="stack-lg" aria-busy="true">
      <span className="skeleton" style={{ width: "50%" }} />
      <div className="grid" aria-hidden="true">
        <div className="skeleton sk-card" />
        <div className="skeleton sk-card" />
        <div className="skeleton sk-card" />
      </div>
    </div>
  );
}
