## Alpakka Kafka Mirror Stream

Sometimes you would like to source data from one kafka cluster to another.
If you are a kafka administrator then you probably would use tools like 'Kafka MirrorMaker' or 'Replicator'.
Else you might wonder whether kafka streams is the right tool for the job.
However, currently, this functionality is not built in [yet](https://kafka.apache.org/27/documentation/streams/developer-guide/config-streams.html#bootstrap-servers).

Therefore, I am presenting this small topic mirroring tool for kafka users with constrained privileges. Just like the MirrorMaker, it is '...little more than a Kafka Consumer and Producer hooked together... [MirrorMaker](https://docs.confluent.io/platform/current/multi-dc-deployments/replicator/migrate-replicator.html)'.

It leverages the akka streams + akka alpakka libraries to minimize the code needed. *At-least-once-delivery* guarantee is implemented by making use of the 'Committable Source and Sink' abstractions provided by the Alpakka library.

Moreover, everything can be configured easily with Hocon.
Either via .conf file or as CONFIG_FORCE_* variables in a container. The minimal configuration needed is presented in ./src/main/resources/application.conf

To add and override configs with external configurations run:

    ./gradlew jar
    java -jar -Dconfig.file=./path/to/config.conf ./build/libs/alpakka-kafka-mirror-1.0.0.jar


Or build the image for your local image repo by running

    ./gradlew jibDockerBuild

and override configs when running the containerized application, configure env variables as follows.
    
    ...
    CONFIG_FORCE_topic_in=some-other-topic
    ...

#### Example

prerequisite: kafkacat 

In the project root run

    docker-compose up -d

Produce some messages to the first 'cluster'

    echo -e "Hello\nworld" | kafkacat -b localhost:9092 -P -t in 

Check what ends up on the second 'cluster'
        
    kafkacat -b localhost:9093 -C -t out

#### Limitations

In its current state tool is only moving the key-value records from topic A to topic B, properties like number of partitions, registered schemas on the confluent platform etc. are left behind... 



