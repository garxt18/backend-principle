import { ArrowRight, CalendarCheck2, Hammer, ListVideo, Map } from 'lucide-react';
import { Link } from 'react-router-dom';

const FEATURES = [
  { icon: ListVideo, title: 'Playlist-first Java & Spring', text: 'Java Basics, Advanced Java and Spring Boot follow Coder Army\'s playlists lecture by lecture - what each video teaches, a practice task and a direct link.' },
  { icon: Map, title: 'Backend roadmap', text: '17 levels up to Kubernetes and system design. Pick the Hindi or English resource you will follow, or attach your own playlist to any level or topic.' },
  { icon: CalendarCheck2, title: 'Planly', text: 'Tell it your hours per week. Planly builds a week-by-week schedule and tells you honestly whether you are ahead or behind.' },
  { icon: Hammer, title: 'Rebuild Lab', text: 'Retype any Spring Boot project line by line in build order. Every line says what it does and why you type it. Download progress, keep going in your IDE, sync back.' },
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
            One place to follow the roadmap, plan every week, and rebuild real Spring Boot projects line by line until you understand every single line. DSA? Straight to Striver's A2Z sheet.
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
