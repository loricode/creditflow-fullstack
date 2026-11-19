import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ESTADOS } from '../../core/catalogos';
import { EstadoSolicitud } from '../../core/models/solicitud.models';
import { pulsoCambio } from '../animations';

@Component({
  selector: 'app-estado-chip',
  imports: [MatIconModule],
  animations: [pulsoCambio],
  template: `
    <span class="chip" [class]="'chip ' + info().clase" [@pulsoCambio]="estado()">
      <mat-icon [class.girando]="estado() === 'EN_EVALUACION'">{{ info().icono }}</mat-icon>
      {{ info().etiqueta }}
    </span>
  `,
  styles: `
    .chip {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 4px 12px 4px 8px; border-radius: 999px;
      font-size: 12.5px; font-weight: 600; letter-spacing: .2px; white-space: nowrap;
    }
    mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .recibida   { background: #e0f2fe; color: #075985; }
    .evaluacion { background: #fef3c7; color: #92400e; }
    .aprobada   { background: #d1fae5; color: #065f46; }
    .rechazada  { background: #ffe4e6; color: #9f1239; }
    .error      { background: #fee2e2; color: #991b1b; box-shadow: inset 0 0 0 1px #fca5a5; }
    .girando { animation: girar 1.2s linear infinite; }
    @keyframes girar { to { transform: rotate(360deg); } }
  `,
})
export class EstadoChipComponent {
  readonly estado = input.required<EstadoSolicitud>();
  protected readonly info = computed(() => ESTADOS[this.estado()]);
}
