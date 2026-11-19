import { Pipe, PipeTransform } from '@angular/core';

const FORMATO = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const COMPACTO = new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 });

/** Pesos colombianos: `1500000 | cop` → "$ 1.500.000"; `| cop:'compacto'` → "$1,5 M". */
@Pipe({ name: 'cop' })
export class CopPipe implements PipeTransform {
  transform(valor: number | null | undefined, modo: 'completo' | 'compacto' = 'completo'): string {
    if (valor === null || valor === undefined || Number.isNaN(valor)) return '—';
    return modo === 'compacto' ? `$${COMPACTO.format(valor)}` : FORMATO.format(valor);
  }
}
