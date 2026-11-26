package br.edu.grafos.dmp.modelo;

import java.util.List;

/**
 * Caminho p = (a, x1, ..., xk, b) que liga dois vértices de contato a e b através de um fragmento.
 * Os vértices internos x1..xk ainda não estão em H (lista vazia quando o fragmento é uma única aresta).
 */
public record Caminho(List<Integer> vertices) {

    public Caminho {
        vertices = List.copyOf(vertices);
    }

    public int inicio() { return vertices.get(0); }

    public int fim() { return vertices.get(vertices.size() - 1); }

    public List<Integer> internos() { return vertices.subList(1, vertices.size() - 1); }

    public String descrever(Grafo g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(g.nome(vertices.get(i)));
        }
        return sb.toString();
    }
}
