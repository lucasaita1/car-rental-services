import { carApi } from './http'
import type { Car, CarPayload } from './types'

export async function listCars(): Promise<Car[]> {
  const { data } = await carApi.get<Car[]>('/cars')
  return data
}

export async function createCar(payload: CarPayload): Promise<Car> {
  const { data } = await carApi.post<Car>('/cars', payload)
  return data
}

export async function updateCar(id: number, payload: CarPayload): Promise<Car> {
  const { data } = await carApi.put<Car>(`/cars/${id}`, payload)
  return data
}

export async function deleteCar(id: number): Promise<void> {
  await carApi.delete(`/cars/${id}`)
}
