import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-vacio',
  imports: [MatIconModule],
  template: `
    <div class="vacio">
      <div class="burbuja"><mat-icon>{{ icono() }}</mat-icon></div>
      <h3>{{ titulo() }}</h3>
      <p>{{ mensaje() }}</p>
      <ng-content />
    </div>
  `,
  styles: `
    .vacio { text-align: center; padding: 48px 16px; color: var(--cf-texto-suave); }
    .burbuja {
      display: inline-grid; place-items: center; width: 76px; height: 76px; border-radius: 50%;
      background: var(--cf-pri-50); color: var(--cf-pri-600); margin-bottom: 12px;
      animation: flotar 3s ease-in-out infinite;
    }
    .burbuja mat-icon { font-size: 38px; width: 38px; height: 38px; }
    h3 { margin: 4px 0; color: var(--cf-texto); font-weight: 600; }
    p { margin: 0 0 16px; }
    @keyframes flotar { 50% { transform: translateY(-6px); } }
  `,
})
export class VacioComponent {
  readonly icono = input('inbox');
  readonly titulo = input('Sin datos');
  readonly mensaje = input('');
}
