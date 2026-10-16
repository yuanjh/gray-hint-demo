package demo.hint.servicea;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.cloud.openfeign.EnableFeignClients
public class Application { public static void main(String[] args) { SpringApplication.run(Application.class,args); } }
