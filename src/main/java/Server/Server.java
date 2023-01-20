package Server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService executorService = Executors.newFixedThreadPool(64);
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            while (true) {
                System.out.println("Server is listening");
                Socket socket = serverSocket.accept();
                executorService.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handle(Socket socket) {

        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream());
                socket
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            System.out.printf("Поток %s обрабатывает запрос %n, %s%n", Thread.currentThread().getName(), requestLine);
            if (parts.length != 3) {
                // just close socket
                return;
            }
            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
                return;
            }
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
                return;
            }
            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
            System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}