package br.edu.grafos.dmp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.modelo.Caminho;
import br.edu.grafos.dmp.modelo.Face;
import br.edu.grafos.dmp.modelo.Fragmento;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.ResultadoFiltro;
import br.edu.grafos.dmp.modelo.SubgrafoPlanar;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Testes unitários de cada etapa isolada do slide: filtro, ciclo, fragmentos e divisão de face. */
class EtapasDmpTest {

    private final AlgoritmoDMP dmp = new AlgoritmoDMP();

    @Test
    void filtroEuler_limiteExato() {
        Grafo g = new Grafo(5);
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                g.adicionarAresta(i, j);
            }
        }
        ResultadoFiltro f = dmp.filtroEuler(g);
        assertFalse(f.aprovado());
        assertEquals(9, f.limite());
        assertTrue(f.mensagem().contains("REJEITADO"));
    }

    @Test
    void filtroEuler_naoSeAplicaComMenosDe3Vertices() {
        ResultadoFiltro f = dmp.filtroEuler(new Grafo(2));
        assertFalse(f.aplicavel());
        assertTrue(f.aprovado());
    }

    @Test
    void cicloInicial_acharCicloEmGrafoComCiclo_eVazioEmArvore() {
        Grafo ciclo = new Grafo(5);
        for (int i = 0; i < 5; i++) {
            ciclo.adicionarAresta(i, (i + 1) % 5);
        }
        List<Integer> c = dmp.cicloInicial(ciclo);
        assertEquals(5, c.size());

        Grafo arvore = new Grafo(4);
        arvore.adicionarAresta(0, 1);
        arvore.adicionarAresta(1, 2);
        arvore.adicionarAresta(1, 3);
        assertTrue(dmp.cicloInicial(arvore).isEmpty());
    }

    @Test
    void fragmentos_cordaEComponente() {
        // Quadrado 0-1-2-3 com corda 0-2 e um vértice 4 ligado a 1 e 3.
        Grafo g = new Grafo(5);
        g.adicionarAresta(0, 1);
        g.adicionarAresta(1, 2);
        g.adicionarAresta(2, 3);
        g.adicionarAresta(3, 0);
        g.adicionarAresta(0, 2);
        g.adicionarAresta(4, 1);
        g.adicionarAresta(4, 3);
        SubgrafoPlanar h = SubgrafoPlanar.deCiclo(g, List.of(0, 1, 2, 3));
        assertEquals(2, h.faces().size());

        List<Fragmento> frs = dmp.fragmentos(g, h);
        assertEquals(2, frs.size());
        assertTrue(frs.get(0).ehAresta());
        assertEquals(java.util.Set.of(0, 2), frs.get(0).contatos());
        assertFalse(frs.get(1).ehAresta());
        assertEquals(java.util.Set.of(4), frs.get(1).internos());
        assertEquals(java.util.Set.of(1, 3), frs.get(1).contatos());
        assertEquals(2, frs.get(1).facesAdmissiveis().size());
    }

    @Test
    void dividirFace_geraDuasFacesEAtualizaH() {
        Grafo g = new Grafo(5);
        for (int i = 0; i < 4; i++) {
            g.adicionarAresta(i, (i + 1) % 4);
        }
        g.adicionarAresta(0, 4);
        g.adicionarAresta(4, 2);
        SubgrafoPlanar h = SubgrafoPlanar.deCiclo(g, List.of(0, 1, 2, 3));
        Face interna = h.faces().get(0);

        List<Face> novas = h.dividirFace(interna, new Caminho(List.of(0, 4, 2)));

        assertEquals(2, novas.size());
        assertEquals(3, h.faces().size()); // externa + 2 novas
        assertTrue(h.temVertice(4));
        assertTrue(h.temAresta(0, 4));
        assertTrue(h.temAresta(4, 2));
        // Cada nova face tem 4 vértices: (0,1,2,4) e (2,3,0,4), a menos de rotação
        for (Face f : novas) {
            assertEquals(4, f.vertices().size());
            assertTrue(f.contem(4));
        }
    }

    @Test
    void dividirFace_rejeitaCaminhoInvalido() {
        Grafo g = new Grafo(4);
        for (int i = 0; i < 4; i++) {
            g.adicionarAresta(i, (i + 1) % 4);
        }
        SubgrafoPlanar h = SubgrafoPlanar.deCiclo(g, List.of(0, 1, 2, 3));
        Face f = h.faces().get(0);
        assertThrows(IllegalArgumentException.class, () -> h.dividirFace(f, new Caminho(List.of(0, 0))));
    }
}
