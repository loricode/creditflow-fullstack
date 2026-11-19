import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificacionService } from '../services/notificacion.service';

/**
 * Errores de red y de servidor se notifican en un solo lugar.
 * Los 400 (validación) los maneja cada formulario para marcar sus campos.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificacion = inject(NotificacionService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        notificacion.error('No hay conexión con la API. ¿Está corriendo el backend?');
      } else if (error.status >= 500) {
        notificacion.error('El servidor tuvo un problema. Intenta de nuevo en unos segundos.');
      } else if (error.status === 404 || error.status === 409) {
        notificacion.error(error.error?.detail ?? 'Operación no disponible');
      }
      return throwError(() => error);
    }),
  );
};
