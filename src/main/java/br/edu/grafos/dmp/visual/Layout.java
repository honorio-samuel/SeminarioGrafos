package br.edu.grafos.dmp.visual;

import br.edu.grafos.dmp.modelo.Grafo;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Posições dos vértices no desenho (coordenadas em [0,1]).
 * <ul>
 *   <li>{@link #circular}: desenho ingênuo (vértices em círculo), costuma ter cruzamentos;</li>
 *   <li>{@link #otimizado}: tenta várias disposições "por forças" e fica com a de menos cruzamentos.
 *       É uma heurística só para desenhar; quem decide a planaridade é o DMP.</li>
 * </ul>
 */
public final class Layout {

    private static final Map<Grafo, double[][]> CACHE = new IdentityHashMap<>();

    private Layout() { }

    public static double[][] circular(Grafo g) {
        int n = g.numVertices();
        double[][] p = new double[n][2];
        for (int i = 0; i < n; i++) {
            double ang = -Math.PI / 2 + 2 * Math.PI * i / Math.max(n, 1);
            p[i][0] = 0.5 + 0.5 * Math.cos(ang);
            p[i][1] = 0.5 + 0.5 * Math.sin(ang);
        }
        return p;
    }

    public static synchronized double[][] otimizado(Grafo g) {
        return CACHE.computeIfAbsent(g, Layout::calcular);
    }

    private static double[][] calcular(Grafo g) {
        int n = g.numVertices();
        if (n <= 2) {
            double[][] p = new double[n][2];
            for (int i = 0; i < n; i++) {
                p[i][0] = n == 1 ? 0.5 : 0.2 + 0.6 * i;
                p[i][1] = 0.5;
            }
            return p;
        }
        List<int[]> arestas = g.arestas();
        int tentativas = n <= 12 ? 25 : n <= 30 ? 6 : 2;
        double[][] melhor = null;
        double melhorCusto = Double.MAX_VALUE;
        for (int s = 0; s < tentativas; s++) {
            double[][] p = forcas(n, arestas, s);
            polir(p, arestas, new Random(1000 + s));
            double custo = custo(p, arestas);
            if (custo < melhorCusto) {
                melhorCusto = custo;
                melhor = p;
            }
        }
        return melhor;
    }

    /** Custo = cruzamentos (peso alto) + estética (vértices afastados, arestas curtas). */
    private static double custo(double[][] p, List<int[]> arestas) {
        double c = 1000.0 * contar(arestas, p);
        int n = p.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = Math.max(Math.hypot(p[i][0] - p[j][0], p[i][1] - p[j][1]), 0.02);
                c += 0.02 / (d * d);
            }
        }
        for (int[] a : arestas) {
            double d = Math.hypot(p[a[0]][0] - p[a[1]][0], p[a[0]][1] - p[a[1]][1]);
            c += 0.5 * d * d;
            // Uma aresta não pode passar colada em outro vértice (seria um cruzamento "escondido").
            for (int v = 0; v < n; v++) {
                if (v != a[0] && v != a[1]) {
                    double folga = distanciaSegmento(p[v], p[a[0]], p[a[1]]);
                    if (folga < 0.07) {
                        c += 1000.0 * (1.0 + (0.07 - folga) / 0.07);
                    }
                }
            }
        }
        return c;
    }

    private static double distanciaSegmento(double[] q, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((q[0] - a[0]) * dx + (q[1] - a[1]) * dy) / len2));
        return Math.hypot(q[0] - (a[0] + t * dx), q[1] - (a[1] + t * dy));
    }

    /** Busca local: move um vértice por vez e só aceita se o custo diminuir. */
    private static void polir(double[][] p, List<int[]> arestas, Random r) {
        int n = p.length;
        double atual = custo(p, arestas);
        int iteracoes = n <= 12 ? 4000 : 1500;
        for (int it = 0; it < iteracoes; it++) {
            double sigma = 0.3 * (1.0 - (double) it / iteracoes) + 0.02;
            int v = r.nextInt(n);
            double ox = p[v][0];
            double oy = p[v][1];
            p[v][0] = Math.min(1, Math.max(0, ox + r.nextGaussian() * sigma));
            p[v][1] = Math.min(1, Math.max(0, oy + r.nextGaussian() * sigma));
            double novo = custo(p, arestas);
            if (novo < atual) {
                atual = novo;
            } else {
                p[v][0] = ox;
                p[v][1] = oy;
            }
        }
        double[][] q = normalizar(p);
        for (int i = 0; i < n; i++) {
            p[i][0] = q[i][0];
            p[i][1] = q[i][1];
        }
    }

    /** Fruchterman-Reingold simplificado, com semente fixa (resultado reproduzível). */
    private static double[][] forcas(int n, List<int[]> arestas, long semente) {
        Random r = new Random(semente * 7919L + 13);
        double[][] p = new double[n][2];
        for (int i = 0; i < n; i++) {
            p[i][0] = r.nextDouble();
            p[i][1] = r.nextDouble();
        }
        double k = Math.sqrt(1.0 / n);
        double t = 0.1;
        for (int it = 0; it < 300; it++) {
            double[][] d = new double[n][2];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = p[i][0] - p[j][0];
                    double dy = p[i][1] - p[j][1];
                    double dist = Math.max(Math.hypot(dx, dy), 1e-4);
                    double f = k * k / dist;
                    d[i][0] += dx / dist * f;
                    d[i][1] += dy / dist * f;
                    d[j][0] -= dx / dist * f;
                    d[j][1] -= dy / dist * f;
                }
            }
            for (int[] a : arestas) {
                double dx = p[a[0]][0] - p[a[1]][0];
                double dy = p[a[0]][1] - p[a[1]][1];
                double dist = Math.max(Math.hypot(dx, dy), 1e-4);
                double f = dist * dist / k;
                d[a[0]][0] -= dx / dist * f;
                d[a[0]][1] -= dy / dist * f;
                d[a[1]][0] += dx / dist * f;
                d[a[1]][1] += dy / dist * f;
            }
            for (int i = 0; i < n; i++) {
                double len = Math.hypot(d[i][0], d[i][1]);
                if (len > 0) {
                    double s = Math.min(len, t) / len;
                    p[i][0] += d[i][0] * s;
                    p[i][1] += d[i][1] * s;
                }
            }
            t *= 0.985;
        }
        return normalizar(p);
    }

    private static double[][] normalizar(double[][] p) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] q : p) {
            minX = Math.min(minX, q[0]);
            maxX = Math.max(maxX, q[0]);
            minY = Math.min(minY, q[1]);
            maxY = Math.max(maxY, q[1]);
        }
        double escala = Math.max(Math.max(maxX - minX, maxY - minY), 1e-9);
        double offX = (1 - (maxX - minX) / escala) / 2;
        double offY = (1 - (maxY - minY) / escala) / 2;
        double[][] r = new double[p.length][2];
        for (int i = 0; i < p.length; i++) {
            r[i][0] = (p[i][0] - minX) / escala + offX;
            r[i][1] = (p[i][1] - minY) / escala + offY;
        }
        return r;
    }

    private static int contar(List<int[]> arestas, double[][] p) {
        int c = 0;
        for (int i = 0; i < arestas.size(); i++) {
            for (int j = i + 1; j < arestas.size(); j++) {
                if (ponto(arestas.get(i), arestas.get(j), p) != null) {
                    c++;
                }
            }
        }
        return c;
    }

    /** Pontos onde arestas (sem extremo comum) se cruzam no desenho dado. */
    public static List<double[]> cruzamentos(Grafo g, double[][] p) {
        List<int[]> arestas = g.arestas();
        List<double[]> pontos = new ArrayList<>();
        for (int i = 0; i < arestas.size(); i++) {
            for (int j = i + 1; j < arestas.size(); j++) {
                double[] x = ponto(arestas.get(i), arestas.get(j), p);
                if (x != null) {
                    pontos.add(x);
                }
            }
        }
        return pontos;
    }

    private static double[] ponto(int[] a, int[] b, double[][] p) {
        if (a[0] == b[0] || a[0] == b[1] || a[1] == b[0] || a[1] == b[1]) {
            return null;
        }
        double[] p1 = p[a[0]], p2 = p[a[1]], p3 = p[b[0]], p4 = p[b[1]];
        double o1 = orient(p1, p2, p3), o2 = orient(p1, p2, p4);
        double o3 = orient(p3, p4, p1), o4 = orient(p3, p4, p2);
        if (o1 * o2 < 0 && o3 * o4 < 0) {
            double t = o3 / (o3 - o4);
            return new double[] {p1[0] + t * (p2[0] - p1[0]), p1[1] + t * (p2[1] - p1[1])};
        }
        return null;
    }

    private static double orient(double[] a, double[] b, double[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    }
}
