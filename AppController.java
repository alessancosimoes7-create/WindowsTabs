package com.windowstabs;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Controla o ciclo de vida das janelas.
 *
 * Nao existe "janela principal": cada janela pode ser fechada sozinha pelo botao X
 * sem afetar as demais. O processo Java so termina quando:
 *   - a ultima janela e fechada; ou
 *   - o usuario pressiona Ctrl + C em qualquer uma das janelas (fecha tudo).
 *
 * Todos os metodos devem ser chamados na thread de eventos do Swing (EDT).
 */
final class AppController {

    private final List<ImageWindow> windows = new ArrayList<>();
    private boolean exiting = false;

    void register(ImageWindow window) {
        windows.add(window);
    }

    /** Chamado por uma janela depois que ela foi fechada (botao X). */
    void windowClosed(ImageWindow window) {
        if (exiting) {
            return;
        }
        windows.remove(window);
        if (windows.isEmpty()) {
            exitProcess();
        }
    }

    /** Fecha todas as janelas e encerra completamente o processo Java. */
    void closeAllAndExit() {
        if (exiting) {
            return;
        }
        exiting = true;
        for (ImageWindow window : new ArrayList<>(windows)) {
            window.dispose();
        }
        windows.clear();
        exitProcess();
    }

    /**
     * Instala o atalho Ctrl + C valido enquanto qualquer janela do programa estiver ativa.
     * Usa um despachante global de teclado, entao funciona independente de qual janela
     * (ou componente) esta com o foco.
     */
    void installCtrlCShortcut() {
        KeyEventDispatcher dispatcher = event -> {
            if (event.getID() == KeyEvent.KEY_PRESSED
                    && event.getKeyCode() == KeyEvent.VK_C
                    && event.isControlDown()
                    && !event.isAltDown()) { // AltGr no Windows aparece como Ctrl+Alt
                closeAllAndExit();
                return true; // evento consumido
            }
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private void exitProcess() {
        System.exit(0);
    }
}
