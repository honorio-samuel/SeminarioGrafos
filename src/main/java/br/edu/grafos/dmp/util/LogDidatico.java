package br.edu.grafos.dmp.util;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Log passo a passo em português para narrar o algoritmo durante o seminário. */
public class LogDidatico {

    private static final int LARGURA = 78;

    private final boolean ativo;
    private final PrintStream saida;
    private final List<String> linhas = new ArrayList<>();

    private LogDidatico(boolean ativo, PrintStream saida) {
        this.ativo = ativo;
        this.saida = saida;
    }

    /** Log que imprime no console (UTF-8). */
    public static LogDidatico console() {
        return new LogDidatico(true, new PrintStream(System.out, true, StandardCharsets.UTF_8));
    }

    /** Log desligado (usado nos testes automatizados). */
    public static LogDidatico silencioso() {
        return new LogDidatico(false, null);
    }

    public boolean ativo() { return ativo; }

    public List<String> linhas() { return Collections.unmodifiableList(linhas); }

    /** Cabeçalho de seção: uma linha só, ex.: "── 2 · Filtro de Euler ────────". */
    public void secao(String titulo) {
        escrever("");
        String cab = "── " + titulo + " ";
        escrever(cab + "─".repeat(Math.max(3, LARGURA - cab.length())));
    }

    /** Linha com recuo padrão. */
    public void info(String texto) { escrever("   " + texto); }

    /** Linha sem recuo (o chamador controla o prefixo). */
    public void linha(String texto) { escrever(texto); }

    /** Caixa de destaque para o veredicto. */
    public void caixa(String texto) {
        escrever("");
        escrever("╔" + "═".repeat(texto.length() + 4) + "╗");
        escrever("║  " + texto + "  ║");
        escrever("╚" + "═".repeat(texto.length() + 4) + "╝");
    }

    private void escrever(String linha) {
        if (!ativo) {
            return;
        }
        linhas.add(linha);
        saida.println(linha);
    }
}
