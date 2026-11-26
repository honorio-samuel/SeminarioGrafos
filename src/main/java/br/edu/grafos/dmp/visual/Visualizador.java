package br.edu.grafos.dmp.visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Janela para navegar pelos passos do algoritmo: botões, ← → do teclado e seletor de cenário. */
public final class Visualizador extends JFrame {

    private final List<Cena> cenas;
    private int cenaAtual;
    private int passoAtual;
    private final JPanel quadro;
    private final JLabel status = new JLabel();

    private Visualizador(List<Cena> cenas) {
        super("DMP · Planaridade e circuitos impressos");
        this.cenas = cenas;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        quadro = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Cena c = cenas.get(cenaAtual);
                RenderizadorPasso.desenhar((Graphics2D) g, getWidth(), getHeight(),
                        c.passos().get(passoAtual), passoAtual, c.passos().size(), c.nome());
            }
        };
        quadro.setPreferredSize(new Dimension(1280, 720));

        JComboBox<String> seletor = new JComboBox<>(cenas.stream().map(Cena::nome).toArray(String[]::new));
        seletor.addActionListener(e -> {
            cenaAtual = seletor.getSelectedIndex();
            passoAtual = 0;
            atualizar();
        });
        JButton anterior = new JButton("◀ Anterior");
        JButton proximo = new JButton("Próximo ▶");
        anterior.addActionListener(e -> mover(-1));
        proximo.addActionListener(e -> mover(+1));
        for (JButton b : new JButton[] {anterior, proximo}) {
            b.setFont(b.getFont().deriveFont(Font.BOLD, 15f));
        }

        JPanel barra = new JPanel();
        barra.add(new JLabel("Cenário:"));
        barra.add(seletor);
        barra.add(anterior);
        barra.add(proximo);
        barra.add(status);

        JComponent raiz = getRootPane();
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "prox");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "prox");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "ant");
        raiz.getActionMap().put("prox", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { mover(+1); }
        });
        raiz.getActionMap().put("ant", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { mover(-1); }
        });

        setLayout(new BorderLayout());
        add(quadro, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        atualizar();
    }

    private void mover(int delta) {
        int total = cenas.get(cenaAtual).passos().size();
        passoAtual = Math.max(0, Math.min(total - 1, passoAtual + delta));
        atualizar();
    }

    private void atualizar() {
        status.setText("  (use ← → ou espaço)");
        quadro.repaint();
    }

    public static void abrir(List<Cena> cenas) {
        SwingUtilities.invokeLater(() -> new Visualizador(cenas).setVisible(true));
    }
}
