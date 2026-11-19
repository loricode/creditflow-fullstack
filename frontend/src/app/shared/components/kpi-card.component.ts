import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ContadorDirective } from '../directives/contador.directive';

@Component({
  selector: 'app-kpi-card',
  imports: [MatIconModule, ContadorDirective],
  template: `
    <article class="kpi" [class]="'kpi ' + tono()">
      <div class="icono"><mat-icon>{{ icono() }}</mat-icon></div>
      <div class="texto">
        <span class="titulo">{{ titulo() }}</span>
        <strong class="valor" [appContador]="valor()" [formato]="formato()"></strong>
        @if (detalle()) { <span class="detalle">{{ detalle() }}</span> }
      </div>
      <div class="brillo"></div>
    </article>
  `,
  styles: `
    .kpi {
      position: relative; overflow: hidden;
      display: flex; align-items: center; gap: 16px;
      padding: 20px; border-radius: 22px; color: #fff;
      box-shadow: 0 12px 30px -12px rgba(4, 47, 46, .45);
      transition: transform .25s ease, box-shadow .25s ease;
    }
    .kpi:hover { transform: translateY(-4px); box-shadow: 0 20px 40px -14px rgba(4, 47, 46, .55); }
    .teal  { background: linear-gradient(135deg, #0f766e, #14b8a6); }
    .cyan  { background: linear-gradient(135deg, #0e7490, #06b6d4); }
    .verde { background: linear-gradient(135deg, #047857, #34d399); }
    .ambar { background: linear-gradient(135deg, #b45309, #f59e0b); }
    .icono {
      flex: none; display: grid; place-items: center;
      width: 52px; height: 52px; border-radius: 16px;
      background: rgba(255, 255, 255, .18); backdrop-filter: blur(4px);
    }
    .icono mat-icon { font-size: 28px; width: 28px; height: 28px; }
    .texto { display: flex; flex-direction: column; min-width: 0; }
    .titulo { font-size: 13px; opacity: .9; font-weight: 500; }
    .valor { font-size: 27px; font-weight: 700; line-height: 1.2; letter-spacing: -.5px; }
    .detalle { font-size: 12px; opacity: .85; }
    .brillo {
      position: absolute; right: -40px; top: -40px; width: 140px; height: 140px; border-radius: 50%;
      background: radial-gradient(circle, rgba(255,255,255,.28), transparent 70%);
      pointer-events: none;
    }
  `,
})
export class KpiCardComponent {
  readonly titulo = input.required<string>();
  readonly valor = input.required<number>();
  readonly icono = input('insights');
  readonly tono = input<'teal' | 'cyan' | 'verde' | 'ambar'>('teal');
  readonly formato = input<'entero' | 'cop' | 'porcentaje'>('entero');
  readonly detalle = input<string>();
}
