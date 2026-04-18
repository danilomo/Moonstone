import { useState } from 'react';
import { FileInfo } from '../api/client';

interface FileExplorerProps {
  files: FileInfo[];
  selectedFile: FileInfo | null;
  onSelectFile: (file: FileInfo) => void;
  onCreateFile: (name: string) => void;
  onDeleteFile: (path: string) => void;
}

export function FileExplorer({
  files,
  selectedFile,
  onSelectFile,
  onCreateFile,
  onDeleteFile,
}: FileExplorerProps) {
  const [showModal, setShowModal] = useState(false);
  const [newFileName, setNewFileName] = useState('');

  const handleCreate = () => {
    if (newFileName.trim()) {
      onCreateFile(newFileName.trim());
      setNewFileName('');
      setShowModal(false);
    }
  };

  // Only show top-level files (not nested)
  const topLevelFiles = files.filter(f => !f.path.includes('/'));

  return (
    <div className="file-explorer">
      <div className="file-explorer-header">
        <span>Files</span>
        <button className="btn btn-icon" onClick={() => setShowModal(true)} title="New File">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
      </div>

      {topLevelFiles.map(file => (
        <div
          key={file.path}
          className={`file-item ${selectedFile?.path === file.path ? 'selected' : ''}`}
          onClick={() => onSelectFile(file)}
        >
          {file.isDirectory ? (
            <svg className="file-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
            </svg>
          ) : (
            <svg className="file-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
          )}
          <span className="file-name">{file.name}</span>
          {file.name !== 'app.scm' && !file.isDirectory && (
            <button
              className="btn btn-icon"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteFile(file.path);
              }}
              title="Delete File"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          )}
        </div>
      ))}

      {topLevelFiles.length === 0 && (
        <div style={{ padding: '16px', color: 'var(--text-muted)', fontSize: '13px' }}>
          No files
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Create New File</h2>
            <input
              type="text"
              className="modal-input"
              placeholder="File name (e.g., utils.scm)"
              value={newFileName}
              onChange={e => setNewFileName(e.target.value)}
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
