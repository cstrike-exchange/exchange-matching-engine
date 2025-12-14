package org.louisjohns32.personal.exchange.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.louisjohns32.personal.exchange.common.domain.Side;

@Getter
public class OrderRequestDTO {

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Quantity must be greater than 0")
    private Double quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Side is required")
    private Side side;

    @NotBlank(message = "Symbol is required")
    @Size(min = 3, max = 5, message = "Symbol must be between 3 and 5 characters")
    @Pattern(regexp = "^[A-Z]+$", message = "Symbol must be uppercase alphabetical characters only")
    private String symbol;

    public OrderRequestDTO() {}

    public OrderRequestDTO(Double quantity, Double price, Side side, String symbol) {
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.symbol = symbol;
    }

}