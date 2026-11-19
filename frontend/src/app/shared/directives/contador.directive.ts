import { Directive, ElementRef, effect, inject, input } from '@angular/core';

/**
 * Anima un número desde su valor anterior hasta el nuevo.
 *   <span [appContador]="total()" formato="cop"></span>
 */
@Directive({ selector: '[appContador]' })
export class ContadorDirective {
  readonly appContador = input.required<number>();
  readonly formato = input<'entero' | 'cop' | 'porcentaje'>('entero');
  readonly duracion = input(900);

  private readonly el = inject(ElementRef<HTMLElement>);
  private actual = 0;
  private frame = 0;

  private static readonly COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  private static readonly ENTERO = new Intl.NumberFormat('es-CO');

  constructor() {
    effect(() => this.animar(this.appContador() ?? 0));
  }

  private animar(destino: number): void {
    cancelAnimationFrame(this.frame);
    const origen = this.actual;
    const inicio = performance.now();
    const dur = this.duracion();
    const paso = (ahora: number) => {
      const t = Math.min((ahora - inicio) / dur, 1);
      const suavizado = 1 - Math.pow(1 - t, 3); // ease-out cúbico
      this.actual = origen + (destino - origen) * suavizado;
      this.el.nativeElement.textContent = this.formatear(this.actual);
      if (t < 1) this.frame = requestAnimationFrame(paso);
    };
    this.frame = requestAnimationFrame(paso);
  }

  private formatear(v: number): string {
    switch (this.formato()) {
      case 'cop': return ContadorDirective.COP.format(Math.round(v));
      case 'porcentaje': return `${v.toFixed(1)} %`;
      default: return ContadorDirective.ENTERO.format(Math.round(v));
    }
  }
}
