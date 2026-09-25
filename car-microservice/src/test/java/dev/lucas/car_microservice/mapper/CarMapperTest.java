package dev.lucas.car_microservice.mapper;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O CarMapper é a fronteira entre o mundo HTTP (DTOs) e o mundo da persistência
 * (entidade). Um campo esquecido aqui não quebra a compilação: apenas some
 * silenciosamente da resposta da API ou deixa de ser gravado no banco.
 */
class CarMapperTest {

    private static final LocalDate ALUGUEL = LocalDate.of(2026, 3, 10);
    private static final LocalDate DEVOLUCAO = LocalDate.of(2026, 3, 20);

    @Nested
    @DisplayName("toEntity: request da API -> entidade")
    class ToEntity {

        @Test
        @DisplayName("Deve copiar todos os campos do request para a entidade")
        void shouldMapEveryRequestField() {
            CarRequestDto dto = CarRequestDto.builder()
                    .model("Civic")
                    .color("Preto")
                    .plate("ABC-1D23")
                    .year(2024)
                    .rentalDate(ALUGUEL)
                    .returnDate(DEVOLUCAO)
                    .userId(42L)
                    .build();

            CarModel entity = CarMapper.toEntity(dto);

            assertThat(entity.getModel()).isEqualTo("Civic");
            assertThat(entity.getColor()).isEqualTo("Preto");
            assertThat(entity.getPlate()).isEqualTo("ABC-1D23");
            assertThat(entity.getYear()).isEqualTo(2024);
            assertThat(entity.getRentalDate()).isEqualTo(ALUGUEL);
            assertThat(entity.getReturnDate()).isEqualTo(DEVOLUCAO);
            assertThat(entity.getUserId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Não deve propagar id vindo do cliente, para o banco gerar a chave")
        void shouldNotAcceptIdFromClient() {
            CarModel entity = CarMapper.toEntity(CarRequestDto.builder().model("Civic").build());

            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("Deve sanitizar modelo e cor e padronizar a placa em maiúsculas")
        void shouldSanitizeTextFields() {
            CarModel entity = CarMapper.toEntity(CarRequestDto.builder()
                    .model(" <b>Civic</b>  Touring ").color("Preto<script>x</script>").plate(" abc1d23 ").build());

            assertThat(entity.getModel()).isEqualTo("Civic Touring");
            assertThat(entity.getColor()).isEqualTo("Pretox");
            assertThat(entity.getPlate()).isEqualTo("ABC1D23");
        }

        @Test
        @DisplayName("Carro cadastrado sem status deve nascer disponível")
        void shouldDefaultStatusToAvailable() {
            CarModel entity = CarMapper.toEntity(CarRequestDto.builder().model("Civic").build());

            assertThat(entity.getStatus()).isEqualTo(CarStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Deve respeitar o status informado no cadastro")
        void shouldKeepProvidedStatus() {
            CarModel entity = CarMapper.toEntity(CarRequestDto.builder()
                    .model("Civic").status(CarStatus.MAINTENANCE).build());

            assertThat(entity.getStatus()).isEqualTo(CarStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("Deve aceitar datas e usuário nulos no cadastro de um carro novo")
        void shouldAcceptNullOptionalFields() {
            CarRequestDto dto = CarRequestDto.builder()
                    .model("Onix")
                    .color("Branco")
                    .plate("XYZ-9Z99")
                    .year(2022)
                    .build();

            CarModel entity = CarMapper.toEntity(dto);

            assertThat(entity.getRentalDate()).isNull();
            assertThat(entity.getReturnDate()).isNull();
            assertThat(entity.getUserId()).isNull();
            assertThat(entity.getModel()).isEqualTo("Onix");
        }
    }

    @Nested
    @DisplayName("toResponseDto: entidade -> resposta da API")
    class ToResponseDto {

        @Test
        @DisplayName("Deve copiar todos os campos da entidade para a resposta")
        void shouldMapEveryEntityField() {
            CarModel entity = new CarModel(
                    7L, "Civic", "Preto", "ABC-1D23", 2024,
                    ALUGUEL, DEVOLUCAO, CarStatus.RENTED, 42L, null
            );

            CarResponseDto dto = CarMapper.toResponseDto(entity);

            assertThat(dto.getId()).isEqualTo(7L);
            assertThat(dto.getModel()).isEqualTo("Civic");
            assertThat(dto.getColor()).isEqualTo("Preto");
            assertThat(dto.getPlate()).isEqualTo("ABC-1D23");
            assertThat(dto.getYear()).isEqualTo(2024);
            assertThat(dto.getRentalDate()).isEqualTo(ALUGUEL);
            assertThat(dto.getReturnDate()).isEqualTo(DEVOLUCAO);
            assertThat(dto.getUserId()).isEqualTo(42L);
            assertThat(dto.getStatus()).isEqualTo(CarStatus.RENTED);
        }

        @Test
        @DisplayName("Ida e volta pelo mapper deve preservar os dados de negócio")
        void shouldSurviveRoundTrip() {
            CarRequestDto original = CarRequestDto.builder()
                    .model("Corolla")
                    .color("Prata")
                    .plate("QWE-4R56")
                    .year(2025)
                    .rentalDate(ALUGUEL)
                    .returnDate(DEVOLUCAO)
                    .userId(99L)
                    .build();

            CarResponseDto resultado = CarMapper.toResponseDto(CarMapper.toEntity(original));

            assertThat(resultado.getModel()).isEqualTo(original.getModel());
            assertThat(resultado.getColor()).isEqualTo(original.getColor());
            assertThat(resultado.getPlate()).isEqualTo(original.getPlate());
            assertThat(resultado.getYear()).isEqualTo(original.getYear());
            assertThat(resultado.getRentalDate()).isEqualTo(original.getRentalDate());
            assertThat(resultado.getReturnDate()).isEqualTo(original.getReturnDate());
            assertThat(resultado.getUserId()).isEqualTo(original.getUserId());
        }
    }
}
