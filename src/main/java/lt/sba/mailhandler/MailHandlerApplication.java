package lt.sba.mailhandler;

import lt.sba.mailhandler.config.GraphProperties;
import lt.sba.mailhandler.config.MailHandlerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MailHandlerApplication {

    public static void main(String[] args) {SpringApplication.run(MailHandlerApplication.class, args);}

}
