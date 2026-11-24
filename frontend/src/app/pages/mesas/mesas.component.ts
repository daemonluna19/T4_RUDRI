import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RudriHeaderComponent } from '../../components/rudri-header/rudri-header.component';
import { MesaCardComponent } from '../../components/mesa-card/mesa-card.component';
import { MesaService } from '../../services/mesa.service';
import { PedidoService } from '../../services/pedido.service';
import { Mesa } from '../../models/mesa.model';

@Component({
  selector: 'app-mesas',
  standalone: true,
  imports: [CommonModule, RudriHeaderComponent, MesaCardComponent],
  templateUrl: './mesas.component.html',
  styleUrl: './mesas.component.css'
})
export class MesasComponent implements OnInit {
  readonly pedidoService = inject(PedidoService);
  private readonly router = inject(Router);
  private readonly mesasApi = inject(MesaService);
  private readonly http = inject(HttpClient);
  private readonly mesasDemo = signal<Mesa[]>([]);

  readonly mesaService = {
   mesas: computed(() => {
  const mesasReales = this.mesasApi.mesas();
  const mesas = mesasReales.length > 0 ? mesasReales : this.mesasDemo();

  const mesasConPedidoActivo = new Set(
    this.pedidoService.pedidosPendientesCocina().map((pedido) => pedido.idMesa)
  );
  return mesas.map((mesa) => {
    if (!mesasConPedidoActivo.has(mesa.idMesa)) return mesa;

    const codigo = mesa.codigoFormateado ?? `MESA ${mesa.numero}`;
    return {
      ...mesa,
      codigoFormateado: `${codigo} · OCUPADA`,
      activa: false
    };
  });
})
  };

  ngOnInit(): void {
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

  elegirMesa(mesa: Mesa): void {
    if (!mesa.activa) return;

    this.pedidoService.seleccionarMesa(mesa);
    this.router.navigate(['/menu']);
  }
}
