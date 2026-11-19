import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly snack = inject(MatSnackBar);

  exito(mensaje: string): void {
    this.snack.open(mensaje, 'OK', { duration: 3500, panelClass: 'snack-exito' });
  }

  error(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 6000, panelClass: 'snack-error' });
  }
}
