package br.edu.grafos.dmp.visual;

import br.edu.grafos.dmp.modelo.Fragmento;
import br.edu.grafos.dmp.modelo.Grafo;
import br.edu.grafos.dmp.modelo.Passo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Desenha um {@link Passo} (grafo à esquerda, explicação à direita). Usado pela janela e pelo exportador de PNG. */
public final class RenderizadorPasso {

    private static final Color[] CORES = {
        new Color(0x2563eb), new Color(0xdc2626), new Color(0xd97706),
        new Color(0x7c3aed), new Color(0x0891b2), new Color(0xdb2777)};
    private static final Color TINTA = new Color(0x111827);
    private static final Color CINZA = new Color(0x9ca3af);

    private RenderizadorPasso() { }

    public static void desenhar(Graphics2D g, int w, int h, Passo p, int indice, int total, String cena) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        // Cabeçalho
        g.setColor(corCabecalho(p));
        g.fillRect(0, 0, w, 64);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString(p.titulo(), 24, 40);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        String sub = cena + "   ·   passo " + (indice + 1) + " de " + total;
        g.drawString(sub, w - 24 - g.getFontMetrics().stringWidth(sub), 38);

        int divisor = (int) (w * 0.62);
        desenharGrafo(g, 20, 80, divisor - 40, h - 80 - 70, p);
        desenharLegenda(g, 24, h - 52, p);
        g.setColor(new Color(0xe5e7eb));
        g.fillRect(divisor, 80, 2, h - 100);
        desenharTexto(g, divisor + 20, 90, w - divisor - 40, h - 100, p);
    }

    private static Color corCabecalho(Passo p) {
        return switch (p.tipo()) {
            case BLOQUEIO -> new Color(0xb91c1c);
            case DIAGNOSTICO -> Boolean.TRUE.equals(p.planar()) ? new Color(0x15803d) : new Color(0xb91c1c);
            case CONCLUIDO -> new Color(0x15803d);
            case INFO -> new Color(0x334155);
            default -> new Color(0x1d4ed8);
        };
    }

    // ------------------------------------------------------------------ grafo

    private static void desenharGrafo(Graphics2D g, int x0, int y0, int gw, int gh, Passo p) {
        Grafo gr = p.grafo();
        int n = gr.numVertices();
        if (n == 0) {
            return;
        }
        double[][] pos = p.desenhoIngenuo() ? Layout.circular(gr) : Layout.otimizado(gr);
        int margem = 60;
        double[][] s = new double[n][2];
        for (int i = 0; i < n; i++) {
            s[i][0] = x0 + margem + pos[i][0] * (gw - 2 * margem);
            s[i][1] = y0 + margem / 2.0 + pos[i][1] * (gh - margem - 20);
        }

        Set<Long> aH = p.arestasH();
        Set<Integer> vH = p.verticesH();
        List<Fragmento> frs = p.fragmentos();
        Set<Long> caminho = new HashSet<>();
        if (p.caminho() != null) {
            List<Integer> pv = p.caminho().vertices();
            for (int i = 0; i + 1 < pv.size(); i++) {
                caminho.add(Grafo.chave(pv.get(i), pv.get(i + 1)));
            }
        }

        Stroke fino = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke tracejado = new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {9f, 7f}, 0f);
        Stroke grosso = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke destaque = new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        // 1) arestas fora de H (cinza ou cor do fragmento); 2) arestas de H; 3) caminho escolhido
        for (int[] a : gr.arestas()) {
            long k = Grafo.chave(a[0], a[1]);
            boolean emH = aH != null && aH.contains(k);
            if (emH) {
                continue;
            }
            int fi = fragmentoDaAresta(frs, a[0], a[1], vH, aH);
            if (fi >= 0) {
                g.setColor(corFragmento(fi));
                g.setStroke(tracejado);
            } else {
                g.setColor(aH == null ? new Color(0x475569) : new Color(0xd1d5db));
                g.setStroke(fino);
            }
            linha(g, s, a);
        }
        for (int[] a : gr.arestas()) {
            if (aH != null && aH.contains(Grafo.chave(a[0], a[1]))) {
                g.setColor(TINTA);
                g.setStroke(grosso);
                linha(g, s, a);
            }
        }
        for (int[] a : gr.arestas()) {
            if (caminho.contains(Grafo.chave(a[0], a[1]))) {
                int fi = indiceFragmento(frs, p.fragmentoEscolhido());
                Color c = corFragmento(fi);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
                g.setStroke(destaque);
                linha(g, s, a);
                g.setColor(c);
                g.setStroke(grosso);
                linha(g, s, a);
            }
        }

        // cruzamentos (apenas quando não há H sendo construído)
        if (aH == null || p.tipo() == Passo.Tipo.DIAGNOSTICO) {
            for (double[] c : Layout.cruzamentos(gr, pos)) {
                double cx = x0 + margem + c[0] * (gw - 2 * margem);
                double cy = y0 + margem / 2.0 + c[1] * (gh - margem - 20);
                g.setColor(new Color(0xdc2626));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine((int) cx - 9, (int) cy - 9, (int) cx + 9, (int) cy + 9);
                g.drawLine((int) cx - 9, (int) cy + 9, (int) cx + 9, (int) cy - 9);
            }
        }

        // vértices
        int r = 17;
        for (int v = 0; v < n; v++) {
            int cx = (int) s[v][0];
            int cy = (int) s[v][1];
            // anéis de contato (um por fragmento que toca este vértice)
            int anel = 0;
            for (int fi = 0; fi < frs.size(); fi++) {
                if (frs.get(fi).contatos().contains(v)) {
                    g.setColor(corFragmento(fi));
                    g.setStroke(new BasicStroke(3.5f));
                    int rr = r + 6 + anel * 5;
                    g.drawOval(cx - rr, cy - rr, 2 * rr, 2 * rr);
                    anel++;
                }
            }
            int dono = fragmentoDoVertice(frs, v);
            Color fundo;
            if (vH != null && vH.contains(v)) {
                fundo = new Color(0xbbf7d0);
            } else if (dono >= 0) {
                Color c = corFragmento(dono);
                fundo = new Color(c.getRed(), c.getGreen(), c.getBlue(), 70);
                fundo = misturarComBranco(c);
            } else {
                fundo = new Color(0xe0e7ff);
            }
            g.setColor(fundo);
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            g.setColor(TINTA);
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
        }
        // rótulos (por cima de tudo, com fundo branco translúcido)
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        for (int v = 0; v < n; v++) {
            String nome = gr.nome(v);
            int tw = fm.stringWidth(nome);
            int cx = (int) s[v][0];
            int cy = (int) s[v][1] + r + 19 + (frsToca(frs, v) ? 8 : 0);
            g.setColor(new Color(255, 255, 255, 215));
            g.fillRoundRect(cx - tw / 2 - 4, cy - 13, tw + 8, 18, 8, 8);
            g.setColor(TINTA);
            g.drawString(nome, cx - tw / 2, cy);
        }
    }

    private static boolean frsToca(List<Fragmento> frs, int v) {
        for (Fragmento f : frs) {
            if (f.contatos().contains(v)) {
                return true;
            }
        }
        return false;
    }

    private static Color misturarComBranco(Color c) {
        return new Color((c.getRed() + 255 * 2) / 3, (c.getGreen() + 255 * 2) / 3, (c.getBlue() + 255 * 2) / 3);
    }

    private static void linha(Graphics2D g, double[][] s, int[] a) {
        g.drawLine((int) s[a[0]][0], (int) s[a[0]][1], (int) s[a[1]][0], (int) s[a[1]][1]);
    }

    private static Color corFragmento(int indice) {
        return CORES[Math.max(indice, 0) % CORES.length];
    }

    private static int indiceFragmento(List<Fragmento> frs, int id) {
        for (int i = 0; i < frs.size(); i++) {
            if (frs.get(i).id() == id) {
                return i;
            }
        }
        return 0;
    }

    private static int fragmentoDoVertice(List<Fragmento> frs, int v) {
        for (int i = 0; i < frs.size(); i++) {
            if (frs.get(i).internos().contains(v)) {
                return i;
            }
        }
        return -1;
    }

    private static int fragmentoDaAresta(List<Fragmento> frs, int u, int v, Set<Integer> vH, Set<Long> aH) {
        for (int i = 0; i < frs.size(); i++) {
            Fragmento f = frs.get(i);
            if (f.ehAresta()) {
                if (f.contatos().contains(u) && f.contatos().contains(v)) {
                    return i;
                }
            } else {
                boolean uIn = f.internos().contains(u);
                boolean vIn = f.internos().contains(v);
                if ((uIn && (vIn || f.contatos().contains(v))) || (vIn && f.contatos().contains(u))) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ legenda

    private static void desenharLegenda(Graphics2D g, int x, int y, Passo p) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        boolean construindo = p.arestasH() != null && p.tipo() != Passo.Tipo.DIAGNOSTICO;
        int cx = x;
        if (construindo) {
            g.setColor(TINTA);
            g.setStroke(new BasicStroke(5f));
            g.drawLine(cx, y, cx + 30, y);
            g.drawString("já desenhado (H)", cx + 38, y + 5);
            cx += 170;
            g.setColor(CORES[0]);
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[] {9f, 7f}, 0f));
            g.drawLine(cx, y, cx + 30, y);
            g.setColor(TINTA);
            g.drawString("fragmento (cor própria)", cx + 38, y + 5);
            cx += 200;
            g.setColor(CORES[0]);
            g.setStroke(new BasicStroke(3.5f));
            g.drawOval(cx, y - 8, 16, 16);
            g.setColor(TINTA);
            g.drawString("contato do fragmento", cx + 24, y + 5);
        } else {
            int cruz = Layout.cruzamentos(p.grafo(),
                    p.desenhoIngenuo() ? Layout.circular(p.grafo()) : Layout.otimizado(p.grafo())).size();
            if (cruz > 0) {
                g.setColor(new Color(0xdc2626));
                g.setStroke(new BasicStroke(4f));
                g.drawLine(cx, y - 8, cx + 16, y + 8);
                g.drawLine(cx, y + 8, cx + 16, y - 8);
                g.setColor(TINTA);
                g.drawString(cruz + " cruzamento(s) de trilhas neste desenho"
                        + (p.desenhoIngenuo() ? "" : " (o mínimo que o desenho automático achou)"), cx + 24, y + 5);
            } else {
                g.setColor(new Color(0x15803d));
                g.drawString("✔ nenhum cruzamento de trilhas neste desenho", cx, y + 5);
            }
        }
        // Dica de cores
        g.setStroke(new BasicStroke(1f));
    }

    // ------------------------------------------------------------------ texto

    private static void desenharTexto(Graphics2D g, int x, int y, int largura, int altura, Passo p) {
        Font corpo = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        Font negrito = new Font(Font.SANS_SERIF, Font.BOLD, 15);
        Font pequeno = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        int ly = y + 18;
        int limite = y + altura;

        g.setFont(corpo);
        for (String par : p.texto()) {
            for (String l : quebrar(g.getFontMetrics(), par, largura)) {
                if (ly > limite) {
                    return;
                }
                g.setColor(TINTA);
                g.drawString(l, x, ly);
                ly += 20;
            }
            ly += 8;
        }

        if (!p.fragmentos().isEmpty()) {
            ly += 4;
            g.setFont(negrito);
            g.setColor(TINTA);
            g.drawString("Fragmentos fora de H:", x, ly);
            ly += 22;
            g.setFont(pequeno);
            for (int i = 0; i < p.fragmentos().size(); i++) {
                Fragmento f = p.fragmentos().get(i);
                String t = "Fragmento " + f.id() + " · contatos " + p.grafo().nomesDe(f.contatos())
                        + " → faces admissíveis: " + (f.facesAdmissiveis().isEmpty() ? "NENHUMA (0)"
                        : f.facesAdmissiveis().stream().map(fa -> "F" + fa.id()).toList())
                        + (f.id() == p.fragmentoEscolhido() ? "   ◀ escolhido" : "");
                g.setColor(corFragmento(i));
                g.fillOval(x, ly - 11, 12, 12);
                g.setColor(TINTA);
                for (String l : quebrar(g.getFontMetrics(), t, largura - 20)) {
                    if (ly > limite) {
                        return;
                    }
                    g.drawString(l, x + 20, ly);
                    ly += 17;
                }
                ly += 4;
            }
        }

        if (!p.faces().isEmpty()) {
            ly += 6;
            g.setFont(negrito);
            g.setColor(TINTA);
            g.drawString(p.tipo() == Passo.Tipo.DIAGNOSTICO ? "Faces finais (" + p.faces().size() + "):"
                    : "Faces de H (" + p.faces().size() + "):", x, ly);
            ly += 22;
            g.setFont(pequeno);
            for (String f : p.faces()) {
                for (String l : quebrar(g.getFontMetrics(), "• " + f, largura)) {
                    if (ly > limite) {
                        g.drawString("…", x, ly);
                        return;
                    }
                    g.drawString(l, x, ly);
                    ly += 17;
                }
            }
        }
    }

    private static List<String> quebrar(FontMetrics fm, String texto, int largura) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String teste = atual.length() == 0 ? palavra : atual + " " + palavra;
            if (fm.stringWidth(teste) > largura && atual.length() > 0) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(teste);
            }
        }
        if (atual.length() > 0) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /** Usado só para evitar aviso de import não usado em algumas IDEs. */
    static Map<Integer, Integer> vazio() { return new HashMap<>(); }
}
