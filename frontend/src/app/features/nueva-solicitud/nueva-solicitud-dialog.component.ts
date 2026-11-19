import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSliderModule } from '@angular/material/slider';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, startWith, switchMap } from 'rxjs';
import { PRODUCTOS, PRODUCTO_POR_VALOR } from '../../core/catalogos';
import { CrearSolicitud, ProblemDetail, Simulacion, TipoProducto } from '../../core/models/solicitud.models';
import { AnaliticaService } from '../../core/services/analitica.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { aparecer } from '../../shared/animations';
import { CopPipe } from '../../shared/pipes/cop.pipe';

const PASO_POR_CAMPO: Record<string, number> = {
  nombreCompleto: 0, numeroDocumento: 0, email: 0,
  ingresosMensuales: 1, gastosMensuales: 1,
  tipoProducto: 2, monto: 2, plazoMeses: 2,
};

@Component({
  selector: 'app-nueva-solicitud-dialog',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatStepperModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSliderModule, MatProgressSpinnerModule, CopPipe, DecimalPipe,
  ],
  animations: [aparecer],
  templateUrl: './nueva-solicitud-dialog.component.html',
  styleUrl: './nueva-solicitud-dialog.component.scss',
})
export class NuevaSolicitudDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<NuevaSolicitudDialogComponent, number>);
  private readonly solicitudes = inject(SolicitudesService);
  private readonly analitica = inject(AnaliticaService);

  /** Clave de idempotencia: reenviar (doble clic, reintento de red) no duplica la solicitud. */
  private readonly externalId = crypto.randomUUID();

  protected readonly productos = PRODUCTOS;
  protected readonly enviando = signal(false);
  protected readonly creada = signal<{ id: number; repetida: boolean } | null>(null);
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly datos = this.fb.nonNullable.group({
    nombreCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    numeroDocumento: ['', [Validators.required, Validators.pattern(/^\d{6,12}$/)]],
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly finanzas = this.fb.nonNullable.group({
    ingresosMensuales: [6_500_000, [Validators.required, Validators.min(500_000)]],
    gastosMensuales: [2_200_000, [Validators.required, Validators.min(0)]],
  });

  protected readonly credito = this.fb.nonNullable.group({
    tipoProducto: ['LIBRE_INVERSION' as TipoProducto, Validators.required],
    monto: [25_000_000, [Validators.required, Validators.min(1_000_000), Validators.max(2_000_000_000)]],
    plazoMeses: [48, [Validators.required, Validators.min(6), Validators.max(360)]],
  });

  private readonly finanzasValor = toSignal(this.finanzas.valueChanges.pipe(startWith(this.finanzas.getRawValue())),
    { initialValue: this.finanzas.getRawValue() });
  protected readonly creditoValor = toSignal(this.credito.valueChanges.pipe(startWith(this.credito.getRawValue())),
    { initialValue: this.credito.getRawValue() });

  protected readonly capacidad = computed(() => {
    const f = this.finanzasValor();
    return Math.max((f.ingresosMensuales ?? 0) - (f.gastosMensuales ?? 0), 0);
  });

  protected readonly endeudamiento = computed(() => {
    const f = this.finanzasValor();
    return f.ingresosMensuales ? Math.min((f.gastosMensuales ?? 0) / f.ingresosMensuales * 100, 100) : 0;
  });

  protected readonly plazoMaximo = computed(() => PRODUCTO_POR_VALOR[this.creditoValor().tipoProducto ?? 'LIBRE_INVERSION'].plazoMaximo);

  /** Simulación en vivo contra PKG_CREDITO (debounce para no saturar la API). */
  protected readonly simulacion = toSignal(
    this.credito.valueChanges.pipe(
      startWith(this.credito.getRawValue()),
      debounceTime(350),
      map(() => this.credito.getRawValue()),
      filter(() => this.credito.valid),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      switchMap(v => this.analitica.simular(v.tipoProducto, v.monto, v.plazoMeses).pipe(catchError(() => of(null)))),
    ),
    { initialValue: null as Simulacion | null },
  );

  /** Porcentaje de la capacidad de pago que ocuparía la cuota (regla del 40 %). */
  protected readonly carga = computed(() => {
    const s = this.simulacion();
    const cap = this.capacidad();
    if (!s || cap <= 0) return 100;
    return Math.min(s.cuotaMensual / cap * 100, 100);
  });

  protected elegirProducto(producto: TipoProducto): void {
    const max = PRODUCTO_POR_VALOR[producto].plazoMaximo;
    this.credito.patchValue({
      tipoProducto: producto,
      plazoMeses: Math.min(this.credito.controls.plazoMeses.value, max),
    });
  }

  protected enviar(stepper: MatStepper): void {
    if (this.datos.invalid || this.finanzas.invalid || this.credito.invalid || this.enviando()) return;
    this.enviando.set(true);
    this.errorGeneral.set(null);

    const solicitud: CrearSolicitud = {
      externalId: this.externalId,
      ...this.datos.getRawValue(),
      ...this.finanzas.getRawValue(),
      ...this.credito.getRawValue(),
    };

    this.solicitudes.registrar(solicitud).subscribe({
      next: resp => {
        this.enviando.set(false);
        this.creada.set({ id: resp.body!.id, repetida: resp.status === 200 });
      },
      error: (err: HttpErrorResponse) => {
        this.enviando.set(false);
        if (err.status === 400) {
          this.mostrarErroresDelServidor(err.error as ProblemDetail, stepper);
        } else {
          this.errorGeneral.set('No se pudo registrar la solicitud. Vuelve a intentarlo: no se duplicará.');
        }
      },
    });
  }

  /** Lleva los errores de validación del backend a los campos y abre el paso correspondiente. */
  private mostrarErroresDelServidor(problema: ProblemDetail, stepper: MatStepper): void {
    const grupos: FormGroup[] = [this.datos, this.finanzas, this.credito];
    let primerPaso = Number.MAX_SAFE_INTEGER;
    Object.entries(problema.errores ?? {}).forEach(([campo, mensaje]) => {
      const paso = PASO_POR_CAMPO[campo];
      if (paso === undefined) return;
      grupos[paso].get(campo)?.setErrors({ servidor: mensaje });
      grupos[paso].get(campo)?.markAsTouched();
      primerPaso = Math.min(primerPaso, paso);
    });
    if (primerPaso !== Number.MAX_SAFE_INTEGER) {
      stepper.selectedIndex = primerPaso;
    }
    this.errorGeneral.set(problema.detail);
  }

  protected verSolicitud(): void {
    this.ref.close(this.creada()?.id);
  }

  protected formatearPesos = (valor: number) =>
    valor >= 1_000_000 ? `${Math.round(valor / 1_000_000)}M` : `${Math.round(valor / 1000)}k`;
}
