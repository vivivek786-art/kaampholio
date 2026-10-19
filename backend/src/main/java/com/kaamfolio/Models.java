package com.kaamfolio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public final class Models {
    private Models() {}
    public record Work(
        @NotBlank @Size(max=80) String id,
        @NotBlank @Size(max=180) String title,
        @NotBlank @Size(max=4000) String description,
        @NotNull @Pattern(regexp="image|video") String kind,
        @NotBlank @Size(max=300) String src,
        @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> tools) {}
    public record Professional(
        @NotBlank @Size(max=80) String id,
        @NotBlank @Size(max=120) String name,
        @NotNull @Pattern(regexp="Plumber|Electrician|Carpenter|Painter|AC technician") String trade,
        @NotBlank @Size(max=100) String city,
        @NotBlank @Size(max=150) String area,
        @Min(0) @Max(60) int experience,
        @DecimalMin("0") @DecimalMax("5") double rating,
        @Min(0) int reviews,
        @Min(0) @Max(100000) int rate,
        boolean available,
        @NotNull @Size(min=1,max=50) List<@NotBlank @Size(max=100) String> skills,
        @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> tools,
        @NotNull @Size(max=4000) String bio,
        @NotNull @Size(max=30) List<@Valid Work> work) {}
    public record State(
        @Min(1) @Max(1) int version,
        @Min(0) long revision,
        @NotNull @Valid Professional profile,
        @NotNull @Size(max=1000) List<@NotBlank @Size(max=80) String> saved) {}
}
