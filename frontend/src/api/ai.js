import request from './request';

export const classifyNoise = () => request.post('/ai/classify');
export const updateAiConfig = (data) => request.put('/ai/config', data);
