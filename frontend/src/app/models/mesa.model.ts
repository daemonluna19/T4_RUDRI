export interface Mesa {
  idMesa: number;
  numero: number;
  codigoFormateado?: string;
  nombre: string;
  nombreFamilia?: string;
  capacidad: number;
  numeroPersonas: number;
  estado: 'LIBRE' | 'OCUPADA';
  activa: boolean;
  ocupada?: boolean;
}
