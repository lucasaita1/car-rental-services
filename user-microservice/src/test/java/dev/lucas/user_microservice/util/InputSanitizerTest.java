package dev.lucas.user_microservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    @Test
    @DisplayName("Remove tags HTML, caracteres de controle e espaços extras")
    void cleansText() {
        assertThat(InputSanitizer.text("  <b>Ana</b>\t\n  <img src=x onerror=alert(1)>Souza  ")).isEqualTo("Ana Souza");
        assertThat(InputSanitizer.text("Ana < Souza")).isEqualTo("Ana Souza");
        assertThat(InputSanitizer.text("a < b > c")).isEqualTo("a c");
        assertThat(InputSanitizer.text(null)).isNull();
    }

    @Test
    @DisplayName("Normaliza e-mail em minúsculas e sem espaços")
    void normalizesEmail() {
        assertThat(InputSanitizer.email("  Ana@Exemplo.COM ")).isEqualTo("ana@exemplo.com");
        assertThat(InputSanitizer.email(null)).isNull();
    }

    @Test
    @DisplayName("Mantém só os dígitos de documentos")
    void keepsDigits() {
        assertThat(InputSanitizer.digits("123.456.789-00")).isEqualTo("12345678900");
        assertThat(InputSanitizer.digits("abc")).isNull();
        assertThat(InputSanitizer.digits(null)).isNull();
    }
}
