import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CrearSolicitud, Cuota, FiltroSolicitudes, Pagina, SolicitudDetalle, SolicitudResumen,
} from '../models/solicitud.models';

@Injectable({ providedIn: 'root' })
export class SolicitudesService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/solicitudes';

  /** Respuesta completa: 201 = nueva, 200 = ya existía (idempotencia por externalId). */
  registrar(solicitud: CrearSolicitud): Observable<HttpResponse<SolicitudDetalle>> {
    return this.http.post<SolicitudDetalle>(this.base, solicitud, { observe: 'response' });
  }

  listar(filtro: FiltroSolicitudes): Observable<Pagina<SolicitudResumen>> {
    let params = new HttpParams()
      .set('pagina', filtro.pagina)
      .set('tamano', filtro.tamano)
      .set('orden', filtro.orden ?? 'creadoEn')
      .set('direccion', filtro.direccion ?? 'desc');
    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.producto) params = params.set('producto', filtro.producto);
    if (filtro.q?.trim()) params = params.set('q', filtro.q.trim());
    return this.http.get<Pagina<SolicitudResumen>>(this.base, { params });
  }

  detalle(id: number): Observable<SolicitudDetalle> {
    return this.http.get<SolicitudDetalle>(`${this.base}/${id}`);
  }

  amortizacion(id: number): Observable<Cuota[]> {
    return this.http.get<Cuota[]>(`${this.base}/${id}/amortizacion`);
  }

  original(id: number): Observable<unknown> {
    return this.http.get<unknown>(`${this.base}/${id}/original`);
  }

  reintentar(id: number): Observable<SolicitudResumen> {
    return this.http.post<SolicitudResumen>(`${this.base}/${id}/reintentar`, {});
  }
}
