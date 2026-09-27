import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  // Produces a small self-contained server in .next/standalone - perfect for the Dockerfile.
  output: 'standalone',
};

export default nextConfig;
