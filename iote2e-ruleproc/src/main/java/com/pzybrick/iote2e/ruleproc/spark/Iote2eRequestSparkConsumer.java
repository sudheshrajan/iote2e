package com.pzybrick.iote2e.ruleproc.spark;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.apache.spark.streaming.kafka.OffsetRange;

import kafka.serializer.DefaultDecoder;
import kafka.serializer.StringDecoder;

public class Iote2eRequestSparkConsumer {
    private static final Log log = LogFactory.getLog(Iote2eRequestSparkConsumer.class);
	
	
    public static void main(String[] args) {
    	Iote2eRequestSparkConsumer sparkAvroConsumer = new Iote2eRequestSparkConsumer();
    	sparkAvroConsumer.process(args);
    }
    	
    public void process(String[] args) {
		String zooKeeper = args[0];
		String groupId = args[1];
		String topic = args[2];
		int numThreads = Integer.parseInt(args[3]);
    	
        SparkConf conf = new SparkConf()
                .setAppName("kafka-sandbox")
                .setMaster("local[*]");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaStreamingContext ssc = new JavaStreamingContext(sc, new Duration(2000));

        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(numThreads));
        Map<String, String> kafkaParams = new HashMap<>();
        
        //kafkaParams.put("metadata.broker.list", "localhost:9092");
        kafkaParams.put("zookeeper.connect", zooKeeper);
        kafkaParams.put("group.id", groupId);
        kafkaParams.put("zookeeper.session.timeout.ms", "400");
        kafkaParams.put("zookeeper.sync.time.ms", "200");
        kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        JavaPairReceiverInputDStream<String, byte[]> directKafkaStream = KafkaUtils.createStream(ssc,
                String.class, byte[].class, StringDecoder.class, DefaultDecoder.class, kafkaParams, topicCountMap, StorageLevel.MEMORY_ONLY());
        
        final AtomicReference<OffsetRange[]> offsetRanges = new AtomicReference<>();
    	
//        directKafkaStream.transformToPair(
//          new Function<JavaPairRDD<String, byte[]>, JavaPairRDD<String, byte[]>>() {
//            @Override
//            public JavaPairRDD<String, byte[]> call(JavaPairRDD<String, byte[]> rdd) throws Exception {
//            	System.out.println(">>>> before offsetranges: " + rdd._2 );
////              OffsetRange[] offsets = ((HasOffsetRanges) rdd.rdd()).offsetRanges();
////              offsetRanges.set(offsets);
//              return rdd;
//            }
//          }
//        ).foreachRDD(
//          new Function<JavaPairRDD<String, byte[]>, Void>() {
//            @Override
//            public Void call(JavaPairRDD<String, byte[]> rdd) throws IOException {
//              for (OffsetRange o : offsetRanges.get()) {
//                System.out.println(
//                  o.topic() + " " + o.partition() + " " + o.fromOffset() + " " + o.untilOffset()
//                );
//              }
//              return null;
//            }
//          }
//        );

        directKafkaStream.foreachRDD(rdd -> {
            rdd.foreach(avroRecord -> {
        		BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(avroRecord._2, null);
				Wave wave = datumReaderWave.read(null, binaryDecoder);

            	System.out.println("Key: " + avroRecord._1 + ", Wave=" + wave.toString()) ;
            	waves.add(wave);
//                Schema.Parser parser = new Schema.Parser();
//                Schema schema = parser.parse(SimpleAvroProducer.USER_SCHEMA);
//                Injection<GenericRecord, byte[]> recordInjection = GenericAvroCodecs.toBinary(schema);
//                GenericRecord record = recordInjection.invert(avroRecord._2).get();
//
//                System.out.println("str1= " + record.get("str1")
//                        + ", str2= " + record.get("str2")
//                        + ", int1=" + record.get("int1"));
            });
            System.out.println("This: " + this );
            System.out.println("Number of waves: " + waves.size());
            waves.clear();
        });
        
        Iote2eRequestSparkProcessor streamProcessor = new Iote2eRequestSparkProcessor();
        directKafkaStream.map(String::new)
                    .map(streamProcessor::convertFromJson)
                    .foreachRDD(streamProcessor::processIote2eRequestRDD);

        ssc.start();
        ssc.awaitTermination();
    }
}