package com.windowstabs;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Painel que baixa uma imagem em segundo plano e a desenha centralizada,
 * ajustada ao tamanho disponivel e mantendo a proporcao original.
 */
final class ImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private enum Status { LOADING, READY, ERROR }

    private final String url;
    private Status state = Status.LOADING;
    private transient BufferedImage image;
    private String errorMessage = "";

    ImagePanel(String url) {
        this.url = url;
        setOpaque(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(400, 400));

        // Em caso de erro, um clique reinicia o download.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state == Status.ERROR) {
                    startLoading();
                }
            }
        });

        startLoading();
    }

    private void startLoading() {
        state = Status.LOADING;
        setCursor(Cursor.getDefaultCursor());
        repaint();

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageLoader.load(url);
            }

            @Override
            protected void done() {
                try {
                    image = get();
                    state = Status.READY;
                    setCursor(Cursor.getDefaultCursor());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    state = Status.ERROR;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                repaint();
            }
        }.execute();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            switch (state) {
                case READY:
                    paintImage(g2);
                    break;
                case ERROR:
                    paintMessage(g2, "Falha ao carregar a imagem", errorMessage, "Clique para tentar novamente");
                    break;
                case LOADING:
                default:
                    paintMessage(g2, "Carregando imagem...", null, null);
                    break;
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintImage(Graphics2D g2) {
        int panelW = getWidth();
        int panelH = getHeight();
        int imgW = image.getWidth();
        int imgH = image.getHeight();
        if (panelW <= 0 || panelH <= 0 || imgW <= 0 || imgH <= 0) {
            return;
        }

        // Escala unica para largura e altura = proporcao preservada.
        double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
        int drawW = Math.max(1, (int) Math.round(imgW * scale));
        int drawH = Math.max(1, (int) Math.round(imgH * scale));
        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, null);
    }

    private void paintMessage(Graphics2D g2, String line1, String line2, String line3) {
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = {line1, line2, line3};
        int count = 0;
        for (String line : lines) {
            if (line != null) {
                count++;
            }
        }
        int lineHeight = fm.getHeight();
        int y = (getHeight() - lineHeight * count) / 2 + fm.getAscent();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            int x = Math.max(4, (getWidth() - fm.stringWidth(line)) / 2);
            g2.drawString(line, x, y);
            y += lineHeight;
        }
    }
}
