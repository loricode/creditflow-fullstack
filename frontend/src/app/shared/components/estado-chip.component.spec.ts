import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { EstadoChipComponent } from './estado-chip.component';

describe('EstadoChipComponent', () => {
  let fixture: ComponentFixture<EstadoChipComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EstadoChipComponent], providers: [provideNoopAnimations()] });
    fixture = TestBed.createComponent(EstadoChipComponent);
  });

  it('muestra la etiqueta en español y la clase del estado', () => {
    fixture.componentRef.setInput('estado', 'APROBADA');
    fixture.detectChanges();
    const chip: HTMLElement = fixture.nativeElement.querySelector('.chip');
    expect(chip.textContent).toContain('Aprobada');
    expect(chip.classList).toContain('aprobada');
  });

  it('anima el ícono mientras la solicitud está en evaluación', () => {
    fixture.componentRef.setInput('estado', 'EN_EVALUACION');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-icon').classList).toContain('girando');
  });
});
