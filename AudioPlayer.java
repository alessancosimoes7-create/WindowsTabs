package com.windowstabs;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Toca um MP3 em segundo plano (sem abrir navegador ou outra janela).
 *
 * Ordem de busca do audio:
 *   1. Arquivo "musica.mp3" na mesma pasta do JAR (ou na pasta atual). E a opcao mais confiavel.
 *   2. O link AUDIO_URL. Se for um link direto para MP3, baixa e toca. Se for uma pagina HTML,
 *      procura nela um link de audio (og:audio ou um endereco terminado em .mp3).
 *
 * O audio toca em loop (mude LOOP para false para tocar uma unica vez).
 * Se nao for possivel tocar, chama onFailure com o motivo. O resto do programa continua normal.
 */
final class AudioPlayer {

    /** Link pedido. Se tiver um link direto para o .mp3, pode colocar aqui. */
    static final String AUDIO_URL =
            "https://audio.com/alvaro-galindo/audio/rap-dos-hokages-naruto-a-vontade-do-fogo-nerd-hits";

    /** Nome do arquivo local procurado primeiro. */
    static final String LOCAL_FILE_NAME = "musica.mp3";

    private static final boolean LOOP = true;
    private static final long MAX_DOWNLOAD_BYTES = 80L * 1024 * 1024;
    private static final long MAX_PAGE_BYTES = 4L * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final Pattern OG_AUDIO = Pattern.compile(
            "<meta[^>]+og:audio(?::url|:secure_url)?[^>]+content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MP3_LINK = Pattern.compile(
            "https?://[^\"'\\s<>\\\\]+?\\.mp3[^\"'\\s<>\\\\]*", Pattern.CASE_INSENSITIVE);

    private final Consumer<String> onFailure;

    AudioPlayer(Consumer<String> onFailure) {
        this.onFailure = onFailure;
    }

    /** Inicia em uma thread daemon: nao trava as janelas e nao impede o programa de encerrar. */
    void start() {
        Thread thread = new Thread(this::run, "audio-player");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        try {
            Path file = findLocalFile();
            if (file == null) {
                file = fetchFromUrl(AUDIO_URL, 0);
            }
            play(file);
        } catch (Throwable error) {
            String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            System.err.println("[audio] " + message);
            onFailure.accept(message);
        }
    }

    private void play(Path file) throws IOException, JavaLayerException {
        do {
            long started = System.nanoTime();
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                Player player = new Player(in);
                player.play();
                player.close();
            }
            if (System.nanoTime() - started < 1_000_000_000L) {
                throw new IOException("o arquivo nao parece ser um MP3 valido");
            }
        } while (LOOP);
    }

    private static Path findLocalFile() {
        List<Path> candidates = new ArrayList<>();
        try {
            Path location = Paths.get(AudioPlayer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dir = Files.isDirectory(location) ? location : location.getParent();
            if (dir != null) {
                candidates.add(dir.resolve(LOCAL_FILE_NAME));
            }
        } catch (Exception ignored) {
            // Sem a pasta do JAR, tenta so a pasta atual.
        }
        candidates.add(Paths.get(System.getProperty("user.dir"), LOCAL_FILE_NAME));

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Baixa o audio de um endereco. Se o endereco for uma pagina (HTML/JSON), procura nela
     * o link do audio e baixa esse link (apenas um nivel).
     */
    static Path fetchFromUrl(String address, int depth) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(address).toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,text/html;q=0.8,*/*;q=0.5");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("o servidor respondeu HTTP " + status + " ao buscar o audio");
            }

            String type = connection.getContentType() == null
                    ? "" : connection.getContentType().toLowerCase(Locale.ROOT);
            boolean isPage = type.contains("html") || type.contains("json") || type.startsWith("text/");

            if (!isPage) {
                return saveToTempFile(connection);
            }

            if (depth >= 1) {
                throw new IOException("o link do audio levou a outra pagina, nao a um arquivo");
            }
            String page = readText(connection);
            String link = findAudioLink(page, address);
            if (link == null) {
                throw new IOException("a pagina nao tem link direto de audio; "
                        + "coloque um arquivo " + LOCAL_FILE_NAME + " ao lado do JAR");
            }
            return fetchFromUrl(link, depth + 1);
        } finally {
            connection.disconnect();
        }
    }

    private static String findAudioLink(String page, String baseAddress) {
        String text = page.replace("\\/", "/").replace("\\u0026", "&").replace("&amp;", "&");
        Matcher og = OG_AUDIO.matcher(text);
        if (og.find()) {
            return URI.create(baseAddress).resolve(og.group(1)).toString();
        }
        Matcher mp3 = MP3_LINK.matcher(text);
        if (mp3.find()) {
            return mp3.group();
        }
        return null;
    }

    private static Path saveToTempFile(HttpURLConnection connection) throws IOException {
        Path temp = Files.createTempFile("windowstabs-", ".mp3");
        temp.toFile().deleteOnExit();
        try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("arquivo de audio grande demais");
                }
                out.write(buffer, 0, read);
            }
        }
        return temp;
    }

    private static String readText(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1 && out.size() < MAX_PAGE_BYTES) {
                out.write(buffer, 0, read);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
