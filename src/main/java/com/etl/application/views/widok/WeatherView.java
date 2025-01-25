package com.etl.application.views.widok;

import com.etl.application.data.HBaseConnection;
import com.etl.application.data.WeatherData;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Route("weather")
@PermitAll
public class WeatherView extends VerticalLayout {

    private final Grid<WeatherData> weatherGrid = new Grid<>(WeatherData.class);
    private final Map map = new Map();
    private List<WeatherData> allWeatherData = new ArrayList<>();
    private List<WeatherData> latestWeatherData = new ArrayList<>();

    public WeatherView() {

        map.setHeight("400px");
        map.setWidth("800px");


        MenuBar menuBar = createMapControls();


        weatherGrid.setColumns("locationName", "temperatureC", "humidity", "conditionText", "windSpeed", "pressure", "visibility");
        weatherGrid.addComponentColumn(this::createLocationButton).setHeader("Map View");


        ComboBox<String> dateFilter = new ComboBox<>("Sort by date");
        dateFilter.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                filterByRelativeDate(event.getValue());
            }
        });


        add(new Span("Weather Data"), dateFilter, weatherGrid, map, menuBar);


        allWeatherData = fetchWeatherDataFromHBase();
        latestWeatherData = getLatestWeatherData(allWeatherData);
        weatherGrid.setItems(latestWeatherData);


        updateDateFilterOptions(dateFilter);
    }

    private List<WeatherData> fetchWeatherDataFromHBase() {
        List<WeatherData> weatherDataList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);

        try (Connection connection = HBaseConnection.getConnection()) {
            Table table = connection.getTable(TableName.valueOf("weather_location_table"));
            Scan scan = new Scan();
            ResultScanner scanner = table.getScanner(scan);

            for (Result result : scanner) {
                String locationName = Bytes.toString(result.getValue(Bytes.toBytes("location"), Bytes.toBytes("location_name")));
                String latitudeStr = Bytes.toString(result.getValue(Bytes.toBytes("location"), Bytes.toBytes("location_lat")));
                String longitudeStr = Bytes.toString(result.getValue(Bytes.toBytes("location"), Bytes.toBytes("location_lon")));
                String temperatureStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_temp_c")));
                String humidityStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_humidity")));
                String conditionText = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_condition_text")));
                String windSpeedStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_wind_kph")));
                String pressureStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_pressure_mb")));
                String visibilityStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_vis_km")));
                String lastUpdatedStr = Bytes.toString(result.getValue(Bytes.toBytes("weather"), Bytes.toBytes("current_last_updated_epoch")));

                if (locationName == null || lastUpdatedStr == null) {
                    continue;
                }

                long lastUpdated = Long.parseLong(lastUpdatedStr);
                double latitude = latitudeStr != null ? numberFormat.parse(latitudeStr).doubleValue() : 0.0;
                double longitude = longitudeStr != null ? numberFormat.parse(longitudeStr).doubleValue() : 0.0;
                double temperatureC = temperatureStr != null ? numberFormat.parse(temperatureStr).doubleValue() : 0.0;
                int humidity = humidityStr != null ? Integer.parseInt(humidityStr) : 0;
                double windSpeed = windSpeedStr != null ? numberFormat.parse(windSpeedStr).doubleValue() : 0.0;
                double pressure = pressureStr != null ? numberFormat.parse(pressureStr).doubleValue() : 0.0;
                double visibility = visibilityStr != null ? numberFormat.parse(visibilityStr).doubleValue() : 0.0;
                conditionText = conditionText != null ? conditionText : "Unknown";

                latitude = Double.parseDouble(decimalFormat.format(latitude));
                longitude = Double.parseDouble(decimalFormat.format(longitude));
                temperatureC = Double.parseDouble(decimalFormat.format(temperatureC));

                WeatherData currentData = new WeatherData(locationName, temperatureC, humidity, conditionText, latitude, longitude);
                currentData.setWindSpeed(windSpeed);
                currentData.setPressure(pressure);
                currentData.setVisibility(visibility);
                currentData.setLastUpdatedEpoch(lastUpdated);

                weatherDataList.add(currentData);
            }

            scanner.close();
            table.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return weatherDataList;
    }

    private List<WeatherData> getLatestWeatherData(List<WeatherData> weatherDataList) {
        return weatherDataList.stream()
                .collect(Collectors.groupingBy(WeatherData::getLocationName))
                .values()
                .stream()
                .map(dataList -> dataList.stream().max(Comparator.comparingLong(WeatherData::getLastUpdatedEpoch)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void updateDateFilterOptions(ComboBox<String> dateFilter) {
        long currentTime = Instant.now().getEpochSecond();

        List<Long> historicalTimes = allWeatherData.stream()
                .map(WeatherData::getLastUpdatedEpoch)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Now");

        if (!historicalTimes.isEmpty()) {
            long closestMinute = historicalTimes.get(0);
            filterOptions.add((currentTime - closestMinute) / 60 + " minutes ago");
        }
        if (historicalTimes.stream().anyMatch(time -> currentTime - time > 3600)) {
            long closestHour = historicalTimes.stream().filter(time -> currentTime - time > 3600).findFirst().orElse(0L);
            filterOptions.add((currentTime - closestHour) / 3600 + " hours ago");
        }
        if (historicalTimes.stream().anyMatch(time -> currentTime - time > 86400)) {
            long closestDay = historicalTimes.stream().filter(time -> currentTime - time > 86400).findFirst().orElse(0L);
            filterOptions.add((currentTime - closestDay) / 86400 + " days ago");
        }

        dateFilter.setItems(filterOptions);
        dateFilter.setValue("Now");
    }

    private void filterByRelativeDate(String filter) {
        long currentTime = Instant.now().getEpochSecond();
        long thresholdStart;
        long thresholdEnd;

        if (filter.equals("Now")) {
            weatherGrid.setItems(latestWeatherData);
            System.out.println("Selected filter: Now - Showing latest data");
            return;
        }


        if (filter.contains("minutes")) {
            long minutes = Long.parseLong(filter.split(" ")[0]);
            thresholdStart = currentTime - (minutes + 2) * 60;
            thresholdEnd = currentTime - (minutes - 2) * 60;
        } else if (filter.contains("hours")) {
            long hours = Long.parseLong(filter.split(" ")[0]);
            thresholdStart = currentTime - (hours + 1) * 3600;
            thresholdEnd = currentTime - (hours - 1) * 3600;
        } else if (filter.contains("days")) {
            long days = Long.parseLong(filter.split(" ")[0]);
            thresholdStart = currentTime - (days + 1) * 86400;
            thresholdEnd = currentTime - (days - 1) * 86400;
        } else {
            thresholdEnd = currentTime;
            thresholdStart = currentTime;
        }


        System.out.println("Filter: " + filter);
        System.out.println("Threshold Start (epoch): " + thresholdStart);
        System.out.println("Threshold End (epoch): " + thresholdEnd);


        List<WeatherData> filteredData = allWeatherData.stream()
                .filter(data -> data.getLastUpdatedEpoch() >= thresholdStart && data.getLastUpdatedEpoch() <= thresholdEnd)
                .collect(Collectors.toList());


        HashMap<String, WeatherData> uniqueDataMap = new HashMap<>();
        for (WeatherData data : filteredData) {
            String locationKey = data.getLocationName();
            if (!uniqueDataMap.containsKey(locationKey) ||
                    uniqueDataMap.get(locationKey).getLastUpdatedEpoch() < data.getLastUpdatedEpoch()) {
                uniqueDataMap.put(locationKey, data);
            }
        }


        System.out.println("Filtered data count: " + uniqueDataMap.size());
        uniqueDataMap.values().forEach(data ->
                System.out.println("Data: " + data.getLocationName() + ", Last Updated: " + data.getLastUpdatedEpoch()));


        weatherGrid.setItems(new ArrayList<>(uniqueDataMap.values()));
    }





    private Button createLocationButton(WeatherData weatherData) {
        return new Button("Show on Map", event -> moveMapToLocation(weatherData.getLatitude(), weatherData.getLongitude()));
    }

    private void moveMapToLocation(double latitude, double longitude) {
        Coordinate coordinate = new Coordinate(longitude, latitude);
        map.getView().setCenter(coordinate);
        map.getView().setZoom(10);
    }

    private MenuBar createMapControls() {
        MenuBar menuBar = new MenuBar();

        SubMenu moveToSubMenu = menuBar.addItem("Move To...").getSubMenu();
        moveToSubMenu.addItem("Berlin", e -> moveMapToLocation(52.520008, 13.404954));
        moveToSubMenu.addItem("Paris", e -> moveMapToLocation(48.856613, 2.352222));
        moveToSubMenu.addItem("New York", e -> moveMapToLocation(40.712776, -74.005974));

        menuBar.addItem("Zoom In", e -> map.getView().setZoom(map.getView().getZoom() + 1));
        menuBar.addItem("Zoom Out", e -> map.getView().setZoom(map.getView().getZoom() - 1));

        return menuBar;
    }
}