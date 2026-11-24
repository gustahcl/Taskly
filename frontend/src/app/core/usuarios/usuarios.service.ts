import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Perfil, Usuario } from '../auth/auth.models';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>('/api/usuarios');
  }

  alterarPerfil(id: number, perfil: Perfil): Observable<Usuario> {
    return this.http.patch<Usuario>(`/api/usuarios/${id}/perfil`, { perfil });
  }
}
