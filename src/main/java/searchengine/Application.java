package searchengine;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
        //TODO прописать присвоение статуса FAILED про остановке поиска
        //TODO завершить обрабоку ошибки в класск ПарсВеб и написания метода получения сущности в класск САЙТКРУД

    }
}
