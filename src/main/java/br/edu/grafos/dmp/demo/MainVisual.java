package br.edu.grafos.dmp.demo;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.demo.Cenarios.Cenario;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.visual.Cena;
import br.edu.grafos.dmp.visual.Visualizador;
import java.util.ArrayList;
import java.util.List;

/**
 * Abre direto a janela gráfica com os 4 cenários (sem precisar de argumentos).
 * Na IDE: botão direito nesta classe → Run 'MainVisual.main()'.
 */
public final class MainVisual {

    private MainVisual() { }

    public static void main(String[] args) {
        List<Cena> cenas = new ArrayList<>();
        for (Cenario c : Cenarios.todos()) {
            ResultadoPlanaridade r = new AlgoritmoDMP().testarPlanaridade(c.grafo().get());
            cenas.add(new Cena("Cenário " + c.numero() + " · " + c.titulo(), r.passos()));
        }
        Visualizador.abrir(cenas);
    }
}
