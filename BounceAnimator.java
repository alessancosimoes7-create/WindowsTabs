package com.windowstabs;

import javax.swing.Timer;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Faz as janelas se moverem sozinhas pela tela e quicarem nas bordas,
 * como o logo do DVD na tela de descanso.
 *
 * - Cada janela tem direcao e velocidade proprias (sorteadas ao iniciar).
 * - Ao bater numa borda da area util da tela (sem a barra de tarefas), inverte a direcao
 *   naquele eixo. Batendo em duas bordas ao mesmo tempo, e a "quina perfeita".
 * - Se voce arrastar uma janela com o mouse, ela para enquanto voce arrasta e volta a
 *   se mover cerca de 1 segundo depois de soltar.
 * - Janelas maximizadas nao se movem.
 *
 * Todos os metodos devem ser chamados na thread de eventos do Swing (EDT).
 */
final class BounceAnimator {

    /** Intervalo entre atualizacoes (~60 quadros por segundo). */
    private static final int TICK_MS = 16;

    /** Velocidade em pixels por segundo. Mude aqui para deixar mais rapido ou mais lento. */
    private static final double MIN_SPEED = 150.0;
    private static final double MAX_SPEED = 230.0;

    /** Diferenca (em pixels) para considerar que o usuario arrastou a janela. */
    private static final int DRAG_TOLERANCE = 4;
    private static final long PAUSE_AFTER_DRAG_NS = 1_200_000_000L;

    private final List<Mover> movers = new ArrayList<>();
    private final Timer timer;
    private long lastTickNs;

    BounceAnimator(ImageWindow... windows) {
        Random random = new Random();
        long now = System.nanoTime();
        for (ImageWindow window : windows) {
            movers.add(new Mover(window, randomVelocity(random), randomVelocity(random), now));
        }
        timer = new Timer(TICK_MS, event -> tick());
        timer.setCoalesce(true);
    }

    void start() {
        lastTickNs = System.nanoTime();
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        // Limita o dt para a janela nao "pular" se o computador travar por um instante.
        double dt = Math.min((now - lastTickNs) / 1_000_000_000.0, 0.1);
        lastTickNs = now;
        for (Mover mover : movers) {
            mover.update(dt, now);
        }
    }

    private static double randomVelocity(Random random) {
        double speed = MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED);
        return random.nextBoolean() ? speed : -speed;
    }

    /** Estado de movimento de uma janela. */
    private static final class Mover {
        private final ImageWindow window;
        private double vx;
        private double vy;
        private double x;
        private double y;
        private int lastX;
        private int lastY;
        private long pausedUntilNs;
        private boolean initialized = false;

        Mover(ImageWindow window, double vx, double vy, long now) {
            this.window = window;
            this.vx = vx;
            this.vy = vy;
            this.pausedUntilNs = now;
        }

        void update(double dt, long now) {
            Point current = window.getLocation();

            if (!initialized) {
                sync(current);
                initialized = true;
            }

            // Janela oculta ou maximizada: apenas acompanha a posicao, sem mover.
            if (!window.isShowing() || window.getExtendedState() != Frame.NORMAL) {
                sync(current);
                return;
            }

            // O usuario arrastou a janela: pausa um pouco e continua de onde ela foi solta.
            if (Math.abs(current.x - lastX) > DRAG_TOLERANCE
                    || Math.abs(current.y - lastY) > DRAG_TOLERANCE) {
                sync(current);
                pausedUntilNs = now + PAUSE_AFTER_DRAG_NS;
                return;
            }
            if (now - pausedUntilNs < 0) {
                sync(current);
                return;
            }

            Rectangle area = usableArea();
            double minX = area.x;
            double minY = area.y;
            double maxX = area.x + Math.max(0, area.width - window.getWidth());
            double maxY = area.y + Math.max(0, area.height - window.getHeight());

            x += vx * dt;
            y += vy * dt;

            if (x < minX) {
                x = minX;
                vx = Math.abs(vx);
            } else if (x > maxX) {
                x = maxX;
                vx = -Math.abs(vx);
            }

            if (y < minY) {
                y = minY;
                vy = Math.abs(vy);
            } else if (y > maxY) {
                y = maxY;
                vy = -Math.abs(vy);
            }

            lastX = (int) Math.round(x);
            lastY = (int) Math.round(y);
            if (lastX != current.x || lastY != current.y) {
                window.setLocation(lastX, lastY);
            }
        }

        private void sync(Point current) {
            x = current.x;
            y = current.y;
            lastX = current.x;
            lastY = current.y;
        }

        /** Area do monitor onde a janela esta, sem a barra de tarefas. */
        private Rectangle usableArea() {
            GraphicsConfiguration gc = window.getGraphicsConfiguration();
            if (gc == null) {
                return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            }
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom);
        }
    }
}
