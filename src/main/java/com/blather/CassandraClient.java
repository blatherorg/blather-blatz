package com.blather;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("cassandraClient")
public class CassandraClient {
    private Cluster cluster;
    private Session session;

    @Autowired
    public CassandraClient(
            @Value("${cassandra.host}") final String cassandraHost,
            @Value("${cassandra.port}") final Integer cassandraPort
    ) {

        Boolean needRetry = true;
        while (needRetry) {
            try {
                Thread.sleep(1000);
                connect(cassandraHost, cassandraPort);
                needRetry = false;
            } catch (Exception e) {
                System.out.println(
                        "waiting for db host=" + cassandraHost + " port=" + cassandraPort);
            }
        }
        getSession();
        createSchema();
    }

    public void connect(String nodeName, Integer portNum) {
        cluster = Cluster.builder().addContactPoint(nodeName).withPort(portNum).build();
        Metadata metadata = cluster.getMetadata();
        System.out.println("Connected to cluster:" + metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            System.out.println("Datatacenter: " + host.getDatacenter()
                    + "; Host: " + host.getAddress() + "; Rack: "
                    + host.getRack());
        }
    }

    public void getSession() {
        session = cluster.connect();
    }

    public void closeSession() {
        session.close();
    }

    public void close() {
        cluster.close();
    }

    public void createSchema() {

        session.execute("CREATE KEYSPACE IF NOT EXISTS blather WITH replication "
                + "= {'class':'SimpleStrategy', 'replication_factor':3};");
        // For now we are putting all blats into a single partition, to allow
        // global sort by timestamp
        session.execute("CREATE TABLE IF NOT EXISTS blather.blats ("
                + "partition_key text,"
                + "id uuid," + "message text," + "creator text, " + "timestamp bigint," + "tags list<text>, "
                + "PRIMARY KEY ((partition_key), timestamp,  creator, id)"
                + ") WITH CLUSTERING ORDER BY (timestamp DESC);");
    }


    public String loadBlat(Blat blat) {

        PreparedStatement statement = session
                .prepare("INSERT INTO blather.blats "
                        + "(partition_key, id, message, creator, timestamp, tags) "
                        + "VALUES (?, ?, ?, ?, ?, ?);");

        BoundStatement boundStatement = new BoundStatement(statement);
        UUID blatId = UUID.randomUUID();
        Calendar calendar = Calendar.getInstance();
        // .todo. pupulate tags from actual blat
        List<String> tagIDs = Arrays.asList("tag_one", "tag_two", "tag_three");
        ResultSet res = session.execute(boundStatement.bind(
                "all_blatz",
                blatId,
                blat.getMessage(), blat.getCreator(),
                blat.getTimestamp(), tagIDs));

        System.err.println(res.toString());
        return blatId.toString();
    }

    public List<Blat> queryBlats() {
        Statement statement = QueryBuilder.select().all()
                .from("blather", "blats");
        ResultSet results = session.execute(statement);
        System.out
                .println(String
                        .format("%-50s\t%-30s\t%-20s\t%-20s\n%s", "id", "message", "creator",
                                "timestamp",
                                "------------------------------------------------------+-------------------------------+------------------------+-----------"));
        List<Blat> blats = new ArrayList<Blat>();
        for (Row row : results) {
            Blat blat = new Blat();
            blat.setBlatId(row.getUUID("id").toString());
            blat.setCreator(row.getString("creator"));
            blat.setMessage(row.getString("message"));
            blat.setTimestamp(row.getLong("timestamp"));
            blats.add(blat);
            System.out.println(String.format("%-50s\t%-30s\t%-20s\t%-20d",
                    row.getUUID("id"), row.getString("message"), row.getString("creator"),
                    row.getLong("timestamp")));
        }
        System.out.println();
        return blats;

    }

    public List<Blat> queryBlatsByHandle(String handle) {
        Select statement = QueryBuilder.select().all()
                .from("blather", "blats");
        statement.where(QueryBuilder.gt("timestamp", 0));
        statement.where(QueryBuilder.eq("creator", handle));
        statement.allowFiltering();
        ResultSet results = session.execute(statement);
        System.out
                .println(String
                        .format("%-50s\t%-30s\t%-20s\t%-20s\n%s", "id", "message", "creator",
                                "timestamp",
                                "------------------------------------------------------+-------------------------------+------------------------+-----------"));
        List<Blat> blats = new ArrayList<Blat>();
        for (Row row : results) {
            Blat blat = new Blat();
            blat.setBlatId(row.getUUID("id").toString());
            blat.setCreator(row.getString("creator"));
            blat.setMessage(row.getString("message"));
            blat.setTimestamp(row.getLong("timestamp"));
            blats.add(blat);
            System.out.println(String.format("%-50s\t%-30s\t%-20s\t%-20d",
                    row.getUUID("id"), row.getString("message"), row.getString("creator"),
                    row.getLong("timestamp")));
        }
        System.out.println();
        return blats;
    }
}
