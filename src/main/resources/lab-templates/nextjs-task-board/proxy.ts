import { NextResponse, type NextRequest } from 'next/server';

// Runs before matching requests (Next.js 16 renamed "middleware" to "proxy").
// Reading data through the API is public; changing it needs the x-api-key header.
export function proxy(request: NextRequest) {
  if (request.method === 'GET') {
    return NextResponse.next();
  }
  const key = request.headers.get('x-api-key');
  if (!process.env.API_KEY || key !== process.env.API_KEY) {
    return NextResponse.json({ error: 'Missing or wrong x-api-key header' }, { status: 401 });
  }
  return NextResponse.next();
}

// Only the REST API is protected; pages and server actions are not affected.
export const config = {
  matcher: '/api/:path*',
};
