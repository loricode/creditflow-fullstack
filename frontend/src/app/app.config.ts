import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsCo from '@angular/common/locales/es-CO';
import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  NavigationError, provideRouter, withComponentInputBinding, withNavigationErrorHandler, withViewTransitions,
} from '@angular/router';
import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';

registerLocaleData(localeEsCo);

/**
 * Si se publicó una versión nueva mientras la pestaña seguía abierta, los chunks
 * viejos (con hash) ya no existen y la navegación falla en silencio. En ese caso
 * se recarga la página en la ruta pedida para obtener la versión actual.
 */
export function recargarSiHayVersionNueva(error: NavigationError): void {
  const mensaje = String((error.error as Error)?.message ?? error.error);
  if (/dynamically imported module|Loading chunk|Importing a module script failed/i.test(mensaje)) {
    window.location.assign(error.url);
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding(), withViewTransitions(), withNavigationErrorHandler(recargarSiHayVersionNueva)),
    provideHttpClient(withFetch(), withInterceptors([errorInterceptor])),
    provideAnimationsAsync(),
    { provide: LOCALE_ID, useValue: 'es-CO' },
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-icons-round' } },
  ],
};
