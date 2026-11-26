package br.edu.grafos.dmp.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Subgrafo planar parcial H, já "desenhado" no plano, mantido como conjunto de faces. */
public class SubgrafoPlanar {

    private final Grafo grafo;
    private final Set<Integer> vertices = new TreeSet<>();
    private final Set<Long> arestas = new HashSet<>();
    private final List<Face> faces = new ArrayList<>();
    private int proximoIdFace = 1;

    private SubgrafoPlanar(Grafo grafo) {
        this.grafo = grafo;
    }

    /** H inicial = ciclo base, com 2 faces: interna e externa (mesma fronteira, sentidos opostos). */
    public static SubgrafoPlanar deCiclo(Grafo g, List<Integer> ciclo) {
        SubgrafoPlanar h = new SubgrafoPlanar(g);
        for (int i = 0; i < ciclo.size(); i++) {
            int u = ciclo.get(i);
            int v = ciclo.get((i + 1) % ciclo.size());
            h.vertices.add(u);
            h.arestas.add(Grafo.chave(u, v));
        }
        List<Integer> inversa = new ArrayList<>(ciclo);
        Collections.reverse(inversa);
        h.faces.add(new Face(h.proximoIdFace++, ciclo));
        h.faces.add(new Face(h.proximoIdFace++, inversa));
        return h;
    }

    public Set<Integer> vertices() { return Collections.unmodifiableSet(vertices); }

    public Set<Long> arestas() { return Collections.unmodifiableSet(arestas); }

    public int numArestas() { return arestas.size(); }

    public List<Face> faces() { return Collections.unmodifiableList(faces); }

    public boolean temVertice(int v) { return vertices.contains(v); }

    public boolean temAresta(int u, int v) { return arestas.contains(Grafo.chave(u, v)); }

    /**
     * {@code dividirFace(Face f, Caminho p)}: insere o caminho p (entre dois vértices de contato
     * distintos de f) dentro de f, dividindo-a em duas novas faces e atualizando vértices e arestas de H.
     *
     * @return as duas novas faces (f deixa de existir)
     * @throws IllegalArgumentException se f ou p forem inválidos (uso incorreto interno)
     */
    public List<Face> dividirFace(Face f, Caminho p) {
        if (!faces.contains(f)) {
            throw new IllegalArgumentException("A face " + f + " não pertence a H.");
        }
        int a = p.inicio();
        int b = p.fim();
        if (a == b || !f.contem(a) || !f.contem(b)) {
            throw new IllegalArgumentException("Extremos do caminho devem ser vértices distintos da face.");
        }
        for (int x : p.internos()) {
            if (vertices.contains(x)) {
                throw new IllegalArgumentException("Vértice interno do caminho já pertence a H.");
            }
        }

        // Rotaciona a face para começar em a; localiza b.
        List<Integer> orig = f.vertices();
        int n = orig.size();
        int ia = orig.indexOf(a);
        List<Integer> rot = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            rot.add(orig.get((ia + k) % n));
        }
        int m = rot.indexOf(b);

        List<Integer> internos = p.internos();
        List<Integer> internosInv = new ArrayList<>(internos);
        Collections.reverse(internosInv);

        // Face 1: a ... b (pelo contorno) e volta por p de b até a.
        List<Integer> v1 = new ArrayList<>(rot.subList(0, m + 1));
        v1.addAll(internosInv);
        // Face 2: b ... a (pelo contorno, passando pelo resto) e vai por p de a até b.
        List<Integer> v2 = new ArrayList<>(rot.subList(m, n));
        v2.add(a);
        v2.addAll(internos);

        faces.remove(f);
        Face f1 = new Face(proximoIdFace++, v1);
        Face f2 = new Face(proximoIdFace++, v2);
        faces.add(f1);
        faces.add(f2);

        List<Integer> pv = p.vertices();
        for (int i = 0; i < pv.size(); i++) {
            vertices.add(pv.get(i));
            if (i + 1 < pv.size()) {
                arestas.add(Grafo.chave(pv.get(i), pv.get(i + 1)));
            }
        }
        return List.of(f1, f2);
    }

    public Grafo grafo() { return grafo; }
}
