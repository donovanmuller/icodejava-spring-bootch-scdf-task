package i.code.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.messaging.handler.annotation.SendTo;

@EnableBinding(Processor.class)
public class BootchProcessor {

    private static final Logger log = LoggerFactory.getLogger(BootchProcessor.class);

    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public TaskLaunchRequest mapLaunchRequest(String path) {
        log.debug("Mapping launch request for path: {}", path);

        return new TaskLaunchRequest("maven://i.code.java:spring-bootch-task:1.0-SNAPSHOT",
                Stream.of(format("filename=%s", path)).collect(toList()),
                null,
                null);
    }
}
