import { periodoDoAtalho } from './consolidado';

describe('periodoDoAtalho', () => {
  const quinta = '2026-11-26';

  it('esta semana e a semana passada vão de segunda a domingo', () => {
    expect(periodoDoAtalho('semana', quinta)).toEqual({ inicio: '2026-11-23', fim: '2026-11-29' });
    expect(periodoDoAtalho('semana-passada', quinta)).toEqual({ inicio: '2026-11-16', fim: '2026-11-22' });
  });

  it('este mês e o mês passado vão do primeiro ao último dia', () => {
    expect(periodoDoAtalho('mes', quinta)).toEqual({ inicio: '2026-11-01', fim: '2026-11-30' });
    expect(periodoDoAtalho('mes-passado', quinta)).toEqual({ inicio: '2026-10-01', fim: '2026-10-31' });
    expect(periodoDoAtalho('mes-passado', '2027-01-15')).toEqual({ inicio: '2026-12-01', fim: '2026-12-31' });
  });
});
