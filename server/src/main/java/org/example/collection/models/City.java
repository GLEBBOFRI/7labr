package org.example.collection.models;

import org.example.collection.exceptions.ValidationException;

public class City implements Comparable<City> {
    private Integer id;
    private String name;
    private Coordinates coordinates;
    private Integer area;
    private Long population;
    private Float metersAboveSeaLevel;
    private Climate climate;
    private Government government;
    private StandardOfLiving standardOfLiving;
    private Human governor;

    // гетеро(ы) и сеттеры для всех полей
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    public Integer getArea() { return area; }
    public void setArea(Integer area) { this.area = area; }
    public Long getPopulation() { return population; }
    public void setPopulation(Long population) { this.population = population; }
    public Float getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public void setMetersAboveSeaLevel(Float metersAboveSeaLevel) { this.metersAboveSeaLevel = metersAboveSeaLevel; }
    public Climate getClimate() { return climate; }
    public void setClimate(Climate climate) { this.climate = climate; }
    public Government getGovernment() { return government; }
    public void setGovernment(Government government) { this.government = government; }
    public StandardOfLiving getStandardOfLiving() { return standardOfLiving; }
    public void setStandardOfLiving(StandardOfLiving standardOfLiving) { this.standardOfLiving = standardOfLiving; }
    public Human getGovernor() { return governor; }
    public void setGovernor(Human governor) { this.governor = governor; }

    // валидация объектовы
    public void validate() throws ValidationException {
        if (name == null || name.isEmpty()) throw new ValidationException("имя не по шаблону кабанчиком переделывать");
        if (coordinates == null) throw new ValidationException("сказали ж нулевая точка запривачена там спавн");
        if (area == null || area <= 0) throw new ValidationException("либо ты хочешь создать город с долгом по площади либо с его отсутствием, переделывай");
        if (population == null || population <= 0) throw new ValidationException("в мире нет зомби апокалипсиса добавь хотяб 1 челика");
        if (climate == null) throw new ValidationException("круто город без климата, отлично придумано!");
        if (standardOfLiving == null) throw new ValidationException("может в твоей жизни все очень хреново но не значит что так должно быть у всех");
    }

    @Override
    public int compareTo(City other) {
        return this.population.compareTo(other.population);
    }

    @Override
    public String toString() {
        return "City{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", area=" + area +
                ", population=" + population +
                ", metersAboveSeaLevel=" + metersAboveSeaLevel +
                ", climate=" + climate +
                ", government=" + government +
                ", standardOfLiving=" + standardOfLiving +
                ", governor=" + governor +
                '}';
    }
}
