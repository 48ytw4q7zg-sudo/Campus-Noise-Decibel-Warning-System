import request from './request';

export const queryReports = (params) => request.get('/reports', { params });
export const getReportDetail = (id) => request.get(`/reports/${id}`);
export const generateReport = (data) => request.post('/reports', data);
export const configureReport = (data) => request.put('/reports/config', data);
