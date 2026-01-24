package ru.spbpu.weather.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "days")
public class Day {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "date")
    private int date;

    @Column(name = "temperature")
    private int temperature;

    @Column(name = "wind")
    private int wind;

    @ManyToOne
    @JoinColumn(name = "weather_id", referencedColumnName = "id")
    private Weather weather;
}