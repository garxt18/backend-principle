import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import './globals.css';

// The root layout wraps every page. It renders once and stays mounted while you navigate.
export const metadata: Metadata = {
  title: 'Task Board',
  description: 'A small full-stack Next.js app: App Router, server actions, API routes and Prisma',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <main>{children}</main>
      </body>
    </html>
  );
}
