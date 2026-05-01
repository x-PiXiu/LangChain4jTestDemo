package the_19;

/**
 * 天气信息 POJO（配合 RobustWeatherOutputParser 使用）
 */
public class WeatherInfo {

    private String city;
    private double temperature;
    private String condition;
    private int humidity;
    private String wind;

    public WeatherInfo() {
    }

    public WeatherInfo(String city, double temperature, String condition, int humidity, String wind) {
        this.city = city;
        this.temperature = temperature;
        this.condition = condition;
        this.humidity = humidity;
        this.wind = wind;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    @Override
    public String toString() {
        return "WeatherInfo{city='" + city + "', temperature=" + temperature +
                ", condition='" + condition + "', humidity=" + humidity +
                ", wind='" + wind + "'}";
    }
}
