import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DadosNomeDialog {
  titulo: string;
  rotulo: string;
  valor?: string;
  maximo: number;
  confirmar?: string;
}

/** Pede um nome (quadro, coluna...). Fecha com o texto digitado ou sem valor se cancelado. */
@Component({
  selector: 'app-nome-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()" novalidate>
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="campo">
          <mat-label>{{ dados.rotulo }}</mat-label>
          <input matInput formControlName="nome" [maxlength]="dados.maximo" cdkFocusInitial />
          @if (form.controls.nome.hasError('required')) {
            <mat-error>Informe o nome</mat-error>
          }
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit">{{ dados.confirmar ?? 'Salvar' }}</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campo {
      width: 100%;
      min-width: 280px;
    }
  `,
})
export class NomeDialog {
  protected readonly dados = inject<DadosNomeDialog>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<NomeDialog, string>);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: [this.dados.valor ?? '', [Validators.required, Validators.pattern(/\S/)]],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.ref.close(this.form.getRawValue().nome.trim());
  }
}
