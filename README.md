# Spring Cloud Data Flow - Bootch Task

This project is a demo from my I Code Java 2016 talk entitled "Stream is the new batch"

This project is a trivial Spring Cloud Data Flow implementation that accepts the file name to process as the POST request body 
to the [HTTP source](https://github.com/spring-cloud/spring-cloud-stream-app-starters/tree/master/http/spring-cloud-starter-stream-source-http) 
application, which then is mapped to a [`TaskLaunchRequest`](http://docs.spring.io/spring-cloud-dataflow/docs/1.0.0.RELEASE/reference/htmlsingle/#spring-cloud-dataflow-launch-tasks-from-stream)
that allows the `task-launcher-local` app to launch our `spring-bootch-task` app, which when launched, 
reads the passed in file name (as command line property), capitalises each line and prints the result to the log.

See the SCDF [reference documentation](http://docs.spring.io/spring-cloud-dataflow/docs/1.0.0.RELEASE/reference/htmlsingle) for more information

## Kafka

We will use the Kafka binder implementations of the apps, therefore we need a running Kafka broker.
The simplest way to stand up a Kafka instance is to use the a Docker image:

```
$ docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=`ipconfig getifaddr en0` --env ADVERTISED_PORT=9092 spotify/kafka
```

_Substitute `ipconfig getifaddr en0` where applicable_

## Spring Cloud Data Flow Local Server

The simplest way to run a SCDF server is using the Local variant, which will run apps locally.
Stand a local server up by first downloading the server and then running it:

```
$ wget http://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-server-local/1.0.0.RELEASE/spring-cloud-dataflow-server-local-1.0.0.RELEASE.jar
$ java -jar spring-cloud-dataflow-server-local-1.0.0.RELEASE.jar
```

## Spring Cloud Data Flow Shell

We will create and deploy our stream definitions using the SCDF Shell (you could also use the [dashboard](http://localhost:9393/dashboard))

```
$ wget http://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-shell/1.0.0.RELEASE/spring-cloud-dataflow-shell-1.0.0.RELEASE.jar 
$ java -jar spring-cloud-dataflow-shell-1.0.0.RELEASE.jar
```

The shell will connect to the SCDF server running at http://localhost:9393 by default:

```
dataflow:>dataflow config server
Successfully targeted http://localhost:9393/
```

## Registering OOTB apps

We will use the out-of-the-box `http` source and `task-launcher-local` sink apps in our stream definition.
Register these apps with the following:

```
dataflow:>app import --uri http://bit.ly/stream-applications-kafka-maven
Successfully registered applications: [source.tcp, sink.jdbc, source.http, sink.rabbit, source.rabbit, source.ftp, sink.gpfdist, processor.transform, source.sftp, processor.filter, source.file, sink.cassandra, processor.groovy-filter, sink.router, source.trigger, sink.hdfs-dataset, processor.splitter, source.load-generator, processor.tcp-client, sink.file, source.time, source.gemfire, source.twitterstream, sink.tcp, source.jdbc, sink.field-value-counter, sink.redis-pubsub, sink.hdfs, processor.bridge, processor.pmml, processor.httpclient, sink.ftp, source.s3, sink.log, sink.gemfire, sink.aggregate-counter, sink.throughput, source.triggertask, sink.s3, source.gemfire-cq, source.jms, source.tcp-client, processor.scriptable-transform, sink.counter, sink.websocket, source.mongodb, source.mail, processor.groovy-transform, source.syslog]

dataflow:>app register --name task-launcher-local --type sink --uri maven://org.springframework.cloud.stream.app:task-launcher-local-sink-kafka:jar:1.0.2.RELEASE
Successfully registered application 'sink:task-launcher-local'
```
## Registering bootch processor

Now we will register our custom bootch processor, which maps a `TaskLaunchRequest` which will allow the `task-launcher-local`
app to launch our bootch task.

```
dataflow:>app register --name bootch-processor --type processor --uri maven://i.code.java:spring-bootch-processor:1.0-SNAPSHOT
Successfully registered application 'processor:bootch-processor'
```

Notice the `maven://` coordinate, this means the SCDF server will resolve our app using Maven.
This means we need to install our apps to our local Maven repo so that the server can find it:

```
$ mvn install

...

[INFO] Reactor Summary:
[INFO] 
[INFO] spring-bootch-scdf-task ............................ SUCCESS [  0.241 s]
[INFO] spring-bootch-task ................................. SUCCESS [  1.825 s]
[INFO] spring-bootch-processor ............................ SUCCESS [  1.026 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Creating the stream definition

Next we create the stream definition:

```
dataflow:>stream create --name bootch --definition "http --port=8080 | bootch-processor | task-launcher-local"
Created new stream 'bootch'
```

Let's dissect this definition to understand what each app does:

* `http --port=8080` - Use the OOTB `http` source app running on port `8080` to receive a HTTP POST request body 
that represents the path of the file to pass into our bootch task
* `bootch-processor` - Push that path into our custom processor, that maps this information to a `TaskLaunchRequest`
* `task-launcher-local` - Use the `TaskLaunchRequest` received from `bootch-processor` and kick off our bootch task

This is what the `bootch-processor` mapping looks like:

```
@EnableBinding(Processor.class)
public class BootchProcessor {

    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public TaskLaunchRequest mapLaunchRequest(String path) {
        return new TaskLaunchRequest("maven://i.code.java:spring-bootch-task:1.0-SNAPSHOT",
                Stream.of(format("filename=%s", path)).collect(toList()),
                null,
                null);
    }
}
```

The `TaskLaunchRequest` contains the Maven coordinates of our bootch task, which will be used to launch the app,
as well as the command line argument `filename=...`, which will contain the path of the file to process with our 
Spring Batch job.

## Deploy and test the stream

Now we can deploy our stream:

```
dataflow:>stream deploy --name bootch
Deployed stream 'bootch'
```

you should see something similar to the following in the SCDF server log:

```
...  INFO 45810 --- [nio-9393-exec-3] o.s.c.d.spi.local.LocalAppDeployer       : deploying app bootch.task-launcher-local instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/bootch-1470906441225/bootch.task-launcher-local
...  INFO 45810 --- [nio-9393-exec-3] o.s.c.d.spi.local.LocalAppDeployer       : deploying app bootch.bootch-processor instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/bootch-1470906441571/bootch.bootch-processor
...  INFO 45810 --- [nio-9393-exec-3] o.s.c.d.spi.local.LocalAppDeployer       : deploying app bootch.http instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/bootch-1470906442089/bootch.http
```

all three apps in our stream are now deployed.
Note that our task is not deployed yet. Spring Cloud Task apps are launched adhoc and then terminate when done.

Tail the logs of the `bootch.task-launcher-local` instance so we can see when our task is launched:

```
$ tail -f /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/bootch-1470906441225/bootch.task-launcher-local/stdout_0.log

...

...  INFO 46530 --- [           main] o.s.c.s.b.k.KafkaMessageChannelBinder$7  : started inbound.bootch.bootch-processor.bootch
...  INFO 46530 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 0
...  INFO 46530 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 2147482647
...  INFO 46530 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 18467 (http)
...  INFO 46530 --- [           main] .k.TaskLauncherLocalSinkKafkaApplication : Started TaskLauncherLocalSinkKafkaApplication in 21.294 seconds (JVM running for 22.114)
```

Let's send a HTTP POST with the path to a file to process using the built in `http` command in the shell:

```
dataflow:>http post --data "/tmp/icodejava/input.txt" --target http://localhost:8080
> POST (text/plain;Charset=UTF-8) http://localhost:8080 /tmp/icodejava/input.txt
> 202 ACCEPTED
```

You'll notice the `bootch.task-launcher-local` logs now contain the path to the launched bootch task instance:

```
...  INFO 46530 --- [ kafka-binder-1] o.s.c.task.launcher.TaskLauncherSink     : Launching Task for the following resource TaskLaunchRequest{uri='maven://i.code.java:spring-bootch-task:1.0-SNAPSHOT', commandlineArguments=[filename=/tmp/icodejava/input.txt], environmentProperties={}, deploymentProperties={}}
...  INFO 46530 --- [ kafka-binder-1] o.s.c.d.spi.local.LocalTaskLauncher      : launching task Task-972575537
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-8036180456886772160/Task-972575537-1470906767360/Task-972575537
```

and if we tail that log, we'll see that our task has run our Spring Batch job successfully:

```
$ tail -f /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-8036180456886772160/Task-972575537-1470906767360/Task-972575537/stdout.log

...

...  INFO 46738 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=uppercase]] launched with the following parameters: [{run.id=1, filename=/tmp/icodejava/input.txt}]
...  INFO 46738 --- [           main] o.s.c.t.b.l.TaskBatchExecutionListener   : The job execution id 1 was run within the task execution 1
...  INFO 46738 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [capitalize]
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> I
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> CODE
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> JAVA
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> 2016
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> LEGACY
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> BATCH
...  INFO 46738 --- [           main] i.code.java.JobConfiguration             : Oh my word -> DEMO
...  INFO 46738 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=uppercase]] completed with the following parameters: [{run.id=1, filename=/tmp/icodejava/input.txt}] and the following status: [COMPLETED]
...  INFO 46738 --- [           main] s.c.a.AnnotationConfigApplicationContext : Closing org.springframework.context.annotation.AnnotationConfigApplicationContext@6d21714c: startup date [Thu Aug 11 11:12:48 SAST 2016]; root of context hierarchy
...  INFO 46738 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Stopping beans in phase 0
...  INFO 46738 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
...  INFO 46738 --- [           main] i.code.java.Application                  : Started Application in 9.646 seconds (JVM running for 10.154)
```

## Conclusion

Using the SCDF approach for launching tasks/jobs has the following benefits:

* Tasks/jobs only contain functionality related to the task/job. They do not need to include logic around how to trigger the job for instance.
This makes them lightweight and easily composable as they can be triggered from multiple sources
* Tasks only use resources when they are running



