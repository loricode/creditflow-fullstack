import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Estadisticas, Simulacion, TipoProducto } from '../models/solicitud.models';

@Injectable({ providedIn: 'root' })
export class AnaliticaService {
  private readonly http = inject(HttpClient);

  estadisticas(): Observable<Estadisticas> {
    return this.http.get<Estadisticas>('/api/v1/estadisticas');
  }

  simular(producto: TipoProducto, monto: number, plazoMeses: number): Observable<Simulacion> {
    const params = new HttpParams().set('producto', producto).set('monto', monto).set('plazoMeses', plazoMeses);
    return this.http.get<Simulacion>('/api/v1/simulador', { params });
  }
}
