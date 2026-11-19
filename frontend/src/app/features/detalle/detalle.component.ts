import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, of, startWith, switchMap, takeWhile, timer } from 'rxjs';
import { ESTADOS, PRODUCTO_POR_VALOR, RIESGOS } from '../../core/catalogos';
import { ESTADOS_EN_PROCESO, SolicitudDetalle } from '../../core/models/solicitud.models';
import { NotificacionService } from '../../core/services/notificacion.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { aparecer, listaEscalonada } from '../../shared/animations';
import { EstadoChipComponent } from '../../shared/components/estado-chip.component';
import { VacioComponent } from '../../shared/components/vacio.component';
import { ContadorDirective } from '../../shared/directives/contador.directive';
import { CopPipe } from '../../shared/pipes/cop.pipe';
import { AmortizacionDialogComponent } from './amortizacion-dialog.component';
import { OriginalDialogComponent } from './original-dialog.component';

type Etapa = { clave: string; etiqueta: string; icono: string; estado: 'hecho' | 'activo' | 'pendiente' | 'fallo' };

@Component({
  selector: 'app-detalle',
  imports: [
    DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule, MatProgressSpinnerModule,
    EstadoChipComponent, VacioComponent, ContadorDirective, CopPipe,
  ],
  animations: [aparecer, listaEscalonada],
  templateUrl: './detalle.component.html',
  styleUrl: './detalle.component.scss',
})
export class DetalleComponent {
  /** Viene de la ruta /solicitudes/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly servicio = inject(SolicitudesService);
  private readonly dialog = inject(MatDialog);
  private readonly notificacion = inject(NotificacionService);

  protected readonly productos = PRODUCTO_POR_VALOR;
  protected readonly estados = ESTADOS;
  protected readonly riesgos = RIESGOS;
  protected readonly reintentando = signal(false);
  private readonly recarga = signal(0);

  /** Consulta cada 1,5 s mientras la solicitud esté en proceso; se detiene al terminar. */
  protected readonly solicitud = toSignal(
    toObservable(computed(() => ({ id: Number(this.id()), n: this.recarga() }))).pipe(
      switchMap(({ id }) => timer(0, 1500).pipe(
        switchMap(() => this.servicio.detalle(id).pipe(catchError(() => of(null)))),
        takeWhile(s => !!s && ESTADOS_EN_PROCESO.includes(s.estado), true),
      )),
      startWith(undefined),
    ),
  );

  protected readonly enProceso = computed(() => {
    const s = this.solicitud();
    return !!s && ESTADOS_EN_PROCESO.includes(s.estado);
  });

  /** Ángulo del medidor de score (0..1000 → 0..180°). */
  protected readonly anguloScore = computed(() => ((this.solicitud()?.score ?? 0) / 1000) * 180);

  protected readonly cargaCuota = computed(() => {
    const s = this.solicitud();
    if (!s?.cuotaMensual) return null;
    const capacidad = s.ingresosMensuales - s.gastosMensuales;
    return capacidad > 0 ? (s.cuotaMensual / capacidad) * 100 : 100;
  });

  protected readonly etapas = computed<Etapa[]>(() => {
    const s = this.solicitud();
    if (!s) return [];
    const evaluada = s.score !== null;
    const final = s.estado === 'APROBADA' || s.estado === 'RECHAZADA';
    const error = s.estado === 'ERROR';
    const estadoDe = (hecho: boolean, activo: boolean): Etapa['estado'] =>
      hecho ? 'hecho' : error && activo ? 'fallo' : activo ? 'activo' : 'pendiente';
    return [
      { clave: 'api', etiqueta: 'Recibida', icono: 'inbox', estado: 'hecho' },
      { clave: 'cola', etiqueta: 'SNS → SQS', icono: 'swap_calls', estado: estadoDe(s.estado !== 'RECIBIDA' || s.intentos > 0, s.estado === 'RECIBIDA') },
      { clave: 's3', etiqueta: 'Archivo S3', icono: 'cloud_done', estado: estadoDe(!!s.s3Key, s.intentos > 0 && !s.s3Key) },
      { clave: 'plsql', etiqueta: 'Score PL/SQL', icono: 'functions', estado: estadoDe(evaluada, !!s.s3Key && !evaluada) },
      { clave: 'ia', etiqueta: 'Explicación IA', icono: 'auto_awesome', estado: estadoDe(!!s.explicacionIa, evaluada && !final) },
      {
        clave: 'core', etiqueta: s.estado === 'RECHAZADA' ? 'Sin desembolso' : 'Core SOAP', icono: 'account_balance',
        estado: estadoDe(final, evaluada && !final),
      },
    ];
  });

  protected verAmortizacion(s: SolicitudDetalle): void {
    this.dialog.open(AmortizacionDialogComponent, {
      data: s, width: '880px', maxWidth: '96vw', maxHeight: '90vh',
      panelClass: 'dialogo-crediflow', backdropClass: 'fondo-desenfocado',
    });
  }

  protected verOriginal(s: SolicitudDetalle): void {
    this.dialog.open(OriginalDialogComponent, {
      data: s, width: '640px', maxWidth: '96vw',
      panelClass: 'dialogo-crediflow', backdropClass: 'fondo-desenfocado',
    });
  }

  protected reintentar(s: SolicitudDetalle): void {
    this.reintentando.set(true);
    this.servicio.reintentar(s.id).subscribe({
      next: () => {
        this.reintentando.set(false);
        this.notificacion.exito(`Solicitud #${s.id} enviada de nuevo a evaluación`);
        this.recarga.update(n => n + 1);
      },
      error: () => this.reintentando.set(false),
    });
  }
}
