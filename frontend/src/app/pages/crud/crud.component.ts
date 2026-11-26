import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RudriHeaderComponent } from '../../components/rudri-header/rudri-header.component';
import { ProductoService } from '../../services/producto.service';
import { MesaService } from '../../services/mesa.service';
import { PedidoService } from '../../services/pedido.service';
import { CategoriaService } from '../../services/categoria.service';
import { Producto } from '../../models/producto.model';
import { Mesa } from '../../models/mesa.model';

@Component({
  selector: 'app-crud',
  standalone: true,
  imports: [CommonModule, FormsModule, RudriHeaderComponent],
  templateUrl: './crud.component.html',
  styleUrl: './crud.component.css'
})
export class CrudComponent {
  readonly productoService = inject(ProductoService);
  readonly mesaService = inject(MesaService);
  readonly pedidoService = inject(PedidoService);
  readonly categoriaService = inject(CategoriaService);

  readonly tabActiva = signal<'productos' | 'mesas' | 'pedidos'>('productos');
  readonly mensaje = signal<string>('');

  formProducto = {
    idProducto: 0,
    idCategoria: 1,
    nombre: '',
    subcategoria: 'Mariscos',
    descripcion: '',
    precio: 25.0,
    disponible: true,
    imagenUrl: 'https://images.unsplash.com/photo-1535399831218-d5bd36d1a6b3?auto=format&fit=crop&w=300&q=80'
  };

formMesa = {
  idMesa: 0,
  numero: 13,
  codigoFormateado: '',
  nombre: '',
  capacidad: 4,
  numeroPersonas: 0,
  estado: 'LIBRE' as 'LIBRE' | 'OCUPADA',
  activa: true
};

  guardarProducto(): void {
    if (!this.formProducto.nombre.trim()) {
      this.mensaje.set('Ingresa el nombre del plato o bebida.');
      return;
    }

    if (this.formProducto.idProducto > 0) {
      this.productoService.actualizarProducto({ ...this.formProducto }).subscribe({
        next: (actualizado) => {
          this.mensaje.set(`Plato "${actualizado.nombre}" actualizado correctamente.`);
          this.limpiarFormProducto();
        },
        error: (err: Error) => {
          this.mensaje.set(err.message);
        }
      });
    } else {
      this.productoService
        .crearProducto({
          idCategoria: Number(this.formProducto.idCategoria),
          nombre: this.formProducto.nombre,
          subcategoria: this.formProducto.subcategoria,
          descripcion: this.formProducto.descripcion,
          precio: Number(this.formProducto.precio),
          disponible: this.formProducto.disponible,
          imagenUrl: this.formProducto.imagenUrl
        })
        .subscribe({
          next: (creado) => {
            this.mensaje.set(`Plato "${creado.nombre}" agregado a la carta.`);
            this.limpiarFormProducto();
          },
          error: (err: Error) => {
            this.mensaje.set(err.message);
          }
        });
    }
  }

  editarProducto(prod: Producto): void {
    this.formProducto = {
      idProducto: prod.idProducto,
      idCategoria: prod.idCategoria,
      nombre: prod.nombre,
      subcategoria: prod.subcategoria || 'Mariscos',
      descripcion: prod.descripcion || '',
      precio: prod.precio,
      disponible: prod.disponible,
      imagenUrl: prod.imagenUrl
    };
  }

  alternarDisponibilidad(prod: Producto): void {
    this.productoService
      .actualizarProducto({ ...prod, disponible: !prod.disponible })
      .subscribe((act) => {
        this.mensaje.set(`"${act.nombre}" ahora está ${act.disponible ? 'Disponible' : 'No disponible'}.`);
      });
  }

  eliminarProducto(idProducto: number): void {
    this.productoService.eliminarProducto(idProducto).subscribe(() => {
      this.mensaje.set('Producto eliminado del catálogo.');
    });
  }

  limpiarFormProducto(): void {
    this.formProducto = {
      idProducto: 0,
      idCategoria: 1,
      nombre: '',
      subcategoria: 'Mariscos',
      descripcion: '',
      precio: 25.0,
      disponible: true,
      imagenUrl: 'https://images.unsplash.com/photo-1535399831218-d5bd36d1a6b3?auto=format&fit=crop&w=300&q=80'
    };
  }

guardarMesa(): void {

  if (this.formMesa.idMesa > 0) {

    this.mesaService.actualizarMesa({
      ...this.formMesa
    }).subscribe({

      next: (m) => {
        this.mensaje.set(`${m.codigoFormateado} actualizada.`);
        this.limpiarFormMesa();
      },

      error: (err: Error) => {
        this.mensaje.set(err.message);
      }

    });

  } else {

    this.mesaService.crearMesa({

      numero: Number(this.formMesa.numero),
      codigoFormateado: this.formMesa.codigoFormateado,
      nombre: this.formMesa.nombre || `Mesa ${this.formMesa.numero}`,
      capacidad: Number(this.formMesa.capacidad),
      numeroPersonas: 0,
      estado: 'LIBRE',
      activa: this.formMesa.activa

    }).subscribe({

      next: (m) => {
        this.mensaje.set(`${m.codigoFormateado} creada exitosamente.`);
        this.limpiarFormMesa();
      },

      error: (err: Error) => {
        this.mensaje.set(err.message);
      }

    });

  }
}

  editarMesa(m: Mesa): void {
  this.formMesa = {
    idMesa: m.idMesa,
    numero: m.numero,
    codigoFormateado: m.codigoFormateado ?? '',
    nombre: m.nombre ?? '',
    capacidad: m.capacidad,
    numeroPersonas: m.numeroPersonas ?? 0,
    estado: m.estado,
    activa: m.activa
  };
}
  

  eliminarMesa(idMesa: number): void {
    this.mesaService.eliminarMesa(idMesa).subscribe(() => {
      this.mensaje.set('Mesa eliminada.');
    });
  }

  limpiarFormMesa(): void {
    this.formMesa = {
      idMesa: 0,
      numero: this.mesaService.mesas().length + 1,
      codigoFormateado: '',
      nombre: '',
      capacidad: 4,
      numeroPersonas: 0,
      estado: 'LIBRE',
      activa: true
    };
  }
}
