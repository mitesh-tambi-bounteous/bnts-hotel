export default function NotFound() {
  return (
    <div className="stack">
      <h1>We couldn&apos;t find that page</h1>
      <p className="text-muted">The hotel or booking you asked for doesn&apos;t exist.</p>
      <a className="btn btn-primary" href="/">
        Search hotels
      </a>
    </div>
  );
}
