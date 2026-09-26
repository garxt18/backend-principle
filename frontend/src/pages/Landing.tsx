import { ArrowRight, CalendarCheck2, Hammer, ListChecks, Map } from 'lucide-react';
import { Link } from 'react-router-dom';

const FEATURES = [
  { icon: Map, title: 'Backend roadmap', text: '17 levels from Java basics to Kubernetes and system design, 101 topics with hour estimates and hand-picked Hindi + English videos.' },
  { icon: ListChecks, title: 'DSA sheet', text: '156 must-do LeetCode problems in 16 patterns. Tick them off, star them for revision and keep notes - with Hindi and English explanations one click away.' },
  { icon: CalendarCheck2, title: 'Planly', text: 'Tell it your hours per week and a DSA target. Planly builds a week-by-week schedule and tells you honestly whether you are ahead or behind.' },
  { icon: Hammer, title: 'Rebuild Lab', text: 'Upload any Spring Boot / Java project and retype it line by line in the order a senior engineer builds it - every line explained, in your own notes too.' },
];

export default function Landing() {
  return (
    <>
      <section className="hero">
        <div className="container">
          <span className="hero-pill">Free for you and your friends · Hindi + English</span>
          <h1>
            Become a <em>production-ready</em> backend engineer.
          </h1>
          <p className="lead">
            One place to follow the roadmap, crack DSA, plan every week, and rebuild real Spring Boot projects line by line until you understand every single line.
          </p>
          <div className="row">
            <Link to="/signup" className="btn btn-primary btn-lg">Start learning <ArrowRight size={18} /></Link>
            <Link to="/login" className="btn btn-outline btn-lg">I have an account</Link>
          </div>
          <div className="flow" aria-label="Learning loop">
            {['learn', 'build', 'break', 'debug', 'improve', 'repeat'].map((s, i, all) => (
              <span key={s} style={{ display: 'contents' }}>
                <span>{s}</span>
                {i < all.length - 1 && <span className="arrow">→</span>}
              </span>
            ))}
          </div>
        </div>
      </section>
      <section className="container">
        <div className="grid features stagger">
          {FEATURES.map((f) => (
            <div key={f.title} className="card card-hover feature">
              <span className="feature-icon"><f.icon size={20} /></span>
              <h3>{f.title}</h3>
              <p>{f.text}</p>
            </div>
          ))}
        </div>
      </section>
      <footer className="footer">
        <div className="container row-between">
          <span>Backend Playground · Java · Spring Boot · PostgreSQL</span>
          <span>learn → build → break → debug → improve → repeat</span>
        </div>
      </footer>
    </>
  );
}
