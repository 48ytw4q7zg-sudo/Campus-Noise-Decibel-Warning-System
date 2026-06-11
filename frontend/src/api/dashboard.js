import request from './request'

export const getDashboardOverview = () => request.get('/dashboard/overview')
export const getLatestNoiseRecords = () => request.get('/noise/records/latest')
export const getAlertList = (params) => request.get('/alerts', { params })
export const getCcswitchStatus = () => request.get('/ccswitch/status')
