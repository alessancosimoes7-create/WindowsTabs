package com.windowstabs;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Uma janela real e independente do Windows (JFrame de nivel superior).
 *
 * Comportamento desta versao:
 *   - NAO pode ser fechada (botao X e Alt+F4 sao ignorados);
 *   - fica sempre por cima das outras janelas, mesmo ao clicar em outro lugar da tela;
 *   - se for minimizada (inclusive por Win+D), volta sozinha;
 *   - continua podendo ser movida e redimensionada.
 *
 * A saida do programa e feita apenas com Ctrl + C (veja AppController).
 */
final class ImageWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    ImageWindow(AppController controller, String title, JComponent content) {
        super(title);

        // Botao X / Alt+F4 nao fecham a janela.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Sempre por cima, mesmo sem foco.
        setAlwaysOnTop(true);

        setResizable(true);
        setMinimumSize(new Dimension(200, 150));

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        // Se alguem minimizar a janela, ela e restaurada imediatamente.
        addWindowStateListener(event -> {
            if ((event.getNewState() & Frame.ICONIFIED) != 0) {
                int restored = event.getNewState() & ~Frame.ICONIFIED;
                SwingUtilities.invokeLater(() -> {
                    setExtendedState(restored);
                    setAlwaysOnTop(true);
                    toFront();
                });
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controller.windowClosed(ImageWindow.this);
            }
        });

        controller.register(this);
    }
}
