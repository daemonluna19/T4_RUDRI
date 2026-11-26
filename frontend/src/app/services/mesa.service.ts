import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { API_CONFIG } from './api.config';
import { Mesa } from '../models/mesa.model';

const STORAGE_MESAS = 'rudri_mesas_data';

@Injectable({
  providedIn: 'root'
})
export class MesaService {
  private readonly http = inject(HttpClient);
private readonly apiUrl = 'http://localhost:8080/api/mesas';
  readonly mesas = signal<Mesa[]>(this.cargarMesasLocales());

  formatearCodigoMesa(numero: number): string {
    return `MESA ${String(numero).padStart(2, '0')}`;
  }

  private validarDatosMesa(numero: number, capacidad: number, idIgnorar?: number): string | null {
    if (!Number.isInteger(numero) || numero <= 0) {
      return 'El número de mesa debe ser un entero positivo.';
    }
    if (!Number.isInteger(capacidad) || capacidad <= 0 || capacidad > 50) {
      return 'La capacidad de la mesa debe estar entre 1 y 50 personas.';
    }
    const duplicada = this.mesas().some((m) => m.numero === numero && m.idMesa !== idIgnorar);
    if (duplicada) {
      return 'Ya existe una mesa registrada con ese número.';
    }
    return null;
  }

private obtenerMesasIniciales(): Mesa[] {
  const capacidades: Record<number, number> = {
    5: 6,
    8: 6,
    11: 8
  };

  return Array.from({ length: 12 }, (_, idx) => {
    const numero = idx + 1;

    return {
      idMesa: numero,
      numero,
      codigoFormateado: this.formatearCodigoMesa(numero),
      nombre: `Mesa ${numero}`,
      capacidad: capacidades[numero] ?? 4,
      numeroPersonas: 0,
      estado: 'LIBRE',
      activa: true,
      ocupada: false
    };
  });
}

  private cargarMesasLocales(): Mesa[] {
    try {
      const guardado = localStorage.getItem(STORAGE_MESAS);
      if (guardado) return JSON.parse(guardado);
    } catch {
      // Ignorar
    }
    return this.obtenerMesasIniciales();
  }

  private persistirLocal(lista: Mesa[]): void {
    this.mesas.set(lista);
    try {
      localStorage.setItem(STORAGE_MESAS, JSON.stringify(lista));
    } catch {
      // Ignorar
    }
  }

  listarMesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(API_CONFIG.endpoints.mesas).pipe(
      tap((datos) => {
        const normalizadas = datos.map((m) => ({
          ...m,
          codigoFormateado: this.formatearCodigoMesa(m.numero)
        }));
        this.persistirLocal(normalizadas);
      }),
      catchError(() => of(this.mesas()))
    );
  }
    ocuparMesa(
  id: number,
  datos: { nombre: string; numeroPersonas: number }
): Observable<Mesa> {

  return this.http.put<Mesa>(
    `${this.apiUrl}/${id}/ocupar`,
    datos
  );
}
    librarMesa(id: number) {
      return this.http.put<Mesa>(
        '${this.apiUrl}/${id}/librar', {});
    }

crearMesa(nueva: Omit<Mesa, 'idMesa'>): Observable<Mesa> {
  const num = Number(nueva?.numero);
  const cap = Number(nueva?.capacidad ?? 4);

  const errorValidacion = this.validarDatosMesa(num, cap);

  if (errorValidacion) {
    return throwError(() => new Error(errorValidacion));
  }

  return this.http.post<Mesa>(
    API_CONFIG.endpoints.mesas,
    nueva
  ).pipe(

    tap((creada) => {
      const lista = [
        ...this.mesas(),
        {
          ...creada,
          codigoFormateado: this.formatearCodigoMesa(creada.numero)
        }
      ];

      this.persistirLocal(lista);
    }),

    catchError(() => {
      const actual = this.mesas();

      const nuevoId = actual.length > 0
        ? Math.max(...actual.map((m) => m.idMesa)) + 1
        : 1;

      const mesaLocal: Mesa = {
        idMesa: nuevoId,
        numero: num,
        codigoFormateado: this.formatearCodigoMesa(num),
        nombre: nueva.nombre || `Mesa ${num}`,
        capacidad: cap,
        numeroPersonas: 0,
        estado: 'LIBRE',
        activa: nueva.activa ?? true,
        ocupada: false
      };

      this.persistirLocal([...actual, mesaLocal]);

      return of(mesaLocal);
    })
  );
}

liberarMesa(idMesa: number): Observable<Mesa> {
  return this.http.put<Mesa>(
    `${this.apiUrl}/${idMesa}/liberar`,
    {}
  ).pipe(
    tap((mesaActualizada) => {
      const actualizadas = this.mesas().map(m =>
        m.idMesa === idMesa
          ? {
              ...mesaActualizada,
              codigoFormateado: this.formatearCodigoMesa(
                mesaActualizada.numero
              )
            }
          : m
      );

      this.persistirLocal(actualizadas);
    })
  );
}
  actualizarMesa(mesa: Mesa): Observable<Mesa> {
    const num = Number(mesa?.numero);
    const cap = Number(mesa?.capacidad ?? 4);
    const errorValidacion = this.validarDatosMesa(num, cap, mesa?.idMesa);
    if (errorValidacion) {
      return throwError(() => new Error(errorValidacion));
    }

    const formateada: Mesa = {
      ...mesa,
      numero: num,
      capacidad: cap,
      codigoFormateado: this.formatearCodigoMesa(num)
    };
    return this.http.put<Mesa>(`${API_CONFIG.endpoints.mesas}/${mesa.idMesa}`, formateada).pipe(
      tap(() => {
        this.persistirLocal(this.mesas().map((m) => (m.idMesa === mesa.idMesa ? formateada : m)));
      }),
      catchError(() => {
        this.persistirLocal(this.mesas().map((m) => (m.idMesa === mesa.idMesa ? formateada : m)));
        return of(formateada);
      })
    );
  }

  eliminarMesa(idMesa: number): Observable<void> {
    return this.http.delete<void>(`${API_CONFIG.endpoints.mesas}/${idMesa}`).pipe(
      tap(() => {
        this.persistirLocal(this.mesas().filter((m) => m.idMesa !== idMesa));
      }),
      catchError(() => {
        this.persistirLocal(this.mesas().filter((m) => m.idMesa !== idMesa));
        return of(undefined);
      })
    );
  }
}
