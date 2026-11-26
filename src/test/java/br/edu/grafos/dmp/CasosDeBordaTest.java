package br.edu.grafos.dmp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.demo.Cenarios;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade.Etapa;
import org.junit.jupiter.api.Test;

/** Casos de borda: grafos triviais, acíclicos, desconexos e entrada suja (laços/paralelas). */
class CasosDeBordaTest {

    private final AlgoritmoDMP dmp = new AlgoritmoDMP();

    @Test
    void grafoNuloEVazio_saoPlanares() {
        assertTrue(assertDoesNotThrow(() -> dmp.testarPlanaridade(null)).planar());
        ResultadoPlanaridade r = dmp.testarPlanaridade(new Grafo(0));
        assertTrue(r.planar());
        assertEquals(Etapa.TRIVIAL, r.etapaDecisiva());
    }

    @Test
    void umEDoisVertices_saoPlanares() {
        assertTrue(dmp.testarPlanaridade(new Grafo(1)).planar());
        Grafo g = new Grafo(2);
        g.adicionarAresta(0, 1);
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(Etapa.TRIVIAL, r.etapaDecisiva());
    }

    @Test
    void arvoreEFloresta_saoPlanaresAciclicas() {
        Grafo arvore = new Grafo(5);
        for (int i = 1; i < 5; i++) {
            arvore.adicionarAresta(0, i);
        }
        ResultadoPlanaridade r = dmp.testarPlanaridade(arvore);
        assertTrue(r.planar());
        assertEquals(Etapa.ACICLICO, r.etapaDecisiva());

        Grafo floresta = new Grafo(7); // duas árvores + vértice isolado
        floresta.adicionarAresta(0, 1);
        floresta.adicionarAresta(1, 2);
        floresta.adicionarAresta(3, 4);
        floresta.adicionarAresta(4, 5);
        r = dmp.testarPlanaridade(floresta);
        assertTrue(r.planar());
        assertEquals(3, r.componentes());
        assertEquals(Etapa.ACICLICO, r.etapaDecisiva());
    }

    @Test
    void grafoSemArestas_ePlanar() {
        assertTrue(dmp.testarPlanaridade(new Grafo(10)).planar());
    }

    @Test
    void lacosEParalelas_saoIgnoradosAntesDoFiltroDeEuler() {
        // K4 planar, mas "sujo": laços e arestas repetidas inflariam E além de 3V-6 = 6 se não fossem limpos.
        Grafo g = new Grafo(4);
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                g.adicionarAresta(i, j);
                g.adicionarAresta(j, i); // paralela
            }
            g.adicionarAresta(i, i); // laço
        }
        assertEquals(6, g.numArestas());
        assertEquals(4, g.lacosIgnorados());
        assertEquals(6, g.paralelasIgnoradas());
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(4, r.lacosIgnorados());
    }

    @Test
    void desconexo_planaresIndependentes_ePlanar() {
        // K4 + triângulo + vértice isolado
        Grafo g = new Grafo(8);
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                g.adicionarAresta(i, j);
            }
        }
        g.adicionarAresta(4, 5);
        g.adicionarAresta(5, 6);
        g.adicionarAresta(6, 4);
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(3, r.componentes());
    }

    @Test
    void desconexo_umComponenteNaoPlanar_tornaTudoNaoPlanar() {
        // Triângulo (planar) + K3,3 (não planar), ainda longe do limite global de Euler.
        Grafo g = new Grafo(9);
        g.adicionarAresta(0, 1);
        g.adicionarAresta(1, 2);
        g.adicionarAresta(2, 0);
        for (int m = 3; m < 6; m++) {
            for (int s = 6; s < 9; s++) {
                g.adicionarAresta(m, s);
            }
        }
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertFalse(r.planar());
        assertEquals(Etapa.DMP, r.etapaDecisiva());
    }

    @Test
    void verticeDeCorte_doisK4CompartilhandoVertice_ePlanar() {
        Grafo g = new Grafo(7);
        int[] a = {0, 1, 2, 3};
        int[] b = {3, 4, 5, 6};
        for (int[] bloco : new int[][] {a, b}) {
            for (int i = 0; i < 4; i++) {
                for (int j = i + 1; j < 4; j++) {
                    g.adicionarAresta(bloco[i], bloco[j]);
                }
            }
        }
        assertTrue(dmp.testarPlanaridade(g).planar());
    }

    @Test
    void k33ComPendente_continuaNaoPlanar() {
        Grafo g = Cenarios.k33();
        int extra = g.adicionarVertice("Extra");
        g.adicionarAresta(0, extra);
        assertFalse(dmp.testarPlanaridade(g).planar());
    }

    @Test
    void k5MenosUmaAresta_ePlanarNoLimiteDeEuler() {
        Grafo g = new Grafo(5);
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                if (!(i == 0 && j == 1)) {
                    g.adicionarAresta(i, j);
                }
            }
        }
        assertEquals(9, g.numArestas()); // = 3V - 6 exatamente
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(9 - 5 + 2, r.facesFinais().size()); // triangulação: 6 faces
    }

    @Test
    void petersen_naoPlanar_passaEulerMasFalhaNoDmp() {
        Grafo g = new Grafo(10);
        for (int i = 0; i < 5; i++) {
            g.adicionarAresta(i, (i + 1) % 5);          // pentágono externo
            g.adicionarAresta(i, i + 5);                // raios
            g.adicionarAresta(5 + i, 5 + (i + 2) % 5);  // estrela interna
        }
        assertTrue(dmp.filtroEuler(g).aprovado());
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertFalse(r.planar());
        assertEquals(Etapa.DMP, r.etapaDecisiva());
    }

    @Test
    void roda_comLogLigado_semExcecao() {
        assertDoesNotThrow(() -> new AlgoritmoDMP(br.edu.grafos.dmp.util.LogDidatico.silencioso())
                .testarPlanaridade(Cenarios.k33()));
    }
}
