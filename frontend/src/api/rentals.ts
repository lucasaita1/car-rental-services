import { carApi } from './http'
import type { Rental } from './types'

export async function rentCar(
  carId: number,
  userId: number,
  expectedReturnDate?: string,
): Promise<string> {
  const { data } = await carApi.post<string>(`/rental/rent/${carId}/user/${userId}`, null, {
    params: expectedReturnDate ? { expectedReturnDate } : undefined,
  })
  return data
}

export async function returnCar(carId: number): Promise<string> {
  const { data } = await carApi.post<string>(`/rental/return/${carId}`)
  return data
}

export async function rentalsByUser(userId: number): Promise<Rental[]> {
  const { data } = await carApi.get<Rental[]>(`/rental/user/${userId}`)
  return data
}

export async function activeRentals(): Promise<Rental[]> {
  const { data } = await carApi.get<Rental[]>('/rental/active')
  return data
}

export async function overdueRentals(): Promise<Rental[]> {
  const { data } = await carApi.get<Rental[]>('/rental/overdue')
  return data
}
