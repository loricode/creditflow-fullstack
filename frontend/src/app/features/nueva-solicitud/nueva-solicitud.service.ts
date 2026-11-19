import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { NuevaSolicitudDialogComponent } from './nueva-solicitud-dialog.component';

@Injectable({ providedIn: 'root' })
export class NuevaSolicitudService {
  private readonly dialog = inject(MatDialog);

  /** Abre el asistente; emite el id de la solicitud creada, o undefined si se cancela. */
  abrir(): Observable<number | undefined> {
    return this.dialog.open<NuevaSolicitudDialogComponent, void, number>(NuevaSolicitudDialogComponent, {
      width: '780px',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: 'first-tabbable',
      panelClass: 'dialogo-crediflow',
      backdropClass: 'fondo-desenfocado',
    }).afterClosed();
  }
}
