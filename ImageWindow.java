package com.windowstabs;

import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Uma janela real e independente do Windows (JFrame de nivel superior).
 *
 * Por ser um JFrame sem "pai", cada instancia:
 *   - aparece separadamente na barra de tarefas;
 *   - tem seus proprios botoes minimizar / maximizar / fechar;
 *   - pode ser movida e redimensionada livremente.
 */
final class ImageWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    ImageWindow(AppController controller, String title, JComponent content) {
        super(title);

        // Fechar esta janela NAO encerra o programa; apenas libera esta janela.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(200, 150));

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controller.windowClosed(ImageWindow.this);
            }
        });

        controller.register(this);
    }
}
