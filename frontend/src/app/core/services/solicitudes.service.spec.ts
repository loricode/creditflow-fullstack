import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SolicitudesService } from './solicitudes.service';

describe('SolicitudesService', () => {
  let servicio: SolicitudesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(SolicitudesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía solo los filtros con valor y recorta el texto', () => {
    servicio.listar({ pagina: 1, tamano: 25, estado: 'APROBADA', producto: null, q: '  laura ' }).subscribe();

    const req = http.expectOne(r => r.url === '/api/v1/solicitudes');
    expect(req.request.params.get('estado')).toBe('APROBADA');
    expect(req.request.params.has('producto')).toBeFalse();
    expect(req.request.params.get('q')).toBe('laura');
    expect(req.request.params.get('pagina')).toBe('1');
    req.flush({ contenido: [], pagina: 1, tamano: 25, totalElementos: 0, totalPaginas: 0 });
  });

  it('expone el código HTTP al registrar (201 nueva, 200 repetida)', () => {
    let status = 0;
    servicio.registrar({
      externalId: 'k', numeroDocumento: '123456', nombreCompleto: 'A', email: 'a@b.co',
      ingresosMensuales: 1, gastosMensuales: 0, tipoProducto: 'VEHICULO', monto: 1, plazoMeses: 6,
    }).subscribe(r => status = r.status);

    http.expectOne('/api/v1/solicitudes').flush({ id: 9 }, { status: 200, statusText: 'OK' });
    expect(status).toBe(200);
  });

  it('reintenta por POST', () => {
    servicio.reintentar(3).subscribe();
    const req = http.expectOne('/api/v1/solicitudes/3/reintentar');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
