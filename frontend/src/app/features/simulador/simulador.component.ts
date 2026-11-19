import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';
import { Router } from '@angular/router';
import { catchError, debounceTime, of, switchMap } from 'rxjs';
import { PRODUCTOS, PRODUCTO_POR_VALOR } from '../../core/catalogos';
import { TipoProducto } from '../../core/models/solicitud.models';
import { AnaliticaService } from '../../core/services/analitica.service';
import { aparecer } from '../../shared/animations';
import { ContadorDirective } from '../../shared/directives/contador.directive';
import { CopPipe } from '../../shared/pipes/cop.pipe';
import { NuevaSolicitudService } from '../nueva-solicitud/nueva-solicitud.service';

@Component({
  selector: 'app-simulador',
  imports: [DecimalPipe, FormsModule, MatSliderModule, MatButtonModule, MatIconModule, CopPipe, ContadorDirective],
  animations: [aparecer],
  template: `
    <section class="pagina">
      <header class="encabezado-pagina" @aparecer>
        <div>
          <h1>Simulador de cuota</h1>
          <p>Cálculo en vivo con <span class="mono">PKG_CREDITO.CALCULAR_CUOTA</span>, el mismo motor que evalúa las solicitudes.</p>
        </div>
      </header>

      <div class="rejilla">
        <article class="tarjeta" @aparecer>
          <h2 class="tarjeta-titulo"><mat-icon>tune</mat-icon> Elige tu crédito</h2>
          <div class="productos">
            @for (p of productos; track p.valor) {
              <button type="button" class="producto" [attr.data-tour]="'sim-' + p.valor" [class.elegido]="producto() === p.valor" (click)="elegir(p.valor)">
                <mat-icon>{{ p.icono }}</mat-icon><span>{{ p.etiqueta }}</span>
              </button>
            }
          </div>

          <div class="control">
            <div class="fila"><span>Monto</span><strong>{{ monto() | cop }}</strong></div>
            <mat-slider min="1000000" max="500000000" step="1000000">
              <input matSliderThumb [ngModel]="monto()" (ngModelChange)="monto.set($event)">
            </mat-slider>
          </div>
          <div class="control">
            <div class="fila"><span>Plazo</span><strong>{{ plazo() }} meses</strong></div>
            <mat-slider min="6" [max]="plazoMaximo()" step="6">
              <input matSliderThumb [ngModel]="plazo()" (ngModelChange)="plazo.set($event)">
            </mat-slider>
          </div>
        </article>

        <article class="resultado" data-tour="sim-resultado" @aparecer>
          @if (simulacion(); as s) {
            <span class="etiqueta">Cuota mensual estimada</span>
            <strong class="cuota" [appContador]="s.cuotaMensual" formato="cop" [duracion]="500"></strong>
            <div class="detalles">
              <div><span>Tasa base</span><b>{{ s.tasaAnual | number: '1.1-2' }} % E.A.</b></div>
              <div><span>Total intereses</span><b>{{ s.totalIntereses | cop }}</b></div>
              <div><span>Total a pagar</span><b>{{ s.totalPagar | cop }}</b></div>
            </div>
            <div class="proporcion" aria-label="Capital frente a intereses">
              <i class="capital" [style.width.%]="s.monto / s.totalPagar * 100"></i>
              <i class="interes"></i>
            </div>
            <div class="leyenda"><span><i class="capital"></i> Capital</span><span><i class="interes"></i> Intereses</span></div>
            <p class="nota">La tasa final depende del riesgo: +3 puntos si es medio, +6 si es alto (tope legal 28 %).</p>
            <button mat-flat-button class="boton-blanco" data-tour="sim-solicitar" (click)="solicitar()"><mat-icon>rocket_launch</mat-icon> Solicitar este crédito</button>
          } @else {
            <div class="skeleton" style="height: 220px; opacity: .3"></div>
          }
        </article>
      </div>
    </section>
  `,
  styles: `
    .rejilla { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: stretch; }
    .productos { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 18px; }
    .producto {
      display: flex; align-items: center; gap: 10px; padding: 14px; border-radius: 16px; cursor: pointer; font: inherit; font-weight: 600;
      background: #fff; border: 2px solid var(--cf-borde); color: var(--cf-texto); transition: all .2s ease;
      mat-icon { color: var(--cf-pri-600); }
      &:hover { border-color: var(--cf-pri-400); transform: translateY(-2px); }
      &.elegido { border-color: var(--cf-pri-600); background: var(--cf-pri-50); box-shadow: 0 10px 22px -14px rgba(8, 145, 178,.9); }
    }
    .control { margin-top: 8px; mat-slider { width: 100%; } }
    .fila { display: flex; justify-content: space-between; font-weight: 600; strong { color: var(--cf-pri-700); font-size: 18px; } }
    .resultado {
      border-radius: 26px; padding: 30px; color: #fff; display: flex; flex-direction: column;
      background: radial-gradient(500px 260px at 100% 0%, rgba(45, 212, 191,.35), transparent 60%), var(--cf-degradado);
      box-shadow: 0 24px 50px -28px rgba(8, 51, 68,.9);
    }
    .etiqueta { opacity: .85; font-weight: 600; }
    .cuota { font-size: 46px; font-weight: 800; letter-spacing: -1.5px; margin: 4px 0 18px; }
    .detalles { display: grid; gap: 10px; div { display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid rgba(255,255,255,.15); } span { opacity: .8; } }
    .proporcion { display: flex; height: 14px; border-radius: 14px; overflow: hidden; margin: 22px 0 8px; background: rgba(255,255,255,.15);
      .capital { background: #67e8f9; transition: width .6s cubic-bezier(.2,.8,.2,1); } .interes { flex: 1; background: #fbbf24; } }
    .leyenda { display: flex; gap: 16px; font-size: 12.5px; opacity: .9;
      i { display: inline-block; width: 10px; height: 10px; border-radius: 3px; margin-right: 4px; }
      .capital { background: #67e8f9; } .interes { background: #fbbf24; } }
    .nota { font-size: 12.5px; opacity: .75; margin: 16px 0; }
    .boton-blanco.mat-mdc-unelevated-button { margin-top: auto; align-self: flex-start; background: #fff !important; color: var(--cf-pri-800) !important; border-radius: 14px; height: 46px; font-weight: 700; }
    @media (max-width: 900px) { .rejilla { grid-template-columns: 1fr; } }
  `,
})
export class SimuladorComponent {
  private readonly analitica = inject(AnaliticaService);
  private readonly nuevaSolicitud = inject(NuevaSolicitudService);
  private readonly router = inject(Router);

  protected readonly productos = PRODUCTOS;
  protected readonly producto = signal<TipoProducto>('VEHICULO');
  protected readonly monto = signal(45_000_000);
  protected readonly plazo = signal(60);
  protected readonly plazoMaximo = computed(() => PRODUCTO_POR_VALOR[this.producto()].plazoMaximo);

  protected readonly simulacion = toSignal(
    toObservable(computed(() => ({ p: this.producto(), m: this.monto(), n: this.plazo() }))).pipe(
      debounceTime(250),
      switchMap(({ p, m, n }) => this.analitica.simular(p, m, n).pipe(catchError(() => of(null)))),
    ),
  );

  protected elegir(p: TipoProducto): void {
    this.producto.set(p);
    this.plazo.update(n => Math.min(n, PRODUCTO_POR_VALOR[p].plazoMaximo));
  }

  protected solicitar(): void {
    this.nuevaSolicitud.abrir().subscribe(id => id && this.router.navigate(['/solicitudes', id]));
  }
}
