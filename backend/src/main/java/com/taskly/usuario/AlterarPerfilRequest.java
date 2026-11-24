package com.taskly.usuario;

import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(@NotNull Perfil perfil) {
}
