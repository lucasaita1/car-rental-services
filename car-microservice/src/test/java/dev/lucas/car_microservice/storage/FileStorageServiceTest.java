package dev.lucas.car_microservice.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    Path tempDir;

    private FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(tempDir.toString());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/jpeg", content);
    }

    @Test
    @DisplayName("Salva JPEG, PNG e WebP pela assinatura real do arquivo")
    void storesImagesByMagicBytes() throws Exception {
        String jpg = storage.storeImage(file("a.jpg", JPEG), "cars");
        String png = storage.storeImage(file("b.png", PNG), "cars");
        String webp = storage.storeImage(file("c.webp", WEBP), "cars");

        assertThat(jpg).matches("cars/[0-9a-f-]{36}\\.jpg");
        assertThat(png).endsWith(".png");
        assertThat(webp).endsWith(".webp");
        assertThat(Files.readAllBytes(tempDir.resolve(jpg))).isEqualTo(JPEG);
    }

    @Test
    @DisplayName("Ignora a extensão e o nome enviados pelo cliente")
    void ignoresClientFilename() {
        String stored = storage.storeImage(file("../../etc/passwd.exe", PNG), "cars");

        assertThat(stored).startsWith("cars/").endsWith(".png").doesNotContain("passwd");
    }

    @Test
    @DisplayName("Texto disfarçado de .jpg é recusado com 415")
    void rejectsFakeImage() {
        assertThatThrownBy(() -> storage.storeImage(file("foto.jpg", "<?php echo 1; ?>".getBytes()), "cars"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415");
    }

    @Test
    @DisplayName("Arquivo vazio é recusado com 400")
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> storage.storeImage(file("x.jpg", new byte[0]), "cars"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("Arquivo acima de 5 MB é recusado com 413")
    void rejectsLargeFile() {
        byte[] big = new byte[(int) FileStorageService.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);

        assertThatThrownBy(() -> storage.storeImage(file("x.jpg", big), "cars"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("413");
    }

    @Test
    @DisplayName("Remove o arquivo salvo")
    void deletesStoredFile() {
        String stored = storage.storeImage(file("a.jpg", JPEG), "cars");

        storage.delete(stored);

        assertThat(tempDir.resolve(stored)).doesNotExist();
    }

    @Test
    @DisplayName("Não remove nada fora do padrão, bloqueando ../")
    void refusesTraversalOnDelete() throws Exception {
        Path outside = Files.writeString(tempDir.resolve("importante.txt"), "x");

        storage.delete("../importante.txt");
        storage.delete("cars/../importante.txt");
        storage.delete(null);

        assertThat(outside).exists();
    }

    @Test
    @DisplayName("Monta a URL pública a partir do caminho relativo")
    void buildsPublicUrl() {
        assertThat(FileStorageService.publicUrl("cars/a.jpg")).isEqualTo("/files/cars/a.jpg");
        assertThat(FileStorageService.publicUrl(null)).isNull();
    }
}
