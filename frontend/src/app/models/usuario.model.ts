export type NombreRol = 'MOZO' | 'COCINA' | 'ADMINISTRADOR';

export interface Rol {
  idRol: number;
  nombre: NombreRol | string;
  descripcion?: string;
}

export interface LoginRequest {
  nombreUsuario: string;
  clave: string;
}

export interface LoginResponse {
  mensaje: string;
  idUsuario: number;
  nombreCompleto: string;
  nombreUsuario: string;
  rol: NombreRol | string;
  token?: string;
}

export interface Usuario {
  idUsuario: number;
  nombreCompleto: string;
  nombreUsuario: string;
  rol: NombreRol | string;
  activo: boolean;
}
