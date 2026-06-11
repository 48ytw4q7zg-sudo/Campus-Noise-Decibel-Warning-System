import request from './request';

export const getCcswitchStatus = () => request.get('/ccswitch/status');
export const reloadCcswitchConfig = () => request.post('/ccswitch/reload');
export const computeThreshold = (data) => request.post('/ccswitch/threshold/compute', data);
export const batchComputeThreshold = (data) => request.post('/ccswitch/threshold/batch-compute', data);
export const updateAreaAdaptiveConfig = (data) => request.put('/ccswitch/threshold/area-config', data);
export const getAreaAdaptiveConfig = () => request.get('/ccswitch/threshold/area-config');
