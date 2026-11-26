package br.edu.grafos.dmp.visual;

import br.edu.grafos.dmp.modelo.Passo;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Gera uma imagem PNG por passo (ótimo para colar nos slides). */
public final class ExportadorPng {

    private ExportadorPng() { }

    public static List<Path> exportar(List<Cena> cenas, Path pasta) throws IOException {
        System.setProperty("java.awt.headless", "true");
        List<Path> gerados = new ArrayList<>();
        for (int c = 0; c < cenas.size(); c++) {
            Cena cena = cenas.get(c);
            Path dir = pasta.resolve("cenario" + (c + 1));
            Files.createDirectories(dir);
            List<Passo> passos = cena.passos();
            for (int i = 0; i < passos.size(); i++) {
                BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                RenderizadorPasso.desenhar(g, 1280, 720, passos.get(i), i, passos.size(), cena.nome());
                g.dispose();
                Path arq = dir.resolve(String.format("passo_%02d.png", i + 1));
                ImageIO.write(img, "png", arq.toFile());
                gerados.add(arq);
            }
        }
        return gerados;
    }
}
