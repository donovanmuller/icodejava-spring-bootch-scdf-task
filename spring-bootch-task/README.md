# Spring Bootch Task

This custom task is launched by the `task-launcher-local` app and
executes a Spring Batch job to process the `filename` passed in as a command line argument.

See the parent `../README.md` for context.

## Standalone test

To see an example of the output this task will produce, run:

```
$ cd spring-bootch-task
$ ./mvnw spring-boot:run -Drun.arguments="filename=/tmp/icodejava/input.txt"

...

...  INFO 47667 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=uppercase]] launched with the following parameters: [{run.id=1, filename=/tmp/icodejava/input.txt}]
...  INFO 47667 --- [           main] o.s.c.t.b.l.TaskBatchExecutionListener   : The job execution id 1 was run within the task execution 1
...  INFO 47667 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [capitalize]
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> I
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> CODE
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> JAVA
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> 2016
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> LEGACY
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> BATCH
...  INFO 47667 --- [           main] i.code.java.JobConfiguration             : Oh my word -> DEMO
...  INFO 47667 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=uppercase]] completed with the following parameters: [{run.id=1, filename=/tmp/icodejava/input.txt}] and the following status: [COMPLETED]
```

where `/tmp/icodejava/input.txt` is a file containing lines that should be capitalised.
