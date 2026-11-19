import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { SolicitudDetalle } from '../../core/models/solicitud.models';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { CopPipe } from '../../shared/pipes/cop.pipe';

@Component({
  selector: 'app-amortizacion-dialog',
  imports: [DatePipe, DecimalPipe, MatDialogModule, MatTableModule, MatButtonModule, MatIconModule, CopPipe],
  template: `
    <div class="cabecera">
      <div>
        <span class="etiqueta">Tabla de amortización · sistema francés</span>
        <h2>{{ s.monto | cop }} a {{ s.plazoMeses }} meses · {{ s.tasaAnual | number: '1.1-2' }} % E.A.</h2>
      </div>
      <button mat-icon-button mat-dialog-close aria-label="Cerrar"><mat-icon>close</mat-icon></button>
    </div>

    <div class="resumen" data-tour="amortizacion-resumen">
      <div><span>Cuota fija</span><strong>{{ s.cuotaMensual | cop }}</strong></div>
      <div><span>Total intereses</span><strong>{{ totalIntereses() | cop }}</strong></div>
      <div><span>Total a pagar</span><strong>{{ totalPagado() | cop }}</strong></div>
    </div>

    <mat-dialog-content class="contenido">
      @if (cuotas(); as lista) {
        <table mat-table [dataSource]="lista">
          <ng-container matColumnDef="n"><th mat-header-cell *matHeaderCellDef>#</th><td mat-cell *matCellDef="let c">{{ c.numeroCuota }}</td></ng-container>
          <ng-container matColumnDef="fecha"><th mat-header-cell *matHeaderCellDef>Fecha</th><td mat-cell *matCellDef="let c">{{ c.fechaPago | date: 'MMM y' }}</td></ng-container>
          <ng-container matColumnDef="cuota"><th mat-header-cell *matHeaderCellDef>Cuota</th><td mat-cell *matCellDef="let c">{{ c.cuota | cop }}</td></ng-container>
          <ng-container matColumnDef="capital">
            <th mat-header-cell *matHeaderCellDef>Capital</th>
            <td mat-cell *matCellDef="let c">
              <div class="celda-barra">
                <span class="mini"><i [style.width.%]="c.capital / c.cuota * 100"></i></span>{{ c.capital | cop }}
              </div>
            </td>
          </ng-container>
          <ng-container matColumnDef="interes"><th mat-header-cell *matHeaderCellDef>Interés</th><td mat-cell *matCellDef="let c">{{ c.interes | cop }}</td></ng-container>
          <ng-container matColumnDef="saldo"><th mat-header-cell *matHeaderCellDef>Saldo</th><td mat-cell *matCellDef="let c"><strong>{{ c.saldo | cop }}</strong></td></ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas; sticky: true"></tr>
          <tr mat-row *matRowDef="let c; columns: columnas"></tr>
        </table>
      } @else {
        <div class="skeleton" style="height: 300px"></div>
      }
    </mat-dialog-content>
    <p class="pie texto-suave">Generada por <span class="mono">PKG_CREDITO.GENERAR_AMORTIZACION</span> con FORALL (inserción masiva)</p>
  `,
  styles: `
    .cabecera { display: flex; justify-content: space-between; align-items: flex-start; padding: 22px 24px; color: #fff; background: var(--cf-degradado);
      h2 { margin: 4px 0 0; font-size: 19px; font-weight: 800; } button { color: #fff; }
      .etiqueta { font-size: 12px; text-transform: uppercase; letter-spacing: 1px; opacity: .8; font-weight: 700; } }
    .resumen { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding: 16px 24px 4px;
      div { background: var(--cf-pri-50); border-radius: 16px; padding: 12px 14px; display: flex; flex-direction: column; }
      span { font-size: 12.5px; color: var(--cf-texto-suave); } strong { font-size: 18px; color: var(--cf-pri-800); } }
    /* sin padding superior: la cabecera fija queda pegada al borde y no asoman filas por encima */
    .contenido { max-height: 52vh !important; padding-top: 0 !important; margin-top: 12px; }
    table { width: 100%; }
    .celda-barra { display: flex; align-items: center; gap: 8px; }
    .mini { display: inline-block; width: 46px; height: 6px; border-radius: 6px; background: #ffe4cc; overflow: hidden;
      i { display: block; height: 100%; background: linear-gradient(90deg, #0891b2, #06b6d4); } }
    .pie { margin: 0; padding: 8px 24px 16px; font-size: 12px; }
    @media (max-width: 600px) { .resumen { grid-template-columns: 1fr; } }
  `,
})
export class AmortizacionDialogComponent {
  protected readonly s = inject<SolicitudDetalle>(MAT_DIALOG_DATA);
  private readonly servicio = inject(SolicitudesService);

  protected readonly columnas = ['n', 'fecha', 'cuota', 'capital', 'interes', 'saldo'];
  protected readonly cuotas = toSignal(this.servicio.amortizacion(this.s.id));
  protected readonly totalIntereses = computed(() => (this.cuotas() ?? []).reduce((t, c) => t + c.interes, 0));
  protected readonly totalPagado = computed(() => (this.cuotas() ?? []).reduce((t, c) => t + c.cuota, 0));
}
