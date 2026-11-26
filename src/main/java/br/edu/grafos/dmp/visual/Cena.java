package br.edu.grafos.dmp.visual;

import br.edu.grafos.dmp.modelo.Passo;
import java.util.List;

/** Um cenário completo (nome + sequência de passos) para a visualização. */
public record Cena(String nome, List<Passo> passos) { }
