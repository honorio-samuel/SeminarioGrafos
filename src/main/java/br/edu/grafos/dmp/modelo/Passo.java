package br.edu.grafos.dmp.modelo;

import java.util.List;
import java.util.Set;

/**
 * "Foto" de um momento do algoritmo, usada pela visualização gráfica.
 *
 * @param verticesH         vértices já desenhados em H (null = não se aplica)
 * @param arestasH          arestas já desenhadas em H (null = não se aplica)
 * @param fragmentoEscolhido id do fragmento escolhido na rodada (0 = nenhum)
 * @param caminho           caminho a inserir (null = nenhum)
 * @param desenhoIngenuo    true = vértices em círculo (mostra cruzamentos de um desenho ingênuo)
 * @param planar            veredicto (apenas no passo de diagnóstico; null nos demais)
 */
public record Passo(Tipo tipo, String titulo, List<String> texto, Grafo grafo,
                    Set<Integer> verticesH, Set<Long> arestasH, List<Fragmento> fragmentos,
                    int fragmentoEscolhido, Caminho caminho, List<String> faces,
                    boolean desenhoIngenuo, Boolean planar) {

    public enum Tipo { INFO, CICLO, RODADA, BLOQUEIO, CONCLUIDO, DIAGNOSTICO }
}
