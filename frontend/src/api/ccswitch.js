import request from './request';

export const getCcswitchStatus = () => request.get('/ccswitch/status');
export const reloadCcswitchConfig = () => request.post('/ccswitch/reload');
