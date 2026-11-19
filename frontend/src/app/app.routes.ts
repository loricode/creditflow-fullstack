import { Routes } from '@angular/router';

// Cada pantalla se carga bajo demanda (lazy loading)
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panel' },
  {
    path: 'panel',
    title: 'Panel · CrediFlow',
    loadComponent: () => import('./features/panel/panel.component').then(m => m.PanelComponent),
  },
  {
    path: 'solicitudes',
    title: 'Solicitudes · CrediFlow',
    loadComponent: () => import('./features/solicitudes/solicitudes.component').then(m => m.SolicitudesComponent),
  },
  {
    path: 'solicitudes/:id',
    title: 'Detalle · CrediFlow',
    loadComponent: () => import('./features/detalle/detalle.component').then(m => m.DetalleComponent),
  },
  {
    path: 'simulador',
    title: 'Simulador · CrediFlow',
    loadComponent: () => import('./features/simulador/simulador.component').then(m => m.SimuladorComponent),
  },
  { path: '**', redirectTo: 'panel' },
];
