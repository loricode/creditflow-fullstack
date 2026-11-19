import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, of, switchMap, timer } from 'rxjs';
import { PRODUCTO_POR_VALOR } from '../../core/catalogos';
import { AnaliticaService } from '../../core/services/analitica.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { listaEscalonada, aparecer } from '../../shared/animations';
import { EstadoChipComponent } from '../../shared/components/estado-chip.component';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';
import { VacioComponent } from '../../shared/components/vacio.component';
import { CopPipe } from '../../shared/pipes/cop.pipe';
import { NuevaSolicitudService } from '../nueva-solicitud/nueva-solicitud.service';

const REFRESCO_MS = 5000;

@Component({
  selector: 'app-panel',
  imports: [
    DatePipe, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule,
    KpiCardComponent, EstadoChipComponent, VacioComponent, CopPipe,
  ],
  animations: [listaEscalonada, aparecer],
  templateUrl: './panel.component.html',
  styleUrl: './panel.component.scss',
})
export class PanelComponent {
  private readonly analitica = inject(AnaliticaService);
  private readonly solicitudes = inject(SolicitudesService);
  private readonly nuevaSolicitud = inject(NuevaSolicitudService);
  private readonly router = inject(Router);

  protected readonly productos = PRODUCTO_POR_VALOR;

  /** Indicadores y últimas solicitudes, refrescados cada 5 s. */
  private readonly datos = toSignal(
    timer(0, REFRESCO_MS).pipe(
      switchMap(() => combineLatest([
        this.analitica.estadisticas(),
        this.solicitudes.listar({ pagina: 0, tamano: 6 }),
      ]).pipe(catchError(() => of(null)))),
    ),
  );

  protected readonly cargando = computed(() => this.datos() === undefined);
  protected readonly stats = computed(() => this.datos()?.[0] ?? null);
  protected readonly recientes = computed(() => this.datos()?.[1].contenido ?? []);

  protected readonly maxDia = computed(() => Math.max(1, ...(this.stats()?.ultimosDias ?? []).map(d => d.total)));
  protected readonly maxProducto = computed(() => Math.max(1, ...(this.stats()?.porProducto ?? []).map(p => p.total)));

  protected abrirNueva(): void {
    this.nuevaSolicitud.abrir().subscribe(id => id && this.router.navigate(['/solicitudes', id]));
  }
}
