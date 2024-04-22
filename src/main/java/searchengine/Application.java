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
        //TODO прописать присвоение статуса FAILED при остановке поиска
        //TODO прописать код, который будет отрезать корневой сайт в PageToDto
        //TODO переписать в классе SavePageAndSiteInDB сравнение < 100. Необходимо ,чтобы сравнивалось с исходником, а потом создавалась копия, но происходит задвоение в БД и ошибка PageToDto.java:38 при удалении элементов из эррэйлиста
    }
}
