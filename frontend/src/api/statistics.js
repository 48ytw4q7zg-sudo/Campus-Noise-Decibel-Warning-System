import request from './request';

export const getTimeseries = (params) => request.get('/statistics/timeseries', { params });
export const getAreaStats = (params) => request.get('/statistics/areas', { params });
export const getModelPerformance = () => request.get('/statistics/models');
export const getMultiDim = (params) => request.get('/statistics/multi-dim', { params });
export const getHeatmap = (params) => request.get('/statistics/heatmap', { params });
export const getRadar = (params) => request.get('/statistics/radar', { params });
