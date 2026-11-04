const PATHS = {
  x: <path d="M18 6 6 18M6 6l12 12" />,
  check: <path d="M20 6 9 17l-5-5" />,
  info: (
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 16v-4M12 8h.01" />
    </>
  ),
  "arrow-left": <path d="m12 19-7-7 7-7M19 12H5" />,
  image: (
    <>
      <rect width="18" height="18" x="3" y="3" rx="2" />
      <circle cx="9" cy="9" r="2" />
      <path d="m21 15-3.09-3.09a2 2 0 0 0-2.82 0L6 21" />
    </>
  ),
} as const;

export function Icon({ name }: { name: keyof typeof PATHS }) {
  return (
    <svg className="icon" aria-hidden="true" viewBox="0 0 24 24">
      {PATHS[name]}
    </svg>
  );
}
