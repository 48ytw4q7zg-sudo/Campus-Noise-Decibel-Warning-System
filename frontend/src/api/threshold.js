import request from './request';

export const getThresholdRules = (params) => request.get('/thresholds/rules', { params });
export const createThresholdRule = (data) => request.post('/thresholds/rules', data);
export const updateThresholdRule = (id, data) => request.put(`/thresholds/rules/${id}`, data);
export const deleteThresholdRule = (id) => request.delete(`/thresholds/rules/${id}`);
export const reloadThresholdRules = () => request.post('/thresholds/rules/reload');
export const getCurrentThreshold = (params) => request.get('/thresholds/current', { params });

// P1-1 自适应阈值
export const getAdaptiveThreshold = (params) => request.get('/thresholds/adaptive/current', { params });
export const updateAdaptiveConfig = (data) => request.put('/thresholds/adaptive/config', data);

// P1-2 混合阈值模型
export const getHybridStatus = () => request.get('/thresholds/hybrid/status');
export const getHybridPerformance = () => request.get('/thresholds/hybrid/performance');
