package com.windowstabs;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Exibe duas imagens na mesma janela.
 * Janela mais larga que alta: lado a lado. Janela mais alta que larga: uma sobre a outra.
 * Cada imagem se ajusta ao seu espaco mantendo a proporcao.
 */
final class DualImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 2;

    private final ImagePanel first;
    private final ImagePanel second;

    DualImagePanel(ImagePanel first, ImagePanel second) {
        super(null); // layout manual feito em doLayout()
        this.first = first;
        this.second = second;
        setBackground(new Color(40, 40, 40));
        add(first);
        add(second);
        setPreferredSize(new Dimension(800, 400));
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();

        if (w >= h) {
            int firstW = (w - GAP) / 2;
            first.setBounds(0, 0, firstW, h);
            second.setBounds(firstW + GAP, 0, w - firstW - GAP, h);
        } else {
            int firstH = (h - GAP) / 2;
            first.setBounds(0, 0, w, firstH);
            second.setBounds(0, firstH + GAP, w, h - firstH - GAP);
        }
    }
}
