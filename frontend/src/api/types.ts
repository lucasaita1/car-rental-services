export type CarStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE'
export type RentalStatus = 'ACTIVE' | 'FINISHED'
export type UserRole = 'USER' | 'ADMIN'

export interface Car {
  id: number
  model: string
  color: string
  plate: string
  year: number
  rentalDate: string | null
  returnDate: string | null
  userId: number | null
  status: CarStatus | null
}

export interface CarPayload {
  model: string
  color: string
  plate: string
  year: number
  status?: CarStatus
}

export interface Rental {
  id: number
  carId: number
  carModel: string
  carPlate: string
  userId: number
  userName: string
  userEmail: string
  rentalDate: string
  expectedReturnDate: string | null
  returnDate: string | null
  status: RentalStatus
  overdue: boolean
}

export interface User {
  id: number
  name: string
  email: string
  cpf: string | null
  cnh: string | null
  role: UserRole
}

export interface RegisterPayload {
  name: string
  email: string
  cpf: string
  cnh: string
  password: string
}

export interface LoginResponse {
  token: string
  role: UserRole
  user: { id: string; name: string; cpf: string | null; email: string }
}

export interface TokenPayload {
  sub: string
  id: number
  name: string
  role: UserRole
  exp: number
}
