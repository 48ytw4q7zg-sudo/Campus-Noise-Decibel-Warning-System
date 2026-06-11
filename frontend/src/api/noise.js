import request from './request'

export const queryNoiseRecords = (params) => request.get('/noise/records', { params })

export const getNoiseLatest = () => request.get('/noise/records/latest')

export const createNoiseRecord = (data) => request.post('/noise/records', data)

export const getNoiseDetail = (id) => request.get(`/noise/records/${id}`)
