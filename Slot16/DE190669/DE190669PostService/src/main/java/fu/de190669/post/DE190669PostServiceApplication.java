package fu.de190669.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190669PostServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DE190669PostServiceApplication.class, args);
    }
}
