import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificacionService } from '../services/notificacion.service';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let notificacion: jasmine.SpyObj<NotificacionService>;

  beforeEach(() => {
    notificacion = jasmine.createSpyObj('NotificacionService', ['error', 'exito']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificacionService, useValue: notificacion },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  it('notifica errores de servidor', () => {
    http.get('/api/x').subscribe({ error: () => undefined });
    backend.expectOne('/api/x').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(notificacion.error).toHaveBeenCalled();
  });

  it('deja los 400 al formulario (no notifica)', () => {
    http.post('/api/x', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/x').flush({ title: 'Datos inválidos' }, { status: 400, statusText: 'Bad Request' });
    expect(notificacion.error).not.toHaveBeenCalled();
  });

  it('muestra el detalle del ProblemDetail en un 409', () => {
    http.post('/api/x', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/x').flush({ detail: 'Solo se reintentan solicitudes en ERROR' }, { status: 409, statusText: 'Conflict' });
    expect(notificacion.error).toHaveBeenCalledWith('Solo se reintentan solicitudes en ERROR');
  });
});
