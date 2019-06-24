package com.blather;
import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class BlatService {

    @Autowired
    private CassandraClient cassandraClient;

    BlatService() {
    }

    List<Blat> getBlats() {
        return cassandraClient.queryBlats();
    }

    List<Blat> getBlatsByHandle(String handle) {
        return cassandraClient.queryBlatsByHandle(handle);
    }

    public void loadBlat(Blat blat) {

        // pull user out of security context
        String username = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
            username = principal.toString();
        }
        System.out.println("djs ... username is " + username);
        // Read more: https://javarevisited.blogspot.com/2018/02/what-is-securitycontext-and-SecurityContextHolder-Spring-security.html#ixzz5bUAl7b97
        blat.setCreator(username);

        Long currDate = Instant.now().getEpochSecond();
        System.out.println("Setting date to " + currDate.toString());
        blat.setTimestamp(currDate);
        String id = cassandraClient.loadBlat(blat);

        // use ID from cassandra insert
        blat.setBlatId(id);
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(calendar.getTime().getTime());

        // Forward message to kafka topic with id and currentTimestamp
        Producer<Long, String> producer = KafkaProducerCreator.createProducer();

        try {
            ObjectMapper mapperObj = new ObjectMapper();
            String jsonStr = null;
            try {
                jsonStr = mapperObj.writeValueAsString(blat);
                System.out.println(jsonStr);
                //ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(IKafkaConstants.TOPIC_NAME,
                //        "This is record : " + blat.getMessage());
                ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(IKafkaConstants.TOPIC_NAME,
                        jsonStr);
                RecordMetadata metadata = producer.send(record).get();
                System.out.println("Record sent with key " + blat.getMessage() + " to partition " + metadata.partition()
                        + " with offset " + metadata.offset());
            } catch (IOException e) {
                System.out.println("exeption serializing json : " + e.toString());
            }
        }
        catch (ExecutionException e) {
            System.out.println("Error in sending record");
            System.out.println(e);
        }
        catch (InterruptedException e) {
            System.out.println("Error in sending record");
            System.out.println(e);
        }

    }
}
