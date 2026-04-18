interface ToolbarProps {
  fileName: string;
  isModified: boolean;
  saveStatus: 'idle' | 'saving' | 'saved' | 'error';
  onSave: () => void;
}

export function Toolbar({ fileName, isModified, saveStatus, onSave }: ToolbarProps) {
  const getStatusText = () => {
    switch (saveStatus) {
      case 'saving':
        return 'Saving...';
      case 'saved':
        return 'Saved';
      case 'error':
        return 'Error saving';
      default:
        return isModified ? 'Modified' : '';
    }
  };

  const getStatusClass = () => {
    switch (saveStatus) {
      case 'saved':
        return 'saved';
      case 'error':
        return 'error';
      default:
        return isModified ? 'modified' : '';
    }
  };

  return (
    <div className="toolbar">
      <span className="toolbar-title">{fileName}</span>
      <span className={`toolbar-status ${getStatusClass()}`}>{getStatusText()}</span>
      <button
        className="btn btn-primary"
        onClick={onSave}
        disabled={!isModified || saveStatus === 'saving'}
      >
        Save
      </button>
    </div>
  );
}
