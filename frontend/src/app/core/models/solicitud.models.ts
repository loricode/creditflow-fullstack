export type EstadoSolicitud = 'RECIBIDA' | 'EN_EVALUACION' | 'APROBADA' | 'RECHAZADA' | 'ERROR';
export type TipoProducto = 'LIBRE_INVERSION' | 'VEHICULO' | 'VIVIENDA' | 'EDUCACION';
export type NivelRiesgo = 'BAJO' | 'MEDIO' | 'ALTO';

export interface CrearSolicitud {
  externalId: string;
  numeroDocumento: string;
  nombreCompleto: string;
  email: string;
  ingresosMensuales: number;
  gastosMensuales: number;
  tipoProducto: TipoProducto;
  monto: number;
  plazoMeses: number;
}

export interface SolicitudResumen {
  id: number;
  externalId: string;
  nombreCompleto: string;
  numeroDocumento: string;
  tipoProducto: TipoProducto;
  monto: number;
  plazoMeses: number;
  estado: EstadoSolicitud;
  score: number | null;
  nivelRiesgo: NivelRiesgo | null;
  creadoEn: string;
}

export interface HistorialEstado {
  estadoAnterior: EstadoSolicitud | null;
  estadoNuevo: EstadoSolicitud;
  cambiadoEn: string;
}

export interface IntentoEvaluacion {
  numeroIntento: number;
  resultado: 'EXITO' | 'FALLO';
  error: string | null;
  duracionMs: number;
  creadoEn: string;
}

export interface SolicitudDetalle extends SolicitudResumen {
  email: string;
  ingresosMensuales: number;
  gastosMensuales: number;
  tasaAnual: number | null;
  cuotaMensual: number | null;
  explicacionIa: string | null;
  proveedorIa: 'claude' | 'reglas' | null;
  referenciaCore: string | null;
  s3Key: string | null;
  ultimoError: string | null;
  intentos: number;
  actualizadoEn: string;
  evaluadoEn: string | null;
  historial: HistorialEstado[];
  historialIntentos: IntentoEvaluacion[];
}

export interface Cuota {
  numeroCuota: number;
  fechaPago: string;
  cuota: number;
  capital: number;
  interes: number;
  saldo: number;
}

export interface Simulacion {
  producto: TipoProducto;
  monto: number;
  plazoMeses: number;
  tasaAnual: number;
  cuotaMensual: number;
  totalPagar: number;
  totalIntereses: number;
}

export interface Estadisticas {
  total: number;
  aprobadas: number;
  rechazadas: number;
  enProceso: number;
  errores: number;
  montoAprobado: number;
  scorePromedio: number;
  tasaAprobacion: number;
  porProducto: { producto: TipoProducto; total: number; aprobadas: number; montoAprobado: number }[];
  ultimosDias: { dia: string; total: number; aprobadas: number }[];
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface FiltroSolicitudes {
  estado?: EstadoSolicitud | null;
  producto?: TipoProducto | null;
  q?: string | null;
  pagina: number;
  tamano: number;
  orden?: string;
  direccion?: 'asc' | 'desc';
}

/** Error RFC 9457 que devuelve la API. */
export interface ProblemDetail {
  title: string;
  status: number;
  detail: string;
  errores?: Record<string, string>;
}

export const ESTADOS_FINALES: EstadoSolicitud[] = ['APROBADA', 'RECHAZADA'];
export const ESTADOS_EN_PROCESO: EstadoSolicitud[] = ['RECIBIDA', 'EN_EVALUACION'];
