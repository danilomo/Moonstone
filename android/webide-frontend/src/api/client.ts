export interface AppInfo {
  id: string;
  name: string;
  description?: string;
  version?: string;
  author?: string;
  hasIcon: boolean;
}

export interface FileInfo {
  name: string;
  path: string;
  isDirectory: boolean;
  size?: number;
}

const BASE_URL = '/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Unknown error' }));
    throw new Error(error.error || `HTTP ${response.status}`);
  }

  return response.json();
}

export const api = {
  // Apps
  async listApps(): Promise<AppInfo[]> {
    return request<AppInfo[]>('/apps');
  },

  async createApp(name: string): Promise<{ id: string; name: string }> {
    return request('/apps', {
      method: 'POST',
      body: JSON.stringify({ name }),
    });
  },

  async deleteApp(appId: string): Promise<void> {
    await request(`/apps/${appId}`, { method: 'DELETE' });
  },

  // Files
  async listFiles(appId: string): Promise<FileInfo[]> {
    return request<FileInfo[]>(`/apps/${appId}/files`);
  },

  async readFile(appId: string, path: string): Promise<string> {
    const result = await request<{ content: string }>(`/apps/${appId}/files/${path}`);
    return result.content;
  },

  async writeFile(appId: string, path: string, content: string): Promise<void> {
    await request(`/apps/${appId}/files/${path}`, {
      method: 'PUT',
      body: JSON.stringify({ content }),
    });
  },

  async createFile(appId: string, name: string, content?: string): Promise<{ name: string; path: string }> {
    return request(`/apps/${appId}/files`, {
      method: 'POST',
      body: JSON.stringify({ name, content: content || '' }),
    });
  },

  async deleteFile(appId: string, path: string): Promise<void> {
    await request(`/apps/${appId}/files/${path}`, { method: 'DELETE' });
  },

  // Status
  async getStatus(): Promise<{ status: string; version: string }> {
    return request('/status');
  },
};
