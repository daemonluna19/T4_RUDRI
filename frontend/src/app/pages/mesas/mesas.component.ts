
import {
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { RudriHeaderComponent } from '../../components/rudri-header/rudri-header.component';
import { MesaCardComponent } from '../../components/mesa-card/mesa-card.component';

import { MesaService } from '../../services/mesa.service';
import { PedidoService } from '../../services/pedido.service';
import { Mesa } from '../../models/mesa.model';

@Component({
  selector: 'app-mesas',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RudriHeaderComponent,
    MesaCardComponent
  ],
  templateUrl: './mesas.component.html',
  styleUrl: './mesas.component.css'
})
export class MesasComponent implements OnInit {

  readonly pedidoService = inject(PedidoService);

  private readonly router = inject(Router);
  private readonly mesasApi = inject(MesaService);
  private readonly http = inject(HttpClient);
readonly liberando = signal(false);
  private readonly mesasDemo = signal<Mesa[]>([]);

  // Mesa seleccionada
  readonly mesaSeleccionada = signal<Mesa | null>(null);

  // Datos del formulario
  readonly nombreFamilia = signal('');
  readonly numeroPersonas = signal(1);

  // Estados del formulario
  readonly guardando = signal(false);
  readonly mensajeError = signal('');

  readonly mostrarFormulario = computed(
    () => this.mesaSeleccionada() !== null
  );

  readonly mesaService = {
    mesas: computed(() => {
      const mesas = this.mesasApi.mesas();
      return mesas.length > 0 ? mesas : this.mesasDemo();
    })
  };

  ngOnInit(): void {
    this.cargarMesas();
  }
liberarMesa(): void {
  const mesa = this.mesaSeleccionada();

  if (!mesa || mesa.estado !== 'OCUPADA') return;

  if (mesa.idMesa <= 0) {
    this.mensajeError.set(
      'No se puede liberar una mesa de demostración.'
    );
    return;
  }

  this.liberando.set(true);
  this.mensajeError.set('');

  this.mesasApi.liberarMesa(mesa.idMesa).subscribe({
    next: () => {
      this.liberando.set(false);
      this.cancelarSeleccion();
      this.cargarMesas();
    },
    error: (error) => {
      console.error('Error al liberar mesa:', error);

      this.liberando.set(false);
      this.mensajeError.set(
        error.error?.message ||
        error.error?.mensaje ||
        'No se pudo liberar la mesa.'
      );
    }
  });
}
  cargarMesas(): void {
    this.mesasApi.listarMesas().subscribe(() => {
      if (this.mesasApi.mesas().length > 0) return;

      this.http.get<Mesa[]>('/mock/mesas.json').subscribe({
        next: (mesas) => {
          this.mesasDemo.set(
            mesas.map((mesa) => ({
              ...mesa,
              idMesa: -Math.abs(mesa.idMesa),
              activa: true
            }))
          );
        },
        error: () => this.mesasDemo.set([])
      });
    });
  }

  // Seleccionar mesa y abrir formulario
 elegirMesa(mesa: Mesa): void {
  if (!mesa.activa) return;

  // Ahora también permitimos seleccionar mesas ocupadas.
  this.mesaSeleccionada.set(mesa);

  this.nombreFamilia.set('');
  this.numeroPersonas.set(1);
  this.mensajeError.set('');
}
  // Registrar ocupación
  confirmarOcupacion(): void {

    const mesa = this.mesaSeleccionada();

    if (!mesa) return;

    const nombre = this.nombreFamilia().trim();
    const personas = Number(this.numeroPersonas());

    if (!nombre) {
      this.mensajeError.set(
        'Ingrese el nombre de la familia.'
      );
      return;
    }

    if (!Number.isInteger(personas) || personas < 1) {
      this.mensajeError.set(
        'Ingrese una cantidad válida de personas.'
      );
      return;
    }

    if (personas > mesa.capacidad) {
      this.mensajeError.set(
        `La mesa tiene una capacidad máxima de ${mesa.capacidad} personas.`
      );
      return;
    }

    // Las mesas de demostración no existen en la base de datos.
    if (mesa.idMesa <= 0) {
      this.mensajeError.set(
        'Esta mesa de demostración no puede registrarse en el servidor.'
      );
      return;
    }

    this.guardando.set(true);
    this.mensajeError.set('');

    this.mesasApi.ocuparMesa(mesa.idMesa, {
      nombre: nombre,
      numeroPersonas: personas
    }).subscribe({

      next: (mesaActualizada) => {

        // Guardar mesa seleccionada para el pedido
        this.pedidoService.seleccionarMesa(mesaActualizada);

        this.guardando.set(false);

        // Ir al menú
        this.router.navigate(['/menu']);
      },

    error: (error) => {
  this.guardando.set(false);

  console.error('ERROR AL OCUPAR MESA:', error);
  console.error('Código HTTP:', error.status);
  console.error('Respuesta del servidor:', error.error);

  const detalle =
    error.error?.message ||
    error.error?.mensaje ||
    error.message ||
    'Error desconocido';

  this.mensajeError.set(
    `No se pudo ocupar la mesa. ${detalle} (HTTP ${error.status})`
  );

  this.cargarMesas();
}

    });
  }

  cancelarSeleccion(): void {
    this.mesaSeleccionada.set(null);
    this.nombreFamilia.set('');
    this.numeroPersonas.set(1);
    this.mensajeError.set('');
  }
}
