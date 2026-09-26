import { Compass } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Empty } from '../components/ui';

export default function NotFound() {
  return (
    <div className="container page">
      <Empty icon={<Compass size={36} />} title="Page not found">
        <p style={{ marginBottom: 14 }}>This page does not exist (or it belongs to someone else).</p>
        <Link to="/" className="btn btn-primary">Go home</Link>
      </Empty>
    </div>
  );
}
