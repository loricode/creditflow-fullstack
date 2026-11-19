import { CopPipe } from './cop.pipe';

describe('CopPipe', () => {
  const pipe = new CopPipe();

  it('formatea pesos colombianos sin decimales', () => {
    expect(pipe.transform(1500000).replace(/\s/g, ' ')).toBe('$ 1.500.000');
  });

  it('usa notación compacta para montos grandes', () => {
    expect(pipe.transform(30000000, 'compacto')).toContain('30');
  });

  it('muestra un guion si no hay valor', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
  });
});
