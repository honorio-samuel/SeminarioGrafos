package br.edu.grafos.dmp.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Face de um subgrafo planar parcial: ciclo fechado de vértices (ordem de contorno). */
public final class Face {

    private final int id;
    private final List<Integer> vertices;
    private final Set<Integer> conjunto;

    public Face(int id, List<Integer> vertices) {
        this.id = id;
        this.vertices = List.copyOf(vertices);
        this.conjunto = new HashSet<>(vertices);
    }

    public int id() { return id; }

    public List<Integer> vertices() { return vertices; }

    public boolean contem(int v) { return conjunto.contains(v); }

    /** Uma face é admissível para um fragmento se contém TODOS os seus vértices de contato. */
    public boolean contemTodos(Collection<Integer> vs) { return conjunto.containsAll(vs); }

    public String descrever(Grafo g) {
        List<String> ns = new ArrayList<>();
        for (int v : vertices) {
            ns.add(g.nome(v));
        }
        return "F" + id + ": " + String.join("-", ns);
    }

    @Override
    public String toString() { return "F" + id + vertices; }
}
