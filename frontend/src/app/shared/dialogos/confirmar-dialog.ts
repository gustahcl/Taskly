import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DadosConfirmarDialog {
  titulo: string;
  mensagem: string;
  confirmar: string;
}

/** Confirmação de ação destrutiva. Fecha com true se o usuário confirmar. */
@Component({
  selector: 'app-confirmar-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ dados.mensagem }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close cdkFocusInitial>Cancelar</button>
      <button mat-flat-button class="perigo" [mat-dialog-close]="true">{{ dados.confirmar }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .perigo {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmarDialog {
  protected readonly dados = inject<DadosConfirmarDialog>(MAT_DIALOG_DATA);
}
