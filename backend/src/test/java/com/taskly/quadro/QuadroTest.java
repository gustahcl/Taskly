package com.taskly.quadro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.taskly.TestData;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.usuario.Perfil;

class QuadroTest {

	private final Quadro quadro = new Quadro(TestData.projeto(1L, TestData.usuario(1L, Perfil.GERENTE)), "Sprint");

	@Test
	void quadroNovoVemComAsColunasPadrao() {
		assertThat(quadro.getColunas()).extracting(Coluna::getNome).containsExactly("A fazer", "Em andamento",
				"Concluído");
		assertThat(quadro.getColunas()).extracting(Coluna::getPosicao).containsExactly(0, 1, 2);
	}

	@Test
	void moverColunaReposicionaTodas() {
		Coluna concluido = quadro.getColunas().get(2);

		quadro.moverColuna(concluido, 0);

		assertThat(quadro.getColunas()).extracting(Coluna::getNome).containsExactly("Concluído", "A fazer",
				"Em andamento");
		assertThat(quadro.getColunas()).extracting(Coluna::getPosicao).containsExactly(0, 1, 2);
	}

	@Test
	void posicaoForaDoIntervaloVaiParaAPonta() {
		Coluna aFazer = quadro.getColunas().get(0);

		quadro.moverColuna(aFazer, 42);

		assertThat(quadro.getColunas().get(2)).isSameAs(aFazer);
		assertThat(aFazer.getPosicao()).isEqualTo(2);
	}

	@Test
	void naoRemoveAUltimaColuna() {
		quadro.removerColuna(quadro.getColunas().get(0));
		quadro.removerColuna(quadro.getColunas().get(0));

		assertThat(quadro.getColunas()).singleElement().extracting(Coluna::getPosicao).isEqualTo(0);
		assertThatThrownBy(() -> quadro.removerColuna(quadro.getColunas().get(0)))
			.isInstanceOf(RegraDeNegocioException.class);
	}

}
