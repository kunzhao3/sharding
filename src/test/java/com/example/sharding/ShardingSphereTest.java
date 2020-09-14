package com.example.sharding;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.config.sharding.KeyGeneratorConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.InlineShardingStrategyConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ShardingSphereTest {

    public static void main(String[] args) throws SQLException {
        // 配置真实数据源
        Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>(2);//为两个数据库的datasource

        // 配置第一个数据源
        HikariDataSource dataSource0 = new HikariDataSource();
        dataSource0.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource0.setJdbcUrl("jdbc:mysql://192.168.179.132:3306/shop_ds_0?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8");
        dataSource0.setUsername("root");
        dataSource0.setPassword("123456");
        dataSourceMap.put("ds0", dataSource0);


        // 配置第二个数据源
        HikariDataSource dataSource1 = new HikariDataSource();
        dataSource1.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource1.setJdbcUrl("jdbc:mysql://192.168.179.132:3306/shop_ds_1?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8");
        dataSource1.setUsername("root");
        dataSource1.setPassword("123456");
        dataSourceMap.put("ds1", dataSource1);

        /****************分片表组装************/
        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTableRuleConfigs().add(getOrderTableRuleConfiguration());
        //shardingRuleConfig.getTableRuleConfigs().add(getOrderItemTableRuleConfiguration());
        shardingRuleConfig.getBindingTableGroups().add("t_order");

        /****************分片具体规则************/
        //datanode为数据分片最小单元
        // 配置分库策略
        //我们采用数据库的分片是用user_id user_id %2 主要定位数据库库
        shardingRuleConfig.setDefaultDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration("user_id", "ds$->{user_id % 2}"));
        //sharding-cloumn 分片字段

        // 配置分表策略 对表分片是采用我们order_id
        shardingRuleConfig.setDefaultTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new PreciseShardingAlgorithm<Long>() {

            @Override
            public String doSharding(Collection<String> collection, final PreciseShardingValue<Long> preciseShardingValue) {
                for (String each : collection) {
                    if (each.endsWith(preciseShardingValue.getValue() % 2 + "")) {//这句话会产生什么？只会产生偶数的订单
                        return each;
                    }
                }
                throw new UnsupportedOperationException();
            }
        }));

        // 获取数据源对象
        Properties properties = new Properties();
        properties.setProperty("sql.show", "true");
        DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, properties);

        //-------------测试部分-----------------//
        ShardingSphereTest test = new ShardingSphereTest();
        //test.drop(dataSource);
        //test.create(dataSource);
        //插入数据
        //test.insertData(dataSource);
        test.selectRange(dataSource);
    }

    /**
     * t_order分表规则配置,主键采用雪花算法
     */
    private static TableRuleConfiguration getOrderTableRuleConfiguration() {
        TableRuleConfiguration orderTableRuleConfig = new TableRuleConfiguration("t_order",
                "ds${0..1}.t_order_${[0, 1]}");
        orderTableRuleConfig.setKeyGeneratorConfig(new KeyGeneratorConfiguration("SNOWFLAKE"
                , "order_id",getProps()));
        return orderTableRuleConfig;
    }

    /**
     * t_order_item分表规则配置,主键采用雪花算法
     */
    private static TableRuleConfiguration getOrderItemTableRuleConfiguration() {
        TableRuleConfiguration orderItemTableRuleConfig = new TableRuleConfiguration("t_order_item",
                "ds${0..1}.t_order_item_${[0, 1]}");
        orderItemTableRuleConfig.setKeyGeneratorConfig(new KeyGeneratorConfiguration("SNOWFLAKE"
                , "order_id",getProps()));
        return orderItemTableRuleConfig;
    }

    //雪花算法，需要有机器序号，手动配置序号
    private static Properties getProps(){
        Properties props = new Properties();
        props.setProperty("worker.id", "123");
        return props;
    }

    /***
     * 用户为中心10 和11 偶数和奇数
     * @param dataSource
     * @throws SQLException
     */
    public void insertData(DataSource dataSource) throws SQLException {
        for (int i = 1; i < 10; i++) {
            long orderId = executeAndGetGeneratedKey(dataSource, "INSERT INTO t_order (user_id, address_id, status) VALUES (10,10, 'INIT')");
            execute(dataSource, String.format("INSERT INTO t_order_item (order_id, user_id, status) VALUES (%d, 10, 'INIT')", orderId));
            orderId = executeAndGetGeneratedKey(dataSource, "INSERT INTO t_order (user_id, address_id, status) VALUES (11,11, 'INIT')");
            execute(dataSource, String.format("INSERT INTO t_order_item (order_id, user_id, status) VALUES (%d, 11, 'INIT')", orderId));
        }
    }

    public void selectRange(DataSource dataSource){
        try {
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            //ResultSet result = statement.executeQuery(String.format("SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE o.order_id in (%d, %d)",405466009580318721l,405466010490482688l));
            ResultSet result = statement.executeQuery(String.format("SELECT * FROM t_order o  WHERE o.order_id in (%d, %d)",405466009580318721l,405466010490482688l));
            while(result.next()){
                log.info("result:"+result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void execute(final DataSource dataSource, final String sql) throws SQLException {
        try (
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }

    private long executeAndGetGeneratedKey(final DataSource dataSource, final String sql) throws SQLException {
        long result = -1;
        try (
                Connection conn = dataSource.getConnection();
                Statement statement = conn.createStatement()) {
                statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    result = resultSet.getLong(1);
                }
        }
        return result;
    }



    /**-----------------------------表初始化--------------------------------*/
    public void drop(DataSource dataSource) throws SQLException {
        execute(dataSource, "DROP TABLE IF EXISTS t_order");
        execute(dataSource, "DROP TABLE IF EXISTS t_order_item;");
    }

    public void create(DataSource dataSource) throws SQLException {
        execute(dataSource, "CREATE TABLE IF NOT EXISTS t_order (order_id BIGINT AUTO_INCREMENT, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_id ))");
        execute(dataSource, " CREATE TABLE IF NOT EXISTS t_order_item (order_item_id BIGINT AUTO_INCREMENT, order_id BIGINT, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_item_id));");
    }

}