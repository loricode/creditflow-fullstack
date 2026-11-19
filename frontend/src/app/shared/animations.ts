// Las transiciones entre páginas usan la View Transitions API (withViewTransitions en app.config).
import { animate, keyframes, query, stagger, style, transition, trigger } from '@angular/animations';

/** Entrada suave hacia arriba. */
export const aparecer = trigger('aparecer', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(12px)' }),
    animate('380ms cubic-bezier(.2,.8,.2,1)', style({ opacity: 1, transform: 'none' })),
  ]),
]);

/** Los hijos con la clase .escalonado entran uno tras otro. */
export const listaEscalonada = trigger('listaEscalonada', [
  transition('* => *', [
    query('.escalonado:enter', [
      style({ opacity: 0, transform: 'translateY(16px) scale(.98)' }),
      stagger(70, animate('420ms cubic-bezier(.2,.8,.2,1)', style({ opacity: 1, transform: 'none' }))),
    ], { optional: true }),
  ]),
]);

/** Rebote al cambiar de estado (p. ej. EN_EVALUACION → APROBADA). */
export const pulsoCambio = trigger('pulsoCambio', [
  transition('* => *', [
    animate('600ms', keyframes([
      style({ transform: 'scale(1)', offset: 0 }),
      style({ transform: 'scale(1.12)', offset: 0.35 }),
      style({ transform: 'scale(1)', offset: 1 }),
    ])),
  ]),
]);
