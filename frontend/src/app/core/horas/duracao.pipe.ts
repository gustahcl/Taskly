import { Pipe, PipeTransform } from '@angular/core';

import { formatarDuracao } from './duracao';

/** Minutos → "1h30". */
@Pipe({ name: 'duracao' })
export class DuracaoPipe implements PipeTransform {
  transform(minutos: number | null | undefined): string {
    return formatarDuracao(minutos ?? 0);
  }
}
