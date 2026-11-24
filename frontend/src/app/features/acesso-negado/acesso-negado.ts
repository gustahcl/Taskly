import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-acesso-negado',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <section>
      <mat-icon>lock</mat-icon>
      <h1>Acesso negado</h1>
      <p>Seu perfil não tem permissão para ver esta página.</p>
      <a mat-stroked-button routerLink="/">Voltar ao início</a>
    </section>
  `,
  styles: `
    section {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 48px 16px;
      text-align: center;
    }
    mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--mat-sys-on-surface-variant);
    }
    h1 {
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
    p {
      margin: 0 0 16px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class AcessoNegado {}
