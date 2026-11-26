package br.edu.grafos.dmp.algoritmo;

import br.edu.grafos.dmp.modelo.Caminho;
import br.edu.grafos.dmp.modelo.Face;
import br.edu.grafos.dmp.modelo.Fragmento;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.Passo;
import br.edu.grafos.dmp.modelo.ResultadoFiltro;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade;
import br.edu.grafos.dmp.modelo.ResultadoPlanaridade.Etapa;
import br.edu.grafos.dmp.modelo.SubgrafoPlanar;
import br.edu.grafos.dmp.util.LogDidatico;
import br.edu.grafos.dmp.visual.Layout;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Teste de planaridade pelo algoritmo DMP (Demoucron, Malgrange e Pertuiset).
 *
 * <p>Etapas (espelhando os slides):
 * <ol>
 *   <li>{@link #filtroEuler(Grafo)}: condição necessária E &lt;= 3V - 6 em O(1);</li>
 *   <li>{@link #cicloInicial(Grafo)}: ciclo base por DFS, formando H com 2 faces;</li>
 *   <li>{@link #fragmentos(Grafo, SubgrafoPlanar)}: fragmentos fora de H, contatos e faces admissíveis;</li>
 *   <li>{@link SubgrafoPlanar#dividirFace(Face, Caminho)}: insere um caminho e divide a face.</li>
 * </ol>
 * O DMP é aplicado em cada bloco (componente 2-conexo) de cada componente conexo do grafo.
 * Além do log de texto, cada momento relevante é guardado como {@link Passo} para a visualização gráfica.
 */
public class AlgoritmoDMP {

    private final LogDidatico log;
    private List<Passo> passos = new ArrayList<>();
    private int contadorBlocos;

    public AlgoritmoDMP() {
        this(LogDidatico.silencioso());
    }

    public AlgoritmoDMP(LogDidatico log) {
        this.log = log;
    }

    // ------------------------------------------------------------------------------------
    // Ponto de entrada
    // ------------------------------------------------------------------------------------

    public ResultadoPlanaridade testarPlanaridade(Grafo entrada) {
        Grafo g = entrada == null ? new Grafo(0) : entrada;
        passos = new ArrayList<>();
        contadorBlocos = 0;
        int v = g.numVertices();
        int e = g.numArestas();
        List<List<Integer>> comps = g.componentesConexos();
        int c = comps.size();

        log.secao("1 · Pré-processamento");
        log.info("Grafo simples: V = " + v + " vértices, E = " + e + " arestas, "
                + c + " componente(s) conexo(s).");
        log.info("Limpeza da entrada: " + g.lacosIgnorados() + " laço(s) e "
                + g.paralelasIgnoradas() + " aresta(s) paralela(s) ignorados.");
        int cruzIngenuo = Layout.cruzamentos(g, Layout.circular(g)).size();
        registrar(Passo.Tipo.INFO, "Grafo de entrada", List.of(
                "Vértices = componentes (V = " + v + "); arestas = trilhas (E = " + e + "); "
                        + c + " componente(s) conexo(s).",
                "Neste desenho ingênuo (vértices em círculo) há " + cruzIngenuo + " cruzamento(s). "
                        + "Isso NÃO decide nada: um desenho melhor pode existir. Quem decide é o algoritmo."),
                g, null, null, 0, null, true, null);

        if (v < 3) {
            return finalizar(true, Etapa.TRIVIAL, g, c, List.of(),
                    "Com " + v + " vértice(s) e " + e + " aresta(s) não há como cruzar trilhas.");
        }

        log.secao("2 · Filtro de Euler  (planar simples ⇒ E ≤ 3V − 6)");
        ResultadoFiltro filtro = filtroEuler(g);
        log.info((filtro.aprovado() ? "✔ " : "✘ ") + filtro.mensagem());
        registrar(Passo.Tipo.INFO, "Filtro de Euler", List.of(filtro.mensagem()),
                g, null, null, 0, null, false, null);
        if (!filtro.aprovado()) {
            return finalizar(false, Etapa.FILTRO_EULER, g, c, List.of(), filtro.mensagem());
        }

        List<String> facesFinais = new ArrayList<>();
        boolean temCiclo = false;
        for (int i = 0; i < comps.size(); i++) {
            Grafo sub = g.subgrafoInduzido(comps.get(i));
            String rotuloComp = "Componente " + (i + 1) + "/" + c + " " + g.nomesDe(comps.get(i));
            if (sub.numVertices() == 1) {
                log.secao("3 · " + rotuloComp);
                log.info("Vértice isolado: planar.");
                continue;
            }
            if (sub.numArestas() == sub.numVertices() - 1) {
                log.secao("3 · " + rotuloComp);
                log.info("É uma árvore (sem ciclos): planar.");
                continue;
            }
            temCiclo = true;
            List<List<int[]>> blocos = Blocos.encontrar(sub);
            if (c > 1 || blocos.size() > 1) {
                log.secao("3 · " + rotuloComp);
                log.info("Dividido em " + blocos.size() + " bloco(s) 2-conexo(s) (o DMP exige 2-conexidade; "
                        + "o grafo é planar sse todos os blocos forem).");
            }
            for (List<int[]> arestasBloco : blocos) {
                if (arestasBloco.size() == 1) {
                    continue; // ponte: sempre planar
                }
                Grafo bloco = sub.subgrafoDeArestas(arestasBloco);
                contadorBlocos++;
                String rotuloBloco = "B" + contadorBlocos;
                boolean igualAoTodo = bloco.numVertices() == v && bloco.numArestas() == e;
                ResultadoBloco rb = testarBloco(bloco, rotuloBloco, igualAoTodo);
                if (!rb.planar()) {
                    return finalizar(false, Etapa.DMP, g, c, List.of(), rb.motivo());
                }
                for (Face f : rb.faces()) {
                    facesFinais.add((contadorBlocos > 1 ? "[" + rotuloBloco + "] " : "") + f.descrever(bloco));
                }
            }
        }
        if (c > 1) {
            log.info("");
            log.info("Euler para grafos desconexos: V − E + F = c + 1  ⇒  F = " + e + " − " + v + " + " + c
                    + " + 1 = " + (e - v + c + 1) + " faces no total.");
        }
        return finalizar(true, temCiclo ? Etapa.DMP : Etapa.ACICLICO, g, c, facesFinais,
                temCiclo ? "O DMP desenhou todos os blocos no plano sem nenhum cruzamento."
                        : "É uma floresta (sem ciclos): basta desenhar os segmentos.");
    }

    // ------------------------------------------------------------------------------------
    // Etapa 1: filtroEuler
    // ------------------------------------------------------------------------------------

    /** Valida em O(1) a condição necessária E &lt;= 3V - 6 (grafo simples, V &gt;= 3). */
    public ResultadoFiltro filtroEuler(Grafo g) {
        int v = g.numVertices();
        int e = g.numArestas();
        if (v < 3) {
            return new ResultadoFiltro(false, true, e, 0,
                    "Filtro não se aplica (V = " + v + " < 3): trivialmente planar.");
        }
        int limite = 3 * v - 6;
        if (e > limite) {
            return new ResultadoFiltro(true, false, e, limite,
                    "REJEITADO: E = " + e + " > 3V - 6 = " + limite + " (V = " + v + "). "
                            + "Grafo planar simples nunca tem tantas arestas.");
        }
        return new ResultadoFiltro(true, true, e, limite,
                "Passou: E = " + e + " ≤ 3V − 6 = " + limite + ". (Condição só NECESSÁRIA: o DMP ainda decide.)");
    }

    // ------------------------------------------------------------------------------------
    // Etapa 2: cicloInicial
    // ------------------------------------------------------------------------------------

    /**
     * Encontra um ciclo via DFS (primeira aresta de retorno).
     *
     * @return vértices do ciclo em ordem, ou lista vazia se o grafo for acíclico
     */
    public List<Integer> cicloInicial(Grafo g) {
        int n = g.numVertices();
        int[] estado = new int[n]; // 0 = não visitado, 1 = na pilha de recursão, 2 = finalizado
        int[] pai = new int[n];
        java.util.Arrays.fill(pai, -1);
        for (int s = 0; s < n; s++) {
            if (estado[s] == 0) {
                List<Integer> ciclo = dfsCiclo(g, s, estado, pai);
                if (!ciclo.isEmpty()) {
                    return ciclo;
                }
            }
        }
        return List.of();
    }

    private List<Integer> dfsCiclo(Grafo g, int u, int[] estado, int[] pai) {
        estado[u] = 1;
        for (int w : g.vizinhos(u)) {
            if (w == pai[u]) {
                continue;
            }
            if (estado[w] == 0) {
                pai[w] = u;
                List<Integer> ciclo = dfsCiclo(g, w, estado, pai);
                if (!ciclo.isEmpty()) {
                    return ciclo;
                }
            } else if (estado[w] == 1) {
                // Aresta de retorno u -> w (w é ancestral de u): ciclo w ... u.
                List<Integer> ciclo = new ArrayList<>();
                int atual = u;
                ciclo.add(atual);
                while (atual != w) {
                    atual = pai[atual];
                    ciclo.add(atual);
                }
                Collections.reverse(ciclo);
                return ciclo;
            }
        }
        estado[u] = 2;
        return List.of();
    }

    // ------------------------------------------------------------------------------------
    // Etapa 3: fragmentos
    // ------------------------------------------------------------------------------------

    /**
     * Mapeia os fragmentos de G em relação a H e calcula, para cada um, os vértices de contato
     * e as faces admissíveis de H.
     */
    public List<Fragmento> fragmentos(Grafo g, SubgrafoPlanar h) {
        List<Fragmento> lista = new ArrayList<>();
        int id = 1;

        // Tipo 1: arestas de G fora de H com os dois extremos em H.
        for (int[] a : g.arestas()) {
            if (h.temVertice(a[0]) && h.temVertice(a[1]) && !h.temAresta(a[0], a[1])) {
                lista.add(montar(id++, true, Set.of(), new TreeSet<>(List.of(a[0], a[1])), h));
            }
        }

        // Tipo 2: componentes conexos de G - V(H) (BFS), com seus contatos em H.
        Set<Integer> visto = new HashSet<>();
        for (int s = 0; s < g.numVertices(); s++) {
            if (h.temVertice(s) || visto.contains(s)) {
                continue;
            }
            Set<Integer> internos = new TreeSet<>();
            Set<Integer> contatos = new TreeSet<>();
            ArrayDeque<Integer> fila = new ArrayDeque<>();
            fila.add(s);
            visto.add(s);
            while (!fila.isEmpty()) {
                int x = fila.poll();
                internos.add(x);
                for (int w : g.vizinhos(x)) {
                    if (h.temVertice(w)) {
                        contatos.add(w);
                    } else if (visto.add(w)) {
                        fila.add(w);
                    }
                }
            }
            lista.add(montar(id++, false, internos, contatos, h));
        }
        return lista;
    }

    private Fragmento montar(int id, boolean ehAresta, Set<Integer> internos, Set<Integer> contatos,
                             SubgrafoPlanar h) {
        List<Face> admissiveis = new ArrayList<>();
        for (Face f : h.faces()) {
            if (f.contemTodos(contatos)) {
                admissiveis.add(f);
            }
        }
        return new Fragmento(id, ehAresta, internos, contatos, admissiveis);
    }

    /**
     * Escolhe um caminho dentro do fragmento ligando dois contatos distintos (BFS nos vértices internos).
     * Em grafo 2-conexo todo fragmento tem pelo menos 2 contatos, logo o caminho sempre existe.
     */
    public Caminho caminhoDoFragmento(Grafo g, SubgrafoPlanar h, Fragmento fr) {
        List<Integer> contatos = new ArrayList<>(fr.contatos());
        if (fr.ehAresta()) {
            return new Caminho(List.of(contatos.get(0), contatos.get(1)));
        }
        int a = contatos.get(0);
        Map<Integer, Integer> pred = new HashMap<>();
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        for (int w : g.vizinhos(a)) {
            if (fr.internos().contains(w)) {
                pred.put(w, a);
                fila.add(w);
            }
        }
        while (!fila.isEmpty()) {
            int x = fila.poll();
            for (int w : g.vizinhos(x)) {
                if (h.temVertice(w)) {
                    if (w != a) {
                        List<Integer> caminho = new ArrayList<>();
                        caminho.add(w);
                        int atual = x;
                        while (atual != a) {
                            caminho.add(atual);
                            atual = pred.get(atual);
                        }
                        caminho.add(a);
                        Collections.reverse(caminho);
                        return new Caminho(caminho);
                    }
                } else if (fr.internos().contains(w) && !pred.containsKey(w)) {
                    pred.put(w, x);
                    fila.add(w);
                }
            }
        }
        throw new IllegalStateException("Fragmento sem caminho entre contatos: o grafo não era 2-conexo.");
    }

    // ------------------------------------------------------------------------------------
    // Loop do DMP para um bloco 2-conexo
    // ------------------------------------------------------------------------------------

    private record ResultadoBloco(boolean planar, String motivo, List<Face> faces) { }

    private ResultadoBloco testarBloco(Grafo b, String rotulo, boolean igualAoTodo) {
        int vb = b.numVertices();
        int eb = b.numArestas();
        log.secao("3 · DMP" + (igualAoTodo ? "" : " no bloco " + rotulo + " " + b.nomesDe(todos(b))));

        if (!igualAoTodo) {
            ResultadoFiltro filtro = filtroEuler(b);
            log.info((filtro.aprovado() ? "✔ " : "✘ ") + "Euler no bloco: " + filtro.mensagem());
            if (!filtro.aprovado()) {
                return new ResultadoBloco(false, filtro.mensagem(), List.of());
            }
        }

        List<Integer> ciclo = cicloInicial(b);
        if (ciclo.isEmpty()) {
            return new ResultadoBloco(true, "", List.of());
        }
        SubgrafoPlanar h = SubgrafoPlanar.deCiclo(b, ciclo);
        log.info("Ciclo inicial (DFS): " + String.join("-", nomes(b, ciclo)));
        log.info("H começa com 2 faces de mesmo contorno: F1 (interna) e F2 (externa).");
        registrar(Passo.Tipo.CICLO, "Ciclo inicial", List.of(
                "A busca em profundidade (DFS) achou o ciclo " + String.join("-", nomes(b, ciclo)) + ".",
                "H começa só com ele e tem 2 faces: F1 (dentro do ciclo) e F2 (fora). "
                        + "O resto do grafo são fragmentos a encaixar."),
                b, h, null, 0, null, false, null);

        int rodada = 0;
        while (true) {
            rodada++;
            List<Fragmento> frs = fragmentos(b, h);
            if (frs.isEmpty()) {
                log.linha("");
                log.info("✔ Nenhum fragmento sobrou: H = G.");
                break;
            }
            log.linha("");
            log.linha("  ┌─ Rodada " + rodada + "  (H: " + h.vertices().size() + " vértices, "
                    + h.numArestas() + " arestas, " + h.faces().size() + " faces)");
            for (Fragmento fr : frs) {
                log.linha("  │  Fragmento " + fr.id() + " · " + (fr.ehAresta() ? "corda" : "componente "
                        + b.nomesDe(fr.internos())) + " · contatos " + b.nomesDe(fr.contatos())
                        + " · faces admissíveis: " + idsFaces(fr.facesAdmissiveis()));
            }

            for (Fragmento fr : frs) {
                if (fr.facesAdmissiveis().isEmpty()) {
                    String motivo = "O fragmento com contatos " + b.nomesDe(fr.contatos())
                            + " tem 0 faces admissíveis: nenhuma face de H contém todos os seus contatos.";
                    log.linha("  │");
                    log.linha("  └─ ✘ BLOQUEIO: " + motivo);
                    registrar(Passo.Tipo.BLOQUEIO, "Rodada " + rodada + ": BLOQUEIO", List.of(
                            "O fragmento " + fr.id() + " (contatos " + b.nomesDe(fr.contatos()) + ") tem 0 faces admissíveis.",
                            "Nenhuma face de H contém TODOS os contatos dele, então ele não cabe em lugar nenhum "
                                    + "sem cruzar trilhas.",
                            "Conclusão: o grafo contém uma subdivisão de K5 ou K3,3 → NÃO PLANAR."),
                            b, h, frs, fr.id(), null, false, null);
                    return new ResultadoBloco(false, motivo, List.of());
                }
            }

            Fragmento escolhido = frs.stream()
                    .min(Comparator.comparingInt((Fragmento f) -> f.facesAdmissiveis().size())
                            .thenComparingInt(Fragmento::id))
                    .orElseThrow();
            Face face = escolhido.facesAdmissiveis().get(0);
            boolean forcada = escolhido.facesAdmissiveis().size() == 1;
            Caminho p = caminhoDoFragmento(b, h, escolhido);

            String decisao = forcada
                    ? "escolha FORÇADA (só 1 face admissível: F" + face.id() + ")"
                    : "escolha LIVRE (" + escolhido.facesAdmissiveis().size()
                    + " faces admissíveis; usa a primeira: F" + face.id() + ")";
            log.linha("  │");
            log.linha("  │  ➜ Fragmento " + escolhido.id() + " (menos faces admissíveis) · " + decisao);
            log.linha("  │  ➜ Caminho inserido: " + p.descrever(b));
            registrar(Passo.Tipo.RODADA, "Rodada " + rodada, List.of(
                    "Fragmento " + escolhido.id() + " tem o menor número de faces admissíveis ("
                            + escolhido.facesAdmissiveis().size() + ") → " + decisao + ".",
                    "Caminho a inserir na face F" + face.id() + ": " + p.descrever(b) + " (linha grossa no desenho)."),
                    b, h, frs, escolhido.id(), p, false, null);

            List<Face> novas = h.dividirFace(face, p);
            log.linha("  └─ ✂ F" + face.id() + " dividida em  " + novas.get(0).descrever(b)
                    + "   e   " + novas.get(1).descrever(b));
        }

        int facesObtidas = h.faces().size();
        boolean euler = vb - eb + facesObtidas == 2;
        log.info("Conferência de Euler: V − E + F = " + vb + " − " + eb + " + " + facesObtidas + " = "
                + (vb - eb + facesObtidas) + (euler ? "  ✔" : "  ✘ (inconsistente!)"));
        registrar(Passo.Tipo.CONCLUIDO, "Bloco desenhado", List.of(
                "Não restam fragmentos: todas as arestas foram encaixadas sem cruzamento.",
                "Conferência de Euler: V − E + F = " + vb + " − " + eb + " + " + facesObtidas + " = "
                        + (vb - eb + facesObtidas) + (euler ? " ✔" : " ✘")),
                b, h, null, 0, null, false, null);
        return new ResultadoBloco(true, "", new ArrayList<>(h.faces()));
    }

    // ------------------------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------------------------

    private void registrar(Passo.Tipo tipo, String titulo, List<String> texto, Grafo grafo, SubgrafoPlanar h,
                           List<Fragmento> frs, int escolhido, Caminho caminho, boolean ingenuo, Boolean planar) {
        Set<Integer> vh = null;
        Set<Long> ah = null;
        List<String> faces = List.of();
        if (h != null) {
            vh = new TreeSet<>(h.vertices());
            ah = new HashSet<>(h.arestas());
            List<String> fs = new ArrayList<>();
            for (Face f : h.faces()) {
                fs.add(f.descrever(grafo));
            }
            faces = fs;
        }
        passos.add(new Passo(tipo, titulo, List.copyOf(texto), grafo, vh, ah,
                frs == null ? List.of() : List.copyOf(frs), escolhido, caminho, faces, ingenuo, planar));
    }

    private static List<Integer> todos(Grafo g) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < g.numVertices(); i++) {
            l.add(i);
        }
        return l;
    }

    private static List<String> nomes(Grafo g, List<Integer> vs) {
        List<String> l = new ArrayList<>();
        for (int x : vs) {
            l.add(g.nome(x));
        }
        return l;
    }

    private static String idsFaces(List<Face> faces) {
        if (faces.isEmpty()) {
            return "nenhuma (0)";
        }
        List<String> ids = new ArrayList<>();
        for (Face f : faces) {
            ids.add("F" + f.id());
        }
        return String.join(", ", ids);
    }

    private ResultadoPlanaridade finalizar(boolean planar, Etapa etapa, Grafo g, int c,
                                           List<String> faces, String motivo) {
        String interpretacao;
        if (planar) {
            interpretacao = "Cabe em CAMADA ÚNICA: trilhas sem cruzamento e sem vias.";
        } else if (etapa == Etapa.FILTRO_EULER) {
            interpretacao = "Trilhas demais para um plano: o cruzamento é inevitável. "
                    + "Use 2ª camada/vias (ou jumpers) ou reorganize o leiaute.";
        } else {
            interpretacao = "Passou em Euler, mas a topologia contém K5 ou K3,3: o cruzamento é inevitável. "
                    + "Use uma via/2ª camada no trecho conflitante ou reorganize as ligações.";
        }

        // Passo final para a visualização.
        Set<Integer> vh = null;
        Set<Long> ah = null;
        if (planar) {
            vh = new TreeSet<>(todos(g));
            ah = new HashSet<>();
            for (int[] a : g.arestas()) {
                ah.add(Grafo.chave(a[0], a[1]));
            }
        }
        List<String> textoFinal = new ArrayList<>();
        textoFinal.add("Diagnóstico: " + (planar ? "PLANAR" : "NÃO PLANAR") + ".");
        textoFinal.add("Por quê: " + motivo);
        textoFinal.add("Na PCB: " + interpretacao);
        passos.add(new Passo(Passo.Tipo.DIAGNOSTICO, "Diagnóstico final", textoFinal, g, vh, ah, List.of(), 0, null,
                List.copyOf(faces), false, planar));

        ResultadoPlanaridade r = new ResultadoPlanaridade(planar, etapa, motivo, interpretacao,
                List.copyOf(faces), g.numVertices(), g.numArestas(), c, g.lacosIgnorados(), g.paralelasIgnoradas(),
                List.copyOf(passos));

        log.caixa("DIAGNÓSTICO: " + r.diagnostico().toUpperCase());
        log.info("Por quê : " + motivo);
        log.info("Na PCB  : " + interpretacao);
        if (!faces.isEmpty()) {
            log.info("Faces finais (" + faces.size() + "):");
            for (String f : faces) {
                log.info("   • " + f);
            }
        }
        return r;
    }
}
