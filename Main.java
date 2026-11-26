package com.windowstabs;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Ponto de entrada do programa.
 *
 * Abre 3 janelas Swing totalmente independentes (cada uma é um JFrame próprio,
 * com botão próprio na barra de tarefas do Windows):
 *   - Janela 1: imagem 1
 *   - Janela 2: imagem 2
 *   - Janela 3: imagens 3 e 4
 */
public final class Main {

    /** URLs exatamente como pedido. Para trocar uma imagem, edite aqui. */
    static final String IMAGE_1_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRePZwznOs6wE1oUYJ2WFjMdgbmGYWO5WTKlqZp4AJcsVz-ceLXFmMJJg2W&s=10";
    static final String IMAGE_2_URL =
            "https://i.pinimg.com/236x/9f/f8/d4/9ff8d492b22b11ebe2751257f1e3f7cb.jpg";
    static final String IMAGE_3_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQylviA73Ck2-geFMAmT5__eEwS-l6ayvu5g5gFusdX-g&s=10";
    static final String IMAGE_4_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTk08kHAZnEOIr_sXmQ5Gchv8DCDhZcOS1nz1hoExUobw&s=10";

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Este programa precisa de um ambiente grafico (Windows com tela).");
            System.exit(1);
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Se falhar, segue com o visual padrao do Swing.
        }

        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        AppController controller = new AppController();

        ImageWindow window1 = new ImageWindow(
                controller, "Janela 1 - Imagem 1", new ImagePanel(IMAGE_1_URL));

        ImageWindow window2 = new ImageWindow(
                controller, "Janela 2 - Imagem 2", new ImagePanel(IMAGE_2_URL));

        ImageWindow window3 = new ImageWindow(
                controller, "Janela 3 - Imagens 3 e 4",
                new DualImagePanel(new ImagePanel(IMAGE_3_URL), new ImagePanel(IMAGE_4_URL)));

        placeSideBySide(window1, window2, window3);

        controller.installCtrlCShortcut();

        window1.setVisible(true);
        window2.setVisible(true);
        window3.setVisible(true);

        // As janelas passam a se mover sozinhas, quicando nas bordas da tela.
        new BounceAnimator(window1, window2, window3).start();

        // Toca o audio em segundo plano. Se falhar, o motivo aparece no titulo da Janela 1.
        new AudioPlayer(reason -> SwingUtilities.invokeLater(
                () -> window1.setTitle("Janela 1 - Imagem 1 (sem audio: " + shorten(reason) + ")"))).start();
    }

    private static String shorten(String text) {
        return text.length() <= 90 ? text : text.substring(0, 87) + "...";
    }

    /** Posiciona as tres janelas lado a lado, ajustando ao tamanho da tela. */
    private static void placeSideBySide(ImageWindow w1, ImageWindow w2, ImageWindow w3) {
        Rectangle area = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        final int margin = 16;
        final int gap = 12;
        int[] widths = {420, 420, 680};
        int height = 520;

        int totalWidth = widths[0] + widths[1] + widths[2] + gap * 2;
        double factor = Math.min(1.0, (double) (area.width - margin * 2) / totalWidth);
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.max(220, (int) (widths[i] * factor));
        }
        height = Math.max(240, Math.min(height, area.height - margin * 2));

        int y = area.y + Math.max(margin, (area.height - height) / 2);
        int x = area.x + margin;

        ImageWindow[] windows = {w1, w2, w3};
        for (int i = 0; i < windows.length; i++) {
            windows[i].setBounds(x, y, widths[i], height);
            x += widths[i] + gap;
        }
    }
}
