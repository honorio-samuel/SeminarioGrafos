package br.edu.grafos.dmp.modelo;

import java.util.List;
import java.util.Set;

/**
 * Fragmento de G em relação a H. Pode ser:
 * (1) uma aresta fora de H com os dois extremos em H (corda), ou
 * (2) um componente conexo de G - V(H) junto com as arestas que o ligam a H.
 *
 * @param id               número do fragmento na rodada (para o log)
 * @param ehAresta         true quando é do tipo (1)
 * @param internos         vértices do fragmento que estão fora de H (vazio no tipo 1)
 * @param contatos         vértices de H aos quais o fragmento está ligado
 * @param facesAdmissiveis faces de H que contêm todos os contatos
 */
public record Fragmento(int id, boolean ehAresta, Set<Integer> internos, Set<Integer> contatos,
                        List<Face> facesAdmissiveis) {

    public String descrever(Grafo g) {
        String tipo = ehAresta ? "aresta (corda)" : "componente com internos " + g.nomesDe(internos);
        return "Fragmento " + id + " [" + tipo + "] contatos " + g.nomesDe(contatos);
    }
}
