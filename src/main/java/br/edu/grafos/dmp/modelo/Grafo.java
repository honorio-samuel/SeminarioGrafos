package br.edu.grafos.dmp.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grafo simples não direcionado.
 *
 * <p>Laços (u-u) e arestas paralelas (u-v repetida) são <b>ignorados na inserção</b> e apenas
 * contabilizados; assim, a fórmula de Euler para grafos simples ({@code E <= 3V - 6})
 * é sempre aplicada sobre o grafo já limpo (pré-processamento da entrada).
 */
public class Grafo {

    private final List<String> nomes = new ArrayList<>();
    private final List<TreeSet<Integer>> adjacencia = new ArrayList<>();
    private int numArestas;
    private int lacosIgnorados;
    private int paralelasIgnoradas;

    /** Cria um grafo com {@code n} vértices chamados "v0", "v1", ... */
    public Grafo(int n) {
        for (int i = 0; i < Math.max(n, 0); i++) {
            adicionarVertice("v" + i);
        }
    }

    /** Cria um grafo com vértices nomeados (útil para dar significado a componentes de PCB). */
    public Grafo(List<String> nomesDosVertices) {
        for (String nome : nomesDosVertices) {
            adicionarVertice(nome);
        }
    }

    public int adicionarVertice(String nome) {
        nomes.add(nome);
        adjacencia.add(new TreeSet<>());
        return nomes.size() - 1;
    }

    /**
     * Adiciona a aresta {u, v}.
     *
     * @return {@code true} se a aresta foi realmente inserida; {@code false} se era laço ou paralela
     * @throws IllegalArgumentException se algum índice não existir (erro de programação do chamador)
     */
    public boolean adicionarAresta(int u, int v) {
        validar(u);
        validar(v);
        if (u == v) {
            lacosIgnorados++;
            return false;
        }
        if (adjacencia.get(u).contains(v)) {
            paralelasIgnoradas++;
            return false;
        }
        adjacencia.get(u).add(v);
        adjacencia.get(v).add(u);
        numArestas++;
        return true;
    }

    /** Variante por nome de vértice. */
    public boolean adicionarAresta(String a, String b) {
        return adicionarAresta(indiceDe(a), indiceDe(b));
    }

    public int indiceDe(String nome) {
        int i = nomes.indexOf(nome);
        if (i < 0) {
            throw new IllegalArgumentException("Vértice inexistente: " + nome);
        }
        return i;
    }

    private void validar(int v) {
        if (v < 0 || v >= nomes.size()) {
            throw new IllegalArgumentException("Índice de vértice inválido: " + v);
        }
    }

    public int numVertices() { return nomes.size(); }

    public int numArestas() { return numArestas; }

    public int lacosIgnorados() { return lacosIgnorados; }

    public int paralelasIgnoradas() { return paralelasIgnoradas; }

    public String nome(int v) { return nomes.get(v); }

    public Set<Integer> vizinhos(int v) { return Collections.unmodifiableSet(adjacencia.get(v)); }

    public boolean temAresta(int u, int v) { return adjacencia.get(u).contains(v); }

    /** Lista de arestas como pares {u, v} com u &lt; v. */
    public List<int[]> arestas() {
        List<int[]> lista = new ArrayList<>();
        for (int u = 0; u < numVertices(); u++) {
            for (int v : adjacencia.get(u)) {
                if (u < v) {
                    lista.add(new int[] {u, v});
                }
            }
        }
        return lista;
    }

    /** Componentes conexos (inclui vértices isolados), cada um com vértices em ordem crescente. */
    public List<List<Integer>> componentesConexos() {
        boolean[] visto = new boolean[numVertices()];
        List<List<Integer>> componentes = new ArrayList<>();
        for (int s = 0; s < numVertices(); s++) {
            if (visto[s]) {
                continue;
            }
            List<Integer> comp = new ArrayList<>();
            java.util.ArrayDeque<Integer> fila = new java.util.ArrayDeque<>();
            fila.add(s);
            visto[s] = true;
            while (!fila.isEmpty()) {
                int x = fila.poll();
                comp.add(x);
                for (int w : adjacencia.get(x)) {
                    if (!visto[w]) {
                        visto[w] = true;
                        fila.add(w);
                    }
                }
            }
            Collections.sort(comp);
            componentes.add(comp);
        }
        return componentes;
    }

    /** Subgrafo induzido pelos vértices dados (nomes preservados; índices renumerados). */
    public Grafo subgrafoInduzido(Collection<Integer> vertices) {
        List<Integer> ordenados = new ArrayList<>(new TreeSet<>(vertices));
        List<String> novosNomes = new ArrayList<>();
        for (int v : ordenados) {
            novosNomes.add(nome(v));
        }
        Grafo sub = new Grafo(novosNomes);
        for (int i = 0; i < ordenados.size(); i++) {
            for (int w : adjacencia.get(ordenados.get(i))) {
                int j = ordenados.indexOf(w);
                if (j > i) {
                    sub.adicionarAresta(i, j);
                }
            }
        }
        return sub;
    }

    /** Subgrafo formado apenas pelas arestas dadas e seus extremos (nomes preservados). */
    public Grafo subgrafoDeArestas(List<int[]> arestas) {
        TreeSet<Integer> vs = new TreeSet<>();
        for (int[] a : arestas) {
            vs.add(a[0]);
            vs.add(a[1]);
        }
        List<Integer> ordenados = new ArrayList<>(vs);
        List<String> novosNomes = new ArrayList<>();
        for (int v : ordenados) {
            novosNomes.add(nome(v));
        }
        Grafo sub = new Grafo(novosNomes);
        for (int[] a : arestas) {
            sub.adicionarAresta(ordenados.indexOf(a[0]), ordenados.indexOf(a[1]));
        }
        return sub;
    }

    /** Chave única de uma aresta não direcionada. */
    public static long chave(int u, int v) {
        long a = Math.min(u, v);
        long b = Math.max(u, v);
        return (a << 32) | b;
    }

    /** Texto "{A, B, C}" com os nomes dos vértices. */
    public String nomesDe(Collection<Integer> vertices) {
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (int v : vertices) {
            if (!primeiro) {
                sb.append(", ");
            }
            sb.append(nome(v));
            primeiro = false;
        }
        return sb.append("}").toString();
    }
}
