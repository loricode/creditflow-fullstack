import { JsonPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of } from 'rxjs';
import { SolicitudDetalle } from '../../core/models/solicitud.models';
import { SolicitudesService } from '../../core/services/solicitudes.service';

@Component({
  selector: 'app-original-dialog',
  imports: [JsonPipe, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="cabecera">
      <div>
        <span class="etiqueta"><mat-icon>cloud</mat-icon> Amazon S3 · cifrado AES-256</span>
        <h2 class="mono">{{ s.s3Key }}</h2>
      </div>
      <button mat-icon-button mat-dialog-close aria-label="Cerrar"><mat-icon>close</mat-icon></button>
    </div>
    <mat-dialog-content>
      @if (original(); as doc) {
        <pre class="json" data-tour="original-json">{{ doc | json }}</pre>
      } @else {
        <div class="skeleton" style="height: 240px"></div>
      }
      <p class="texto-suave nota">
        Particionado estilo Hive (<span class="mono">anio=/mes=/producto=</span>) para consultarlo con Athena sin mover datos.
      </p>
    </mat-dialog-content>
  `,
  styles: `
    .cabecera { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; padding: 22px 24px; color: #fff; background: var(--cf-degradado);
      h2 { margin: 6px 0 0; font-size: 13px; word-break: break-all; font-weight: 500; opacity: .95; } button { color: #fff; }
      .etiqueta { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; font-weight: 700; opacity: .85;
        mat-icon { font-size: 18px; width: 18px; height: 18px; } } }
    .json { margin: 16px 0 8px; padding: 18px; border-radius: 16px; background: #083344; color: #a5f3fc;
      font-family: 'JetBrains Mono', monospace; font-size: 13px; line-height: 1.6; overflow: auto; max-height: 50vh; }
    .nota { font-size: 12.5px; }
  `,
})
export class OriginalDialogComponent {
  protected readonly s = inject<SolicitudDetalle>(MAT_DIALOG_DATA);
  protected readonly original = toSignal(inject(SolicitudesService).original(this.s.id).pipe(catchError(() => of({ error: 'No disponible' }))));
}
