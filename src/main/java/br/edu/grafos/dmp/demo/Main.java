package br.edu.grafos.dmp.demo;

import br.edu.grafos.dmp.algoritmo.AlgoritmoDMP;
import br.edu.grafos.dmp.demo.Cenarios.Cenario;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.util.LogDidatico;
import br.edu.grafos.dmp.visual.Cena;
import br.edu.grafos.dmp.visual.ExportadorPng;
import br.edu.grafos.dmp.visual.Visualizador;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Demonstração ao vivo do seminário: roda os 4 cenários do Slide 16.
 *
 * <p>Opções:
 * <ul>
 *   <li>{@code 1..4}: roda só aquele cenário;</li>
 *   <li>{@code --gui}: abre a janela gráfica passo a passo (também em {@link MainVisual});</li>
 *   <li>{@code --png[=pasta]}: grava uma imagem por passo (padrão: pasta "imagens");</li>
 *   <li>{@code --silencioso}: só o quadro-resumo; {@code --pausar}: espera ENTER entre cenários.</li>
 * </ul>
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) throws IOException {
        boolean silencioso = false;
        boolean pausar = false;
        boolean gui = false;
        Path pastaPng = null;
        Integer apenas = null;
        for (String a : args) {
            if (a.equals("--silencioso")) {
                silencioso = true;
            } else if (a.equals("--pausar")) {
                pausar = true;
            } else if (a.equals("--gui")) {
                gui = true;
            } else if (a.equals("--png")) {
                pastaPng = Path.of("imagens");
            } else if (a.startsWith("--png=")) {
                pastaPng = Path.of(a.substring(6));
            } else if (a.matches("[1-4]")) {
                apenas = Integer.parseInt(a);
            } else {
                System.err.println("Argumento ignorado: " + a);
            }
        }

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        boolean graficoApenas = gui || pastaPng != null;
        if (!silencioso && !graficoApenas) {
            imprimirLegenda(out);
        }

        List<Cena> cenas = new ArrayList<>();
        List<String> tabela = new ArrayList<>();
        Scanner teclado = pausar ? new Scanner(System.in, StandardCharsets.UTF_8) : null;
        for (Cenario c : Cenarios.todos()) {
            if (apenas != null && apenas != c.numero()) {
                continue;
            }
            if (!graficoApenas) {
                out.println();
                out.println("█".repeat(78));
                out.println("█  CENÁRIO " + c.numero() + " · " + c.titulo());
                out.println("█  " + c.roteiro());
                out.println("█".repeat(78));
            }
            boolean logar = !silencioso && !graficoApenas;
            LogDidatico log = logar ? LogDidatico.console() : LogDidatico.silencioso();
            ResultadoPlanaridade r = new AlgoritmoDMP(log).testarPlanaridade(c.grafo().get());
            cenas.add(new Cena("Cenário " + c.numero() + " · " + c.titulo(), r.passos()));
            tabela.add(String.format("  %s  %d · %-38s  V=%-2d E=%-2d  %-10s  (decidido em: %s)",
                    r.planar() ? "✔" : "✘", c.numero(), c.titulo(), r.vertices(), r.arestas(),
                    r.diagnostico(), r.etapaDecisiva()));
            if (pausar && !graficoApenas && teclado.hasNextLine()) {
                out.println("\n[ENTER para o próximo cenário]");
                teclado.nextLine();
            }
        }

        out.println();
        out.println("╔" + "═".repeat(76) + "╗");
        out.println("║" + String.format("%-76s", " QUADRO-RESUMO DOS CENÁRIOS") + "║");
        out.println("╚" + "═".repeat(76) + "╝");
        tabela.forEach(out::println);

        if (pastaPng != null) {
            List<Path> arquivos = ExportadorPng.exportar(cenas, pastaPng);
            out.println("\n" + arquivos.size() + " imagens gravadas em: " + pastaPng.toAbsolutePath());
        }
        if (gui) {
            Visualizador.abrir(cenas);
        }
    }

    private static void imprimirLegenda(PrintStream out) {
        out.println("""

                ┌─ LEGENDA (para acompanhar o log) ───────────────────────────────────────────┐
                │ G = grafo da placa (vértices = componentes, arestas = trilhas)              │
                │ H = parte de G que já foi desenhada no plano, sem cruzamentos               │
                │ Face = região do plano delimitada por H                                     │
                │ Fragmento = pedaço de G ainda fora de H (uma corda ou um componente)        │
                │ Contatos = vértices de H em que o fragmento se liga                         │
                │ Face admissível = face de H que contém TODOS os contatos do fragmento       │
                │   0 faces → NÃO planar · 1 face → escolha forçada · 2+ → escolha livre      │
                └─────────────────────────────────────────────────────────────────────────────┘""");
    }
}
