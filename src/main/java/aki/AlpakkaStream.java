package aki;

import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

@Slf4j
public class AlpakkaStream {

    public static void main(String[] args) {
        var mirrorStream = new MirrorStream();
        mirrorStream.start();
        Runtime.getRuntime().addShutdownHook(new Thread(mirrorStream::stop));
    }

    private static class MirrorStream {
        private final Config config;
        private final ActorSystem system;
        private final ConsumerSettings<byte[], byte[]> consumerSettings;
        private final ProducerSettings<byte[], byte[]> producerSettings;
        private final CommitterSettings committerSettings;
        private Consumer.Control control;

        private MirrorStream() {
            this.config = ConfigFactory.load();

            consumerSettings = ConsumerSettings.create(
                    config.getConfig("akka.kafka.consumer"),
                    new ByteArrayDeserializer(),
                    new ByteArrayDeserializer()
            );

            producerSettings = ProducerSettings.create(
                    config.getConfig("akka.kafka.producer"),
                    new ByteArraySerializer(),
                    new ByteArraySerializer()
            );

            committerSettings = CommitterSettings.create(config.getConfig("akka.kafka.committer"));
            this.system = ActorSystem.create();
        }

        void start() {
            control = Consumer
                    .committableSource(consumerSettings, Subscriptions.topics(config.getString("topic.in")))
                    .map(this::toProducerRecord)
                    .to(Producer.committableSink(producerSettings, committerSettings))
                    .run(Materializer.matFromSystem(system));
        }

        private ProducerMessage.Envelope<byte[], byte[], ConsumerMessage.Committable> toProducerRecord(ConsumerMessage.CommittableMessage<byte[], byte[]> msg) {
            return ProducerMessage.single(
                    new ProducerRecord<>(
                            config.getString("topic.out"),
                            msg.record().key(),
                            msg.record().value()
                    ),
                    msg.committableOffset()
            );
        }

        void stop() throws RuntimeException {
            control.stop().whenComplete((done, throwable) -> {
                if (throwable != null) log.warn("encountered trouble when stopping the stream", throwable);
                log.info("stream {}", done);
            });
        }
    }

}
