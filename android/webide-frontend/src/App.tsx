import { useState, useEffect, useCallback } from 'react';
import { api, AppInfo, FileInfo } from './api/client';
import { Layout } from './components/Layout';
import { AppList } from './components/AppList';
import { FileExplorer } from './components/FileExplorer';
import { CodeEditor } from './components/CodeEditor';
import { Toolbar } from './components/Toolbar';

function App() {
  const [apps, setApps] = useState<AppInfo[]>([]);
  const [selectedApp, setSelectedApp] = useState<AppInfo | null>(null);
  const [files, setFiles] = useState<FileInfo[]>([]);
  const [selectedFile, setSelectedFile] = useState<FileInfo | null>(null);
  const [fileContent, setFileContent] = useState<string>('');
  const [originalContent, setOriginalContent] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');

  // Load apps on mount
  useEffect(() => {
    loadApps();
  }, []);

  const loadApps = async () => {
    try {
      setLoading(true);
      setError(null);
      const appList = await api.listApps();
      setApps(appList);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load apps');
    } finally {
      setLoading(false);
    }
  };

  // Load files when app is selected
  useEffect(() => {
    if (selectedApp) {
      loadFiles(selectedApp.id);
    } else {
      setFiles([]);
      setSelectedFile(null);
    }
  }, [selectedApp]);

  const loadFiles = async (appId: string) => {
    try {
      const fileList = await api.listFiles(appId);
      setFiles(fileList);
    } catch (err) {
      console.error('Failed to load files:', err);
    }
  };

  // Load file content when file is selected
  useEffect(() => {
    if (selectedApp && selectedFile && !selectedFile.isDirectory) {
      loadFileContent(selectedApp.id, selectedFile.path);
    } else {
      setFileContent('');
      setOriginalContent('');
    }
  }, [selectedApp, selectedFile]);

  const loadFileContent = async (appId: string, path: string) => {
    try {
      const content = await api.readFile(appId, path);
      setFileContent(content);
      setOriginalContent(content);
      setSaveStatus('idle');
    } catch (err) {
      console.error('Failed to load file:', err);
      setFileContent('');
      setOriginalContent('');
    }
  };

  const handleSelectApp = (app: AppInfo) => {
    if (selectedApp?.id !== app.id) {
      setSelectedApp(app);
      setSelectedFile(null);
    }
  };

  const handleSelectFile = (file: FileInfo) => {
    if (!file.isDirectory && selectedFile?.path !== file.path) {
      setSelectedFile(file);
    }
  };

  const handleContentChange = (content: string) => {
    setFileContent(content);
    setSaveStatus('idle');
  };

  const handleSave = useCallback(async () => {
    if (!selectedApp || !selectedFile || selectedFile.isDirectory) return;

    try {
      setSaveStatus('saving');
      await api.writeFile(selectedApp.id, selectedFile.path, fileContent);
      setOriginalContent(fileContent);
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
    } catch (err) {
      console.error('Failed to save:', err);
      setSaveStatus('error');
    }
  }, [selectedApp, selectedFile, fileContent]);

  const handleCreateApp = async (name: string) => {
    try {
      const newApp = await api.createApp(name);
      await loadApps();
      const app = apps.find(a => a.id === newApp.id) || { id: newApp.id, name: newApp.name, hasIcon: false };
      setSelectedApp(app);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create app');
    }
  };

  const handleDeleteApp = async (appId: string) => {
    if (!confirm('Are you sure you want to delete this app?')) return;

    try {
      await api.deleteApp(appId);
      if (selectedApp?.id === appId) {
        setSelectedApp(null);
        setSelectedFile(null);
      }
      await loadApps();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete app');
    }
  };

  const handleCreateFile = async (name: string) => {
    if (!selectedApp) return;

    try {
      await api.createFile(selectedApp.id, name);
      await loadFiles(selectedApp.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create file');
    }
  };

  const handleDeleteFile = async (path: string) => {
    if (!selectedApp) return;
    if (!confirm('Are you sure you want to delete this file?')) return;

    try {
      await api.deleteFile(selectedApp.id, path);
      if (selectedFile?.path === path) {
        setSelectedFile(null);
      }
      await loadFiles(selectedApp.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete file');
    }
  };

  const isModified = fileContent !== originalContent;

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (error) {
    return (
      <div className="error">
        <p>Error: {error}</p>
        <button onClick={loadApps} className="btn btn-primary">Retry</button>
      </div>
    );
  }

  return (
    <Layout
      sidebar={
        <>
          <AppList
            apps={apps}
            selectedApp={selectedApp}
            onSelectApp={handleSelectApp}
            onCreateApp={handleCreateApp}
            onDeleteApp={handleDeleteApp}
          />
          {selectedApp && (
            <FileExplorer
              files={files}
              selectedFile={selectedFile}
              onSelectFile={handleSelectFile}
              onCreateFile={handleCreateFile}
              onDeleteFile={handleDeleteFile}
            />
          )}
        </>
      }
      toolbar={
        selectedFile && !selectedFile.isDirectory ? (
          <Toolbar
            fileName={selectedFile.path}
            isModified={isModified}
            saveStatus={saveStatus}
            onSave={handleSave}
          />
        ) : null
      }
      content={
        selectedFile && !selectedFile.isDirectory ? (
          <CodeEditor
            content={fileContent}
            onChange={handleContentChange}
            onSave={handleSave}
          />
        ) : (
          <div className="empty-state">
            <h2>KleinLisp Web IDE</h2>
            <p>
              {!selectedApp
                ? 'Select an app from the sidebar to get started'
                : 'Select a file to edit'}
            </p>
          </div>
        )
      }
    />
  );
}

export default App;
