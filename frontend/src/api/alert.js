import request from './request'

export const queryAlerts = (params) => request.get('/alerts', { params })

export const getAlertDetail = (id) => request.get(`/alerts/${id}`)

export const confirmAlert = (id, data) => request.put(`/alerts/${id}/confirm`, data)

export const resolveAlert = (id, data) => request.put(`/alerts/${id}/resolve`, data)
