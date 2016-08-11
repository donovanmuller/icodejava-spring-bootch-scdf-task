package i.code.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
@EnableBatchProcessing
public class JobConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JobConfiguration.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job(Step capitalize) throws Exception {
        return jobBuilderFactory.get("uppercase")
                .incrementer(new RunIdIncrementer())
                .start(capitalize)
                .build();
    }

    @Bean
    public Step capitalize(FlatFileItemReader<String> reader,
            ItemProcessor<String, String> itemProcessor,
            ItemWriter<String> writer) {
        return stepBuilderFactory.get("capitalize")
                .<String, String>chunk(1)
                    .reader(reader)
                    .processor(itemProcessor)
                    .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<String> reader(@Value("#{jobParameters[filename]}") String filename) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filename));
        reader.setLineMapper(new PassThroughLineMapper());
        return reader;
    }

    @Bean
    public ItemProcessor<String, String> capitaliseProcessor() {
        return word -> {

            // take it slow
            Thread.sleep(1000);

            return word.toUpperCase();
        };
    }

    @Bean
    public ItemWriter<String> writer() {
        return words -> words.forEach(word -> log.info("Oh my word -> {}", word));
    }
}
