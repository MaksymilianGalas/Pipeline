package com.etl.application.services;

import com.etl.application.data.HBaseConnection;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseService {

    public void readWeatherData() {
        try (Connection connection = HBaseConnection.getConnection()) {
            Table table = connection.getTable(TableName.valueOf("weather_data"));
            Scan scan = new Scan();
            ResultScanner scanner = table.getScanner(scan);

            for (Result result : scanner) {
                String rowKey = Bytes.toString(result.getRow());
                String locationInfo = Bytes.toString(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("location_info")));
                System.out.println("Row Key: " + rowKey + ", Location Info: " + locationInfo);
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
