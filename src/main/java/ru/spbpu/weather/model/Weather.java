package ru.spbpu.weather.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weather")
public class Weather {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "temperature")
    private int temperature;

    @Column(name = "wind")
    private int wind;

    @Column(name = "description")
    private String description;

    @OneToOne
    @JoinColumn(name = "request_id", referencedColumnName = "id")
    private RequestHistoryEntity request;
}