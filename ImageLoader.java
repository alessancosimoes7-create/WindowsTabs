package com.windowstabs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/** Baixa uma imagem de uma URL (com algumas tentativas) e a decodifica. */
final class ImageLoader {

    private static final int MAX_ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private ImageLoader() {
    }

    /** Metodo bloqueante: chame sempre fora da thread de eventos do Swing. */
    static BufferedImage load(String address) throws IOException {
        IOException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return download(address);
            } catch (IOException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(700L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrompido", ie);
                    }
                }
            }
        }
        throw lastError != null ? lastError : new IOException("Falha desconhecida");
    }

    private static BufferedImage download(String address) throws IOException {
        URL url = URI.create(address).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36");
            // O Java nao le WebP/AVIF nativamente, entao pedimos apenas formatos suportados.
            connection.setRequestProperty("Accept", "image/jpeg,image/png,image/gif;q=0.9,*/*;q=0.1");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Servidor respondeu com codigo HTTP " + status);
            }

            try (InputStream in = connection.getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("Formato de imagem nao suportado");
                }
                return image;
            }
        } finally {
            connection.disconnect();
        }
    }
}
