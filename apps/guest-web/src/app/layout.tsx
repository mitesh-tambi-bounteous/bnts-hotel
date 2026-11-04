import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "Guest Web",
  description: "Search, choose a room and book",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <div className="shell">
          <header className="shell-header">
            <a className="shell-brand" href="/">
              Guest Web
            </a>
          </header>
          <main className="shell-main">{children}</main>
          <footer className="shell-footer">Hotel guest web app</footer>
        </div>
      </body>
    </html>
  );
}
