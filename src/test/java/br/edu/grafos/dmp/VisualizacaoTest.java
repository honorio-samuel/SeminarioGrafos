package br.edu.grafos.dmp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.demo.Cenarios;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.Passo;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.visual.Cena;
import br.edu.grafos.dmp.visual.ExportadorPng;
import br.edu.grafos.dmp.visual.Layout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Passos registrados para a visualização, layout e exportação de imagens. */
class VisualizacaoTest {

    @Test
    void passosTerminamNoDiagnosticoComVeredictoCorreto() {
        for (Cenarios.Cenario c : Cenarios.todos()) {
            ResultadoPlanaridade r = new AlgoritmoDMP().testarPlanaridade(c.grafo().get());
            List<Passo> passos = r.passos();
            assertFalse(passos.isEmpty());
            Passo ultimo = passos.get(passos.size() - 1);
            assertEquals(Passo.Tipo.DIAGNOSTICO, ultimo.tipo());
            assertEquals(r.planar(), ultimo.planar());
        }
    }

    @Test
    void layoutDosCenariosPlanaresNaoTemCruzamentos_eK5K33TemExatamenteUm() {
        assertEquals(0, cruz(Cenarios.planarEnganoso()));
        assertEquals(0, cruz(Cenarios.pcbSimulada()));
        assertEquals(1, cruz(Cenarios.k5()));
        assertEquals(1, cruz(Cenarios.k33()));
    }

    @Test
    void exportaUmPngPorPasso(@TempDir Path pasta) throws Exception {
        ResultadoPlanaridade r = new AlgoritmoDMP().testarPlanaridade(Cenarios.k33());
        List<Path> arquivos = ExportadorPng.exportar(List.of(new Cena("teste", r.passos())), pasta);
        assertEquals(r.passos().size(), arquivos.size());
        for (Path a : arquivos) {
            assertTrue(Files.size(a) > 1000);
        }
    }

    private static int cruz(Grafo g) {
        return Layout.cruzamentos(g, Layout.otimizado(g)).size();
    }
}
