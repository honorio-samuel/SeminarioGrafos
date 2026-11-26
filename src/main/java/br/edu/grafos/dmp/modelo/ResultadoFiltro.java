package br.edu.grafos.dmp.modelo;

/**
 * Resultado de {@code filtroEuler}.
 *
 * @param aplicavel  false quando V &lt; 3 (a desigualdade só vale para V &gt;= 3)
 * @param aprovado   false se E &gt; 3V - 6 (grafo certamente NÃO planar)
 * @param arestas    E do grafo simples
 * @param limite     3V - 6 (0 se não aplicável)
 * @param mensagem   explicação em português
 */
public record ResultadoFiltro(boolean aplicavel, boolean aprovado, int arestas, int limite, String mensagem) { }
