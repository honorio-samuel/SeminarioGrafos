package br.edu.grafos.dmp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.demo.Cenarios;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade.Etapa;
import org.junit.jupiter.api.Test;

/** Os 4 cenários do Slide 16. */
class CenariosSeminarioTest {

    private final AlgoritmoDMP dmp = new AlgoritmoDMP();

    @Test
    void cenario1_planarEnganoso_ePlanarComFaces() {
        Grafo g = Cenarios.planarEnganoso();
        assertEquals(6, g.numVertices());
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(Etapa.DMP, r.etapaDecisiva());
        // Euler (conexo): F = E - V + 2 = 9 - 6 + 2 = 5 faces
        assertEquals(5, r.facesFinais().size());
    }

    @Test
    void cenario2_k5_barradoPeloFiltroDeEuler() {
        Grafo g = Cenarios.k5();
        assertEquals(5, g.numVertices());
        assertEquals(10, g.numArestas());
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertFalse(r.planar());
        assertEquals(Etapa.FILTRO_EULER, r.etapaDecisiva());
        assertTrue(r.motivo().contains("10 > 3V - 6 = 3(5) - 6 = 9") || r.motivo().contains("E = 10"));
    }

    @Test
    void cenario3_k33_passaEulerMasFalhaNoDmp() {
        Grafo g = Cenarios.k33();
        assertEquals(6, g.numVertices());
        assertEquals(9, g.numArestas());
        assertTrue(dmp.filtroEuler(g).aprovado(), "K3,3 deve passar em Euler (9 <= 12)");
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertFalse(r.planar());
        assertEquals(Etapa.DMP, r.etapaDecisiva());
        assertTrue(r.motivo().contains("0 faces admissíveis"));
    }

    @Test
    void cenario4_pcbSimulada_ePlanarEmCamadaUnica() {
        Grafo g = Cenarios.pcbSimulada();
        ResultadoPlanaridade r = dmp.testarPlanaridade(g);
        assertTrue(r.planar());
        assertEquals(g.numArestas() - g.numVertices() + 2, r.facesFinais().size());
        assertTrue(r.interpretacaoPCB().contains("CAMADA ÚNICA"));
    }
}
