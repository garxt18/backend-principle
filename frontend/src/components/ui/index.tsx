import { clsx } from 'clsx';
import { Check as CheckIcon, ChevronRight, Star as StarIcon } from 'lucide-react';
import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';

// ------------------------------------------------------------------------------------ Button
type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'default' | 'primary' | 'ghost' | 'outline' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  icon?: boolean;
  loading?: boolean;
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'default', size = 'md', icon, loading, className, children, disabled, type = 'button', ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={clsx(
        'btn',
        variant !== 'default' && `btn-${variant}`,
        size === 'sm' && 'btn-sm',
        size === 'lg' && 'btn-lg',
        icon && 'btn-icon',
        className,
      )}
      {...rest}
    >
      {loading && <span className="spinner" aria-hidden />}
      {children}
    </button>
  );
});

// ------------------------------------------------------------------------------------ Progress
export function Progress({ value, className, tone }: { value: number; className?: string; tone?: 'success' }) {
  // Start at 0 and grow on mount so every bar animates in.
  const [shown, setShown] = useState(0);
  useEffect(() => {
    const id = requestAnimationFrame(() => setShown(Math.max(0, Math.min(100, value))));
    return () => cancelAnimationFrame(id);
  }, [value]);
  return (
    <div className={clsx('progress', tone, className)} role="progressbar" aria-valuenow={Math.round(value)} aria-valuemin={0} aria-valuemax={100}>
      <span style={{ width: `${shown}%` }} />
    </div>
  );
}

export function Ring({ value, size = 96, stroke = 8, children, color }: { value: number; size?: number; stroke?: number; children?: ReactNode; color?: string }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const [shown, setShown] = useState(0);
  useEffect(() => {
    const id = requestAnimationFrame(() => setShown(Math.max(0, Math.min(100, value))));
    return () => cancelAnimationFrame(id);
  }, [value]);
  return (
    <div className="ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden>
        <circle className="track" cx={size / 2} cy={size / 2} r={r} strokeWidth={stroke} />
        <circle
          className="value"
          cx={size / 2}
          cy={size / 2}
          r={r}
          strokeWidth={stroke}
          strokeDasharray={c}
          strokeDashoffset={c - (shown / 100) * c}
          style={color ? { stroke: color } : undefined}
        />
      </svg>
      <div className="ring-label">{children}</div>
    </div>
  );
}

// ------------------------------------------------------------------------------------ Check & Star
export function Check({ checked, onChange, label }: { checked: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <button type="button" role="checkbox" aria-checked={checked} aria-label={label} className={clsx('check', checked && 'on')} onClick={() => onChange(!checked)}>
      <CheckIcon size={12} strokeWidth={3.5} />
    </button>
  );
}

export function Star({ on, onChange, label }: { on: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <button type="button" aria-pressed={on} aria-label={label} title={label} className={clsx('star', on && 'on')} onClick={() => onChange(!on)}>
      <StarIcon size={17} />
    </button>
  );
}

// ------------------------------------------------------------------------------------ Accordion
export function Accordion({
  open,
  onToggle,
  leading,
  title,
  subtitle,
  meta,
  children,
}: {
  open: boolean;
  onToggle: () => void;
  leading?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  meta?: ReactNode;
  children: ReactNode;
}) {
  const id = useId();
  return (
    <section className={clsx('acc', open && 'open')}>
      <button type="button" className="acc-head" aria-expanded={open} aria-controls={id} onClick={onToggle}>
        <span className="row" style={{ gap: 12, flexWrap: 'nowrap' }}>
          <ChevronRight size={18} className="acc-chevron" />
          {leading}
        </span>
        <span style={{ minWidth: 0 }}>
          <span className="acc-title">{title}</span>
          {subtitle && <span className="small subtle" style={{ display: 'block', marginTop: 2 }}>{subtitle}</span>}
        </span>
        {meta && <span className="acc-meta">{meta}</span>}
      </button>
      <div className="acc-body" id={id}>
        <div className="acc-inner">{open && <div className="acc-content">{children}</div>}</div>
      </div>
    </section>
  );
}

// ------------------------------------------------------------------------------------ Chips (segmented control)
export function Chips<T extends string>({ value, options, onChange, label }: { value: T; options: { value: T; label: ReactNode }[]; onChange: (v: T) => void; label: string }) {
  return (
    <div className="chips" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button key={o.value} type="button" role="radio" aria-checked={value === o.value} className={clsx('chip', value === o.value && 'active')} onClick={() => onChange(o.value)}>
          {o.label}
        </button>
      ))}
    </div>
  );
}

// ------------------------------------------------------------------------------------ Form field
export function Field({ label, hint, error, children }: { label: string; hint?: ReactNode; error?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {error ? <span className="field-error">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  );
}

// ------------------------------------------------------------------------------------ Feedback
export function Skeleton({ height = 16, width = '100%', style }: { height?: number; width?: number | string; style?: React.CSSProperties }) {
  return <div className="skeleton" style={{ height, width, ...style }} />;
}

export function PageSkeleton() {
  return (
    <div className="page" aria-busy="true">
      <Skeleton height={30} width={260} />
      <Skeleton height={14} width={420} style={{ marginTop: 12 }} />
      <div className="grid grid-4" style={{ marginTop: 26 }}>
        {[0, 1, 2, 3].map((i) => <Skeleton key={i} height={104} />)}
      </div>
      <div className="stack" style={{ marginTop: 18 }}>
        {[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} height={58} />)}
      </div>
    </div>
  );
}

export function Empty({ icon, title, children }: { icon?: ReactNode; title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      {icon}
      <h3>{title}</h3>
      {children && <div className="small">{children}</div>}
    </div>
  );
}

export function ErrorState({ error }: { error: unknown }) {
  return <div className="alert alert-danger">{error instanceof Error ? error.message : 'Something went wrong.'}</div>;
}

// ------------------------------------------------------------------------------------ Confirm dialog
export function ConfirmDialog({ title, body, confirmLabel, onConfirm, onClose, danger }: { title: string; body: ReactNode; confirmLabel: string; onConfirm: () => void; onClose: () => void; danger?: boolean }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <div className="muted" style={{ marginBottom: 18 }}>{body}</div>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} autoFocus>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}

export function Difficulty({ value }: { value: string }) {
  const label = value === 'EASY' ? 'Easy' : value === 'MEDIUM' ? 'Medium' : 'Hard';
  return <span className={`diff diff-${value}`}>{label}</span>;
}

export function LangBadge({ lang }: { lang: 'HI' | 'EN' }) {
  return <span className={clsx('badge', lang === 'HI' ? 'badge-warning' : 'badge-accent')}>{lang === 'HI' ? 'Hindi' : 'English'}</span>;
}
