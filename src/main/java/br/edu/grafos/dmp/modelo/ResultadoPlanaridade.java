package br.edu.grafos.dmp.modelo;

import java.util.List;

/** Diagnóstico final do teste de planaridade. */
public record ResultadoPlanaridade(
        boolean planar,
        Etapa etapaDecisiva,
        String motivo,
        String interpretacaoPCB,
        List<String> facesFinais,
        int vertices,
        int arestas,
        int componentes,
        int lacosIgnorados,
        int paralelasIgnoradas,
        List<Passo> passos) {

    /** Etapa que decidiu o resultado. */
    public enum Etapa {
        /** Grafo com menos de 3 vértices (ou vazio). */
        TRIVIAL,
        /** Violou E &lt;= 3V - 6. */
        FILTRO_EULER,
        /** Floresta: nenhum ciclo, nada a desenhar além de segmentos. */
        ACICLICO,
        /** Decidido pelo loop do DMP. */
        DMP
    }

    public String diagnostico() { return planar ? "Planar" : "Não Planar"; }
}
