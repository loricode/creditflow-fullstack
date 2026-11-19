import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { catchError, debounceTime, map, of, startWith, switchMap, tap, timer } from 'rxjs';
import { ESTADOS, PRODUCTOS, PRODUCTO_POR_VALOR, RIESGOS } from '../../core/catalogos';
import {
  ESTADOS_EN_PROCESO, EstadoSolicitud, FiltroSolicitudes, Pagina, SolicitudResumen, TipoProducto,
} from '../../core/models/solicitud.models';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { aparecer } from '../../shared/animations';
import { EstadoChipComponent } from '../../shared/components/estado-chip.component';
import { VacioComponent } from '../../shared/components/vacio.component';
import { CopPipe } from '../../shared/pipes/cop.pipe';
import { NuevaSolicitudService } from '../nueva-solicitud/nueva-solicitud.service';
import { paginadorEnEspanol } from './paginador-es';

@Component({
  selector: 'app-solicitudes',
  imports: [
    DatePipe, FormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatChipsModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatIconModule, MatButtonModule, MatProgressBarModule,
    EstadoChipComponent, VacioComponent, CopPipe,
  ],
  providers: [{ provide: MatPaginatorIntl, useFactory: paginadorEnEspanol }],
  animations: [aparecer],
  templateUrl: './solicitudes.component.html',
  styleUrl: './solicitudes.component.scss',
})
export class SolicitudesComponent {
  private readonly servicio = inject(SolicitudesService);
  private readonly router = inject(Router);
  private readonly nuevaSolicitud = inject(NuevaSolicitudService);

  protected readonly columnas = ['id', 'nombreCompleto', 'producto', 'monto', 'score', 'estado', 'creadoEn'];
  protected readonly estados = Object.entries(ESTADOS) as [EstadoSolicitud, (typeof ESTADOS)[EstadoSolicitud]][];
  protected readonly productos = PRODUCTOS;

  protected readonly texto = signal('');
  protected readonly filtro = signal<FiltroSolicitudes>({ pagina: 0, tamano: 10, orden: 'creadoEn', direccion: 'desc' });
  protected readonly cargando = signal(true);

  private readonly textoConPausa = toSignal(toObservable(this.texto).pipe(debounceTime(350)), { initialValue: '' });

  /**
   * Recarga al cambiar filtros o texto. Mientras haya solicitudes en evaluación
   * en pantalla se refresca cada 3 s para ver el cambio de estado en vivo.
   */
  protected readonly pagina = toSignal(
    toObservable(computed(() => ({ ...this.filtro(), q: this.textoConPausa() }))).pipe(
      tap(() => this.cargando.set(true)),
      switchMap(f => timer(0, 3000).pipe(
        switchMap(() => this.servicio.listar(f).pipe(catchError(() => of(null)))),
        tap(() => this.cargando.set(false)),
      )),
      map(p => p ?? ({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 } as Pagina<SolicitudResumen>)),
      startWith(null),
    ),
  );

  protected readonly hayEnProceso = computed(() =>
    (this.pagina()?.contenido ?? []).some(s => ESTADOS_EN_PROCESO.includes(s.estado)));

  /** Las filas de mat-table llegan sin tipo: se tipan aquí. */
  protected producto(s: SolicitudResumen) {
    return PRODUCTO_POR_VALOR[s.tipoProducto];
  }

  protected claseRiesgo(s: SolicitudResumen): string {
    return s.nivelRiesgo ? RIESGOS[s.nivelRiesgo].clase : '';
  }

  protected filtrarEstado(estado: EstadoSolicitud | null): void {
    this.filtro.update(f => ({ ...f, estado, pagina: 0 }));
  }

  protected filtrarProducto(producto: TipoProducto | null): void {
    this.filtro.update(f => ({ ...f, producto, pagina: 0 }));
  }

  protected buscar(valor: string): void {
    this.texto.set(valor);
    this.filtro.update(f => ({ ...f, pagina: 0 }));
  }

  protected ordenar(sort: Sort): void {
    const orden = sort.direction ? sort.active : 'creadoEn';
    this.filtro.update(f => ({ ...f, orden, direccion: (sort.direction || 'desc') as 'asc' | 'desc', pagina: 0 }));
  }

  protected paginar(e: PageEvent): void {
    this.filtro.update(f => ({ ...f, pagina: e.pageIndex, tamano: e.pageSize }));
  }

  protected abrir(s: SolicitudResumen): void {
    this.router.navigate(['/solicitudes', s.id]);
  }

  protected abrirNueva(): void {
    this.nuevaSolicitud.abrir().subscribe(id => id && this.router.navigate(['/solicitudes', id]));
  }

  protected limpiar(): void {
    this.texto.set('');
    this.filtro.set({ pagina: 0, tamano: this.filtro().tamano, orden: 'creadoEn', direccion: 'desc' });
  }
}
