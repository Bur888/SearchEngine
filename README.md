# searchengine

## Имя
Search Engine

## Описание
Этот проект представляет собой поисковую систему, которая осуществляет поиск страниц по сайтам, индексирует страницы и ищет леммы на каждой странице. 
Приложение может индексировать как весь сайт, так и отдельные страницы сайта. Информация, в случае, если указанная страница имеется в базе данных, удаляется и перезаписывается заново.
Поиск лемм может осуществляться как по одному сайту, так и по всем сайтам, которые указаны в конфигурационном файле.
При запуске индексации сайтов поиск страниц осуществляется в многопоточном режиме с помощью `RecursiveTask`. Параллельно с поиском страниц работает еще один поток, который каждые 3 секунды проверяет выполнение условия: количество найденных на сайтах страниц должно быть более 100. В случае выполнения этого условия найденные страницы сохраняются в базу данных.
Поиск лемм и индексация страниц запускается сразу после сохранения страниц в базу данных. При этом поиск страниц продолжается параллельно с поиском лемм и индуксацией страниц.
В целях ускорения работы приложения взаимодействие с базой данных организовано в основном с помощью  составных запросов.

## Установка

### Требования
- Java 8 или выше
- Maven
- MySQL или другая поддерживаемая база данных

### Шаги установки
1. **Клонирование репозитория**

   ```sh
   git clone https://github.com/your-username/search-engine.git
   cd search-engine
   
2. **Настройка базы данных**
      Создайте базу данных и настройте подключение в файле application.properties:
        spring.datasource.url=jdbc:mysql://localhost:3306/search_engine
      spring.datasource.username=your_username
      spring.datasource.password=your_password
      spring.jpa.hibernate.ddl-auto=update
      
3. Запуск приложения:
mvn spring-boot:run 

## Использование
### API Endpoints
GET/api/statistics    
curl -X GET "http://localhost:8087/"

GET/api/search
Поиск страниц по заданным леммам.
curl -X GET "http://localhost:8080/api/search?query=example&offset=0&limit=10"

GET/api/startIndexing
Запуск индексации сайтов.
curl -X GET "http://localhost:8080/api/startIndexing"

GET/api/stopIndexing
Остановка индексации сайтов.
curl -X GET "http://localhost:8080/api/stopIndexing"

POST/api/indexPage
Получение страниц по их идентификаторам.
curl -X GET "http://localhost:8080/api/indexPage" -H "Content-Type: application/json" -d 
'{
"url": "http://example.com/page"
}'

## Структура проекта
controllers
    ApiController.java: Контроллер для обработки API запросов.
    DefaultController.java: Контроллер для отображения стартовой страницы приложения.

services
    IndexingService.java: Сервис для поиска страниц на сайтах и индексации найденных страниц
    PageCRUDService.java: Сервис для работы с данными страниц.
    LemmaService.java: Сервис для работы с леммами.
    SiteCRUDService.java: Сервис для работы с данными сайтов.
    IndexCRUDService.java: Сервис для работы с проиндексированными леммами и страницами.
    SearchService.java: Сервис для поиска лемм на проиндексированных страницах.
    StatisticsServiceImpl.java: Сервис для обработки и выдачи статистики по сайтам.

repository
    PageRepository.java: Репозиторий для работы с данными страниц.
    LemmaRepository.java: Репозиторий для работы с данными лемм.
    IndexRepository.java: Репозиторий для работы с данными индексов.
    SiteRepository.java: Репозиторий для работы с данными сайтов.

models
    entityes:
        PageEntity.java: Сущность для представления страницы.
        LemmaEntity.java: Сущность для представления леммы.
        IndexEntity.java: Сущность для представления индекса.
        SiteEntity.java: Сущность для представления сайта.
    findAndSaveLemmaAndIndex
        FindAndSaveLemmaAndIndex.java: Класс для поиска и сохранения лемм и индексации страниц
        LemmaFinder.java: Класс для поиска лемм.
        MultiLuceneMorphology.java: Класс для создания русско и англоязычного морфологического разбора.
    searchLinks
        ConnectionWeb.java: Класс для получения HTML кода страницы.
        Link.java: Класс для работы с сылками, найденными на страницах сайта.
        ParseWeb.java: Класс для парсинга Web-страниц.
        ParseWebRecursive.java: Класс для запуска рекурсивных задач по поиску ссылок.
        ThreadForSavePageAndSiteInDB.java: Клас для сохранения сайтов и страниц сайтов в базу данных.
        ThreadForSearchLinks.java: Класс для организации рекурсивного поиска ссылок на страницах.
    IndexPage.java: Класс для индексации конкретной страницы.
    SearchWords.java: Класс для поиска в базе данных слов, указанных в запросе.
    StartIndexingSites.java: Класс для запуска индексации сайтов.

config
    Site.java: Класс, предоставляющий данные о сайте.
    SitesList.java: Класс с перечнем сайтов, по которым требуется индексация.
    application.yaml: Конфигурационный файл для настройки приложения.

## Контакты
Если у вас есть вопросы или предложения, пожалуйста, свяжитесь со мной:
Email: Zhuchkov888@yandex.ru
