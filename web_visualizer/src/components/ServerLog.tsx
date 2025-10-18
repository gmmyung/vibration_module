import React, { useState, useEffect } from 'react';
import './ServerLog.css';

interface LogEntry {
  id: string;
  timestamp: string;
  type: 'info' | 'error' | 'success' | 'warning';
  message: string;
  data?: any;
}

interface ServerLogProps {
  logs: LogEntry[];
  onClearLogs?: () => void;
}

export const ServerLog: React.FC<ServerLogProps> = ({ logs, onClearLogs }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [autoScroll, setAutoScroll] = useState(true);

  // 자동 스크롤
  useEffect(() => {
    if (autoScroll) {
      const logContainer = document.getElementById('log-container');
      if (logContainer) {
        logContainer.scrollTop = logContainer.scrollHeight;
      }
    }
  }, [logs, autoScroll]);

  const getLogIcon = (type: string) => {
    switch (type) {
      case 'info': return '📝';
      case 'error': return '❌';
      case 'success': return '✅';
      case 'warning': return '⚠️';
      default: return '📝';
    }
  };

  const getLogClass = (type: string) => {
    switch (type) {
      case 'info': return 'log-info';
      case 'error': return 'log-error';
      case 'success': return 'log-success';
      case 'warning': return 'log-warning';
      default: return 'log-info';
    }
  };

  return (
    <div className="server-log">
      <div className="log-header">
        <h3>📊 서버 로그</h3>
        <div className="log-controls">
          <button 
            onClick={() => setAutoScroll(!autoScroll)}
            className={`auto-scroll-btn ${autoScroll ? 'active' : ''}`}
            title={autoScroll ? '자동 스크롤 끄기' : '자동 스크롤 켜기'}
          >
            {autoScroll ? '🔒' : '🔓'}
          </button>
          <button 
            onClick={() => setIsExpanded(!isExpanded)}
            className="expand-btn"
            title={isExpanded ? '접기' : '펼치기'}
          >
            {isExpanded ? '📉' : '📈'}
          </button>
          <button 
            onClick={onClearLogs}
            className="clear-btn"
            title="로그 지우기"
          >
            🗑️
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="log-content">
          <div className="log-stats">
            <span>총 {logs.length}개 로그</span>
            <span>최근 업데이트: {logs.length > 0 ? new Date(logs[logs.length - 1].timestamp).toLocaleTimeString() : '없음'}</span>
          </div>
          
          <div id="log-container" className="log-container">
            {logs.length === 0 ? (
              <div className="no-logs">로그가 없습니다</div>
            ) : (
              logs.map((log) => (
                <div key={log.id} className={`log-entry ${getLogClass(log.type)}`}>
                  <div className="log-timestamp">
                    {new Date(log.timestamp).toLocaleTimeString()}
                  </div>
                  <div className="log-icon">
                    {getLogIcon(log.type)}
                  </div>
                  <div className="log-message">
                    {log.message}
                  </div>
                  {log.data && (
                    <div className="log-data">
                      <pre>{JSON.stringify(log.data, null, 2)}</pre>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
