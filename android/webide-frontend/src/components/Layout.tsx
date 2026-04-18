import { ReactNode } from 'react';

interface LayoutProps {
  sidebar: ReactNode;
  toolbar: ReactNode | null;
  content: ReactNode;
}

export function Layout({ sidebar, toolbar, content }: LayoutProps) {
  return (
    <div className="app-layout">
      <div className="sidebar">
        <div className="sidebar-header">
          <h1>KleinLisp IDE</h1>
          <p>Edit your apps from anywhere</p>
        </div>
        <div className="sidebar-content">
          {sidebar}
        </div>
      </div>
      <div className="main-content">
        {toolbar}
        <div className="editor-container">
          {content}
        </div>
      </div>
    </div>
  );
}
