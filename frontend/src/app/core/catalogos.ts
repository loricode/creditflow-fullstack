import { EstadoSolicitud, NivelRiesgo, TipoProducto } from './models/solicitud.models';

export interface InfoProducto {
  valor: TipoProducto;
  etiqueta: string;
  icono: string;
  descripcion: string;
  plazoMaximo: number;
}

export const PRODUCTOS: InfoProducto[] = [
  { valor: 'LIBRE_INVERSION', etiqueta: 'Libre inversión', icono: 'payments', descripcion: 'Para lo que necesites', plazoMaximo: 84 },
  { valor: 'VEHICULO', etiqueta: 'Vehículo', icono: 'directions_car', descripcion: 'Nuevo o usado', plazoMaximo: 84 },
  { valor: 'VIVIENDA', etiqueta: 'Vivienda', icono: 'home', descripcion: 'Compra de casa o apartamento', plazoMaximo: 360 },
  { valor: 'EDUCACION', etiqueta: 'Educación', icono: 'school', descripcion: 'Pregrado, posgrado o cursos', plazoMaximo: 120 },
];

export const PRODUCTO_POR_VALOR: Record<TipoProducto, InfoProducto> =
  Object.fromEntries(PRODUCTOS.map(p => [p.valor, p])) as Record<TipoProducto, InfoProducto>;

export interface InfoEstado {
  etiqueta: string;
  icono: string;
  clase: string;
}

export const ESTADOS: Record<EstadoSolicitud, InfoEstado> = {
  RECIBIDA: { etiqueta: 'Recibida', icono: 'inbox', clase: 'recibida' },
  EN_EVALUACION: { etiqueta: 'En evaluación', icono: 'autorenew', clase: 'evaluacion' },
  APROBADA: { etiqueta: 'Aprobada', icono: 'verified', clase: 'aprobada' },
  RECHAZADA: { etiqueta: 'Rechazada', icono: 'block', clase: 'rechazada' },
  ERROR: { etiqueta: 'Error', icono: 'error', clase: 'error' },
};

export const RIESGOS: Record<NivelRiesgo, { etiqueta: string; clase: string }> = {
  BAJO: { etiqueta: 'Riesgo bajo', clase: 'bajo' },
  MEDIO: { etiqueta: 'Riesgo medio', clase: 'medio' },
  ALTO: { etiqueta: 'Riesgo alto', clase: 'alto' },
};
