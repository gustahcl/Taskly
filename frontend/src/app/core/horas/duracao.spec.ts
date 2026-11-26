import { fimDoMes, formatarDuracao, inicioDaSemana, lerDuracao, nomeDoDia, somarDias } from './duracao';

describe('duração', () => {
  it('formata minutos em horas', () => {
    expect(formatarDuracao(0)).toBe('0h');
    expect(formatarDuracao(45)).toBe('0h45');
    expect(formatarDuracao(90)).toBe('1h30');
    expect(formatarDuracao(480)).toBe('8h');
    expect(formatarDuracao(605)).toBe('10h05');
  });

  it('entende os formatos mais comuns de duração', () => {
    expect(lerDuracao('1h30')).toBe(90);
    expect(lerDuracao(' 2 h ')).toBe(120);
    expect(lerDuracao('1h5')).toBe(65);
    expect(lerDuracao('1h30min')).toBe(90);
    expect(lerDuracao('45m')).toBe(45);
    expect(lerDuracao('45min')).toBe(45);
    expect(lerDuracao('1:30')).toBe(90);
    expect(lerDuracao('1,5')).toBe(90);
    expect(lerDuracao('0.25')).toBe(15);
    expect(lerDuracao('8')).toBe(480);
  });

  it('recusa o que não é uma duração válida', () => {
    expect(lerDuracao('')).toBeNull();
    expect(lerDuracao('abc')).toBeNull();
    expect(lerDuracao('0')).toBeNull();
    expect(lerDuracao('1:75')).toBeNull();
    expect(lerDuracao('-1')).toBeNull();
  });
});

describe('datas', () => {
  it('acha a segunda-feira da semana, inclusive a partir do domingo', () => {
    expect(inicioDaSemana('2026-11-26')).toBe('2026-11-23'); // quinta
    expect(inicioDaSemana('2026-11-23')).toBe('2026-11-23'); // a própria segunda
    expect(inicioDaSemana('2026-11-29')).toBe('2026-11-23'); // domingo
  });

  it('soma dias atravessando meses e anos', () => {
    expect(somarDias('2026-11-30', 1)).toBe('2026-12-01');
    expect(somarDias('2026-12-28', 7)).toBe('2027-01-04');
    expect(somarDias('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('acha o último dia do mês', () => {
    expect(fimDoMes('2026-11-26')).toBe('2026-11-30');
    expect(fimDoMes('2028-02-10')).toBe('2028-02-29');
  });

  it('dá o nome curto do dia', () => {
    expect(nomeDoDia('2026-11-26')).toBe('Qui');
  });
});
