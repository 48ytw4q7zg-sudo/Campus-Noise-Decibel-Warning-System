import request from './request'

export const getAreas = () => request.get('/areas')

export const updateArea = (id, data) => request.put(`/areas/${id}`, data)
