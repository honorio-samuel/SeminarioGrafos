package br.edu.grafos.dmp.demo;

import br.edu.grafos.dmp.modelo.Grafo;
import java.util.List;
import java.util.function.Supplier;

/** Fábrica dos 4 grafos usados na demonstração do seminário (Slide 16). */
public final class Cenarios {

    /** Descrição de um cenário da apresentação. */
    public record Cenario(int numero, String titulo, String roteiro, Supplier<Grafo> grafo) { }

    private Cenarios() { }

    /** Os 4 cenários do Slide 16, na ordem da apresentação. */
    public static List<Cenario> todos() {
        return List.of(
                new Cenario(1, "Planar Enganoso",
                        "Hexágono com cordas que parecem se cruzar. Esperado: PLANAR.",
                        Cenarios::planarEnganoso),
                new Cenario(2, "Bloqueio por Euler (K5)",
                        "V = 5, E = 10 > 3V - 6 = 9. Esperado: NÃO PLANAR já no filtro.",
                        Cenarios::k5),
                new Cenario(3, "Armadilha K3,3 (3 MCUs x 3 Sensores)",
                        "V = 6, E = 9 ≤ 12 passa em Euler. Esperado: NÃO PLANAR no DMP.",
                        Cenarios::k33),
                new Cenario(4, "PCB Simulada",
                        "Placa simplificada (MCU, regulador, sensores...). Esperado: PLANAR.",
                        Cenarios::pcbSimulada));
    }

    /**
     * Cenário 1 - Planar Enganoso: hexágono 0-1-2-3-4-5 com as cordas 0-3, 1-4 e 0-2.
     * Num desenho ingênuo (todas as cordas dentro do hexágono) 0-3 e 1-4 se cruzam,
     * mas o grafo é planar: basta desenhar uma delas por fora.
     */
    public static Grafo planarEnganoso() {
        Grafo g = new Grafo(List.of("A", "B", "C", "D", "E", "F"));
        String[][] ciclo = {{"A", "B"}, {"B", "C"}, {"C", "D"}, {"D", "E"}, {"E", "F"}, {"F", "A"}};
        for (String[] a : ciclo) {
            g.adicionarAresta(a[0], a[1]);
        }
        g.adicionarAresta("A", "D"); // corda longa
        g.adicionarAresta("B", "E"); // corda longa que "cruza" A-D no desenho ingênuo
        g.adicionarAresta("A", "C"); // corda curta
        return g;
    }

    /** Cenário 2 - K5: V = 5, E = 10 > 3*5 - 6 = 9 (barrado pelo filtro de Euler). */
    public static Grafo k5() {
        Grafo g = new Grafo(List.of("N1", "N2", "N3", "N4", "N5"));
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                g.adicionarAresta(i, j);
            }
        }
        return g;
    }

    /** Cenário 3 - K3,3: 3 MCUs ligadas a 3 Sensores (V = 6, E = 9 <= 12, passa em Euler, falha no DMP). */
    public static Grafo k33() {
        Grafo g = new Grafo(List.of("MCU1", "MCU2", "MCU3", "Sensor1", "Sensor2", "Sensor3"));
        for (int m = 0; m < 3; m++) {
            for (int s = 3; s < 6; s++) {
                g.adicionarAresta(m, s);
            }
        }
        return g;
    }

    /**
     * Cenário 4 - PCB simulada (planar): MCU no centro ligada a 5 componentes de uma "malha" em anel,
     * mais duas trilhas extras que correm pelo lado de fora. V = 8, E = 14 <= 18.
     */
    public static Grafo pcbSimulada() {
        Grafo g = new Grafo(List.of("Conector", "Regulador", "MCU", "Sensor1", "Sensor2",
                "Cristal", "LED", "Resistor"));
        // Anel externo (trilhas de alimentação/sinal)
        String[] anel = {"Conector", "Regulador", "Sensor1", "Sensor2", "Cristal", "LED", "Resistor"};
        for (int i = 0; i < anel.length; i++) {
            g.adicionarAresta(anel[i], anel[(i + 1) % anel.length]);
        }
        // MCU no centro
        for (String comp : new String[] {"Regulador", "Sensor1", "Sensor2", "Cristal", "Resistor"}) {
            g.adicionarAresta("MCU", comp);
        }
        // Trilhas extras por fora do anel
        g.adicionarAresta("Conector", "Sensor1"); // GND comum
        g.adicionarAresta("Cristal", "Resistor"); // clock/pull-up
        return g;
    }
}
