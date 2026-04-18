import { useState } from 'react';
import { AppInfo } from '../api/client';

interface AppListProps {
  apps: AppInfo[];
  selectedApp: AppInfo | null;
  onSelectApp: (app: AppInfo) => void;
  onCreateApp: (name: string) => void;
  onDeleteApp: (appId: string) => void;
}

export function AppList({
  apps,
  selectedApp,
  onSelectApp,
  onCreateApp,
  onDeleteApp,
}: AppListProps) {
  const [showModal, setShowModal] = useState(false);
  const [newAppName, setNewAppName] = useState('');

  const handleCreate = () => {
    if (newAppName.trim()) {
      onCreateApp(newAppName.trim());
      setNewAppName('');
      setShowModal(false);
    }
  };

  return (
    <div className="app-list">
      <div className="app-list-header">
        <span>Apps</span>
        <button className="btn btn-icon" onClick={() => setShowModal(true)} title="New App">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
      </div>

      {apps.map(app => (
        <div
          key={app.id}
          className={`app-item ${selectedApp?.id === app.id ? 'selected' : ''}`}
          onClick={() => onSelectApp(app)}
        >
          <svg className="app-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="3" width="18" height="18" rx="2"></rect>
            <path d="M9 9l3 3-3 3"></path>
          </svg>
          <span className="app-name">{app.name}</span>
          <button
            className="btn btn-icon"
            onClick={(e) => {
              e.stopPropagation();
              onDeleteApp(app.id);
            }}
            title="Delete App"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      ))}

      {apps.length === 0 && (
        <div style={{ padding: '16px', color: 'var(--text-muted)', fontSize: '13px' }}>
          No apps found. Create one to get started.
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Create New App</h2>
            <input
              type="text"
              className="modal-input"
              placeholder="App name"
              value={newAppName}
              onChange={e => setNewAppName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreate()}
              autoFocus
            />
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleCreate}>
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
