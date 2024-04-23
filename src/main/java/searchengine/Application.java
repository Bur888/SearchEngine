package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
        //TODO почему не работает стопиндексинг
        //TODO прописать код, который будет отрезать корневой сайт в PageToDto
    }
}
