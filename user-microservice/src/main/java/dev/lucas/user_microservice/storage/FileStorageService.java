package dev.lucas.user_microservice.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    public static final String PUBLIC_PREFIX = "/files/";
    static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern STORED_PATH = Pattern.compile("^[a-z]+/[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private final Path root;

    public FileStorageService(@Value("${STORAGE_DIR:storage}") String storageDir) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo de imagem.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem deve ter no máximo 5 MB.");
        }

        String extension = detectImageExtension(file);
        String relativePath = folder + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(relativePath);

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a imagem.");
        }
        return relativePath;
    }

    public void delete(String relativePath) {
        if (relativePath == null || !STORED_PATH.matcher(relativePath).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ignored) {
        }
    }

    public static String publicUrl(String relativePath) {
        return relativePath == null ? null : PUBLIC_PREFIX + relativePath;
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de arquivo inválido.");
        }
        return path;
    }

    private static String detectImageExtension(MultipartFile file) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo ilegível.");
        }
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && Arrays.equals(Arrays.copyOf(header, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "png";
        }
        if (read >= 12 && new String(header, 0, 4).equals("RIFF") && new String(header, 8, 4).equals("WEBP")) {
            return "webp";
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado. Use JPG, PNG ou WebP.");
    }
}
