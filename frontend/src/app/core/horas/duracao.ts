/** Utilitários de duração (em minutos) e de datas locais no formato yyyy-MM-dd. */

/** 90 → "1h30", 480 → "8h", 0 → "0h". */
export function formatarDuracao(minutos: number): string {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, '0')}`;
}

/**
 * Lê o que a pessoa digitou como duração. Aceita "1h30", "1h", "45m", "45min", "1:30", "1,5" e "1.5" (horas).
 * Devolve os minutos, ou null se não entender ou se der zero.
 */
export function lerDuracao(texto: string): number | null {
  const t = texto.trim().toLowerCase().replace(/\s+/g, '');
  if (!t) {
    return null;
  }
  let minutos: number | null = null;
  let m: RegExpMatchArray | null;
  if ((m = t.match(/^(\d{1,2}):([0-5]\d)$/))) {
    minutos = Number(m[1]) * 60 + Number(m[2]);
  } else if ((m = t.match(/^(\d{1,2})h(?:([0-5]?\d)(?:m|min)?)?$/))) {
    minutos = Number(m[1]) * 60 + Number(m[2] ?? 0);
  } else if ((m = t.match(/^(\d{1,4})(?:m|min)$/))) {
    minutos = Number(m[1]);
  } else if ((m = t.match(/^(\d{1,2})(?:[.,](\d{1,2}))?$/))) {
    minutos = Math.round(Number(`${m[1]}.${m[2] ?? 0}`) * 60);
  }
  return minutos && minutos > 0 ? minutos : null;
}

/** Data local em yyyy-MM-dd (sem passar por UTC, que poderia trocar o dia). */
export function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

/** yyyy-MM-dd → Date à meia-noite local. */
export function deIso(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function somarDias(iso: string, dias: number): string {
  const data = deIso(iso);
  data.setDate(data.getDate() + dias);
  return paraIso(data);
}

/** Segunda-feira da semana da data informada. */
export function inicioDaSemana(iso: string): string {
  const diaDaSemana = deIso(iso).getDay(); // 0 = domingo
  return somarDias(iso, diaDaSemana === 0 ? -6 : 1 - diaDaSemana);
}

export function inicioDoMes(iso: string): string {
  return iso.slice(0, 8) + '01';
}

export function fimDoMes(iso: string): string {
  const data = deIso(inicioDoMes(iso));
  return paraIso(new Date(data.getFullYear(), data.getMonth() + 1, 0));
}

export function hojeIso(): string {
  return paraIso(new Date());
}

export const DIAS_DA_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

export function nomeDoDia(iso: string): string {
  return DIAS_DA_SEMANA[deIso(iso).getDay()];
}
