import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NuevaSolicitudService } from './features/nueva-solicitud/nueva-solicitud.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly nuevaSolicitud = inject(NuevaSolicitudService);
  private readonly router = inject(Router);

  protected readonly enlaces = [
    { ruta: '/panel', etiqueta: 'Panel', icono: 'space_dashboard' },
    { ruta: '/solicitudes', etiqueta: 'Solicitudes', icono: 'receipt_long' },
    { ruta: '/simulador', etiqueta: 'Simulador', icono: 'calculate' },
  ];

  protected abrirNueva(): void {
    this.nuevaSolicitud.abrir().subscribe(id => {
      if (id) this.router.navigate(['/solicitudes', id]);
    });
  }
}
