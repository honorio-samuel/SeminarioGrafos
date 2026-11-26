package br.edu.grafos.dmp.algoritmo;

import br.edu.grafos.dmp.modelo.Grafo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Blocos (componentes biconexos) via algoritmo de Tarjan.
 * O DMP exige grafo 2-conexo; um grafo é planar se e somente se cada bloco for planar.
 */
public final class Blocos {

    private Blocos() { }

    /** Cada bloco é devolvido como lista de arestas {u, v}. Pontes formam blocos de 1 aresta. */
    public static List<List<int[]>> encontrar(Grafo g) {
        int n = g.numVertices();
        int[] disc = new int[n];
        int[] low = new int[n];
        int[] tempo = {0};
        Deque<int[]> pilha = new ArrayDeque<>();
        List<List<int[]>> blocos = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            if (disc[s] == 0) {
                dfs(g, s, -1, disc, low, tempo, pilha, blocos);
            }
        }
        return blocos;
    }

    private static void dfs(Grafo g, int u, int pai, int[] disc, int[] low, int[] tempo,
                            Deque<int[]> pilha, List<List<int[]>> blocos) {
        disc[u] = low[u] = ++tempo[0];
        for (int w : g.vizinhos(u)) {
            if (w == pai) {
                continue;
            }
            if (disc[w] == 0) {
                pilha.push(new int[] {u, w});
                dfs(g, w, u, disc, low, tempo, pilha, blocos);
                low[u] = Math.min(low[u], low[w]);
                if (low[w] >= disc[u]) {
                    List<int[]> bloco = new ArrayList<>();
                    int[] a;
                    do {
                        a = pilha.pop();
                        bloco.add(a);
                    } while (!(a[0] == u && a[1] == w));
                    blocos.add(bloco);
                }
            } else if (disc[w] < disc[u]) {
                pilha.push(new int[] {u, w});
                low[u] = Math.min(low[u], disc[w]);
            }
        }
    }
}
