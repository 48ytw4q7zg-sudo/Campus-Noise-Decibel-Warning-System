import request from './request';

export const importData = (formData) => request.post('/data/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
});
export const exportReport = (params) => request.get('/data/export-report', { params, responseType: 'blob' });
