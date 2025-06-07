package org.example.database.models;

import java.io.Serializable;
import java.util.Date;

public class City implements Comparable<City>, Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private Coordinates coordinates;
    private java.util.Date creationDate;
    private Integer area;
    private Long population;
    private Float metersAboveSeaLevel;
    private Climate climate;
    private Government government;
    private StandardOfLiving standardOfLiving;
    private Human governor;
    private String ownerId;

    public City() {
        this.creationDate = new Date();
    }

    public City(int id, String name, Coordinates coordinates, Date creationDate, Integer area, Long population, Float metersAboveSeaLevel, Climate climate, Government government, StandardOfLiving standardOfLiving, Human governor, String ownerId) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.area = area;
        this.population = population;
        this.metersAboveSeaLevel = metersAboveSeaLevel;
        this.climate = climate;
        this.government = government;
        this.standardOfLiving = standardOfLiving;
        this.governor = governor;
        this.ownerId = ownerId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Coordinates getCoordinates() { return coordinates; }
    public Date getCreationDate() { return creationDate; }
    public Integer getArea() { return area; }
    public Long getPopulation() { return population; }
    public Float getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public Climate getClimate() { return climate; }
    public Government getGovernment() { return government; }
    public StandardOfLiving getStandardOfLiving() { return standardOfLiving; }
    public Human getGovernor() { return governor; }
    public String getOwnerId() { return ownerId; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
    public void setArea(Integer area) { this.area = area; }
    public void setPopulation(Long population) { this.population = population; }
    public void setMetersAboveSeaLevel(Float metersAboveSeaLevel) { this.metersAboveSeaLevel = metersAboveSeaLevel; }
    public void setClimate(Climate climate) { this.climate = climate; }
    public void setGovernment(Government government) { this.government = government; }
    public void setStandardOfLiving(StandardOfLiving standardOfLiving) { this.standardOfLiving = standardOfLiving; }
    public void setGovernor(Human governor) { this.governor = governor; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @Override
    public int compareTo(City other) {
        if (this.metersAboveSeaLevel == null && other.metersAboveSeaLevel == null) {
            return 0;
        }
        if (this.metersAboveSeaLevel == null) {
            return -1;
        }
        if (other.metersAboveSeaLevel == null) {
            return 1;
        }
        return Float.compare(this.metersAboveSeaLevel, other.metersAboveSeaLevel);
    }

    @Override
    public String toString() {
        return "City{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", area=" + area +
                ", population=" + population +
                ", metersAboveSeaLevel=" + metersAboveSeaLevel +
                ", climate=" + climate +
                ", government=" + government +
                ", standardOfLiving=" + standardOfLiving +
                ", governor=" + governor +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }

    public void validate() throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Имя города не может быть null или пустым.");
        if (coordinates == null) throw new IllegalArgumentException("Координаты города не могут быть null.");
        if (creationDate == null) throw new IllegalArgumentException("Дата создания не может быть null.");
        if (area == null || area <= 0) throw new IllegalArgumentException("Площадь города должна быть больше 0.");
        if (population == null || population <= 0) throw new IllegalArgumentException("Население города должно быть больше 0.");
        if (climate == null) throw new IllegalArgumentException("Климат города не может быть null.");
        if (standardOfLiving == null) throw new IllegalArgumentException("Стандарт жизни города не может быть null.");

        coordinates.validate();
        if (governor != null) {
            governor.validate();
        }
    }
}