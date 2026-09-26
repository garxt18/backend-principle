/** Brand icons are not part of lucide-react v1, so the YouTube mark is drawn here. */
export function Youtube({ size = 16, color = 'currentColor', style }: { size?: number; color?: string; style?: React.CSSProperties }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden style={style}>
      <rect x="2" y="5" width="20" height="14" rx="4" stroke={color} strokeWidth="2" />
      <path d="M10 9.2v5.6L15 12l-5-2.8z" fill={color} />
    </svg>
  );
}
