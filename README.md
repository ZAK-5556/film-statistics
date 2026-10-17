# Film Statistics — Блок 1: Java Core

Консольна програма, яка парсить перелік JSON-файлів з фільмами та формує статистику
(загальну кількість) у розрізі одного з атрибутів. Результат записується у XML-файл
`statistics_by_{attribute}.xml`, відсортований за кількістю від більшого до меншого.

Без СКБД і Spring. Java 21, Maven. JSON читається через Jackson Streaming API, тести — JUnit 5.

## Збірка та запуск

```bash
git clone <repo>
cd film-statistics
mvn clean package            # збірка + unit-тести → target/film-statistics.jar
java -jar target/film-statistics.jar data genres
```

## Предметна область

| Сутність | Роль | Атрибути |
|----------|------|----------|
| **Film** (Фільм) | основна | `title` — назва; `director` — режисер (зв'язок багато-до-одного з `Director`); `year` — рік виходу; `country` — країна виробництва; `genres` — **багатозначний текстовий атрибут**, жанри через кому |
| **Director** (Режисер) | другорядна | `name` — ім'я |

Багато фільмів належать одному режисеру — зв'язок *багато-до-одного*.

### Атрибути, за якими формується статистика

| Атрибут | Тип | Приклад значення |
|---------|-----|------------------|
| `director` | текст | `"Christopher Nolan"` |
| `year` | число | `2010` |
| `country` | текст | `"South Korea"` |
| `genres` | текст, кілька значень через кому (також підтримується JSON-масив рядків) | `"Sci-Fi, Thriller, Action"` |

Для `genres` один фільм додає +1 до кожного свого жанру.

## Параметри запуску

```bash
java -jar target/film-statistics.jar <папка_з_json> <атрибут> [--threads N] [--out DIR]
```

Параметри:

| Параметр | Опис |
|----------|------|
| `<папка_з_json>` | папка, у якій лежать `*.json` файли (інші файли ігноруються) |
| `<атрибут>` | `director`, `year`, `country` або `genres` |
| `--threads N` | розмір пулу потоків для парсингу (за замовчуванням — кількість ядер CPU) |
| `--out DIR` | куди покласти результат (за замовчуванням — у вхідну папку) |

Приклад:

```bash
java -jar target/film-statistics.jar data genres --threads 4
```

Вивід у консоль:

```
Statistics by 'genres' (4 threads, 35 ms):
  Drama                                    3
  Sci-Fi                                   3
  Thriller                                 3
  Action                                   1
  ...
Result written to /.../data/statistics_by_genres.xml
```

Тести окремо: `mvn test`.

## Приклади файлів

### Вхідний файл `data/films_1.json`

```json
[
  {
    "title": "Inception",
    "director": "Christopher Nolan",
    "year": 2010,
    "country": "USA",
    "genres": "Sci-Fi, Thriller, Action"
  },
  {
    "title": "Interstellar",
    "director": "Christopher Nolan",
    "year": 2014,
    "country": "USA",
    "genres": "Sci-Fi, Drama"
  },
  {
    "title": "Parasite",
    "director": "Bong Joon-ho",
    "year": 2019,
    "country": "South Korea",
    "genres": "Thriller, Drama, Comedy"
  }
]
```

Другий файл `data/films_2.json` містить ще три фільми (Dune, Spirited Away, Memories of Murder).

### Вихідний файл `statistics_by_genres.xml` (для обох файлів з `data/`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<statistics>
  <item>
    <value>Drama</value>
    <count>3</count>
  </item>
  <item>
    <value>Sci-Fi</value>
    <count>3</count>
  </item>
  <item>
    <value>Thriller</value>
    <count>3</count>
  </item>
  <item>
    <value>Action</value>
    <count>1</count>
  </item>
  ...
</statistics>
```

### Вихідний файл `statistics_by_director.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<statistics>
  <item>
    <value>Bong Joon-ho</value>
    <count>2</count>
  </item>
  <item>
    <value>Christopher Nolan</value>
    <count>2</count>
  </item>
  <item>
    <value>Denis Villeneuve</value>
    <count>1</count>
  </item>
  <item>
    <value>Hayao Miyazaki</value>
    <count>1</count>
  </item>
</statistics>
```

Сортування: за кількістю за спаданням, при рівній кількості — за значенням за алфавітом
(щоб результат був детермінованим).

## Структура проекту

```
com.example.filmstats
├── model          Film, Director                      — сутності
├── parser         FilmFileParser                      — потокове читання файлу (Jackson JsonParser), по одному об'єкту
│                  FilmMapper                          — JsonNode → Film, валідація
├── statistics     Attribute                           — перелік атрибутів + логіка витягування значень
│                  StatisticsCollector                 — потокобезпечний підрахунок (ConcurrentHashMap + LongAdder)
│                  Statistics, StatisticsEntry         — відсортований результат
├── service        StatisticsService                   — пошук файлів, пул потоків, об'єднання результатів
├── xml            XmlStatisticsWriter                 — запис statistics_by_{attribute}.xml (javax.xml.stream)
├── cli            Main, CommandLineArgs               — консольний інтерфейс
└── benchmark      BenchmarkRunner, TestDataGenerator  — генерація великих файлів і заміри швидкодії
```

Тести (`src/test`, структура given-when-then): `FilmMapperTest`, `FilmFileParserTest`,
`StatisticsCollectorTest` (включно з перевіркою потокобезпечності), `XmlStatisticsWriterTest`,
`StatisticsServiceTest` (інтеграційний: кілька файлів, однаковий результат на 1 і 8 потоках,
помилка парсингу прокидається з робочого потоку).

## Як обробляються великі файли

`FilmFileParser` використовує потоковий `JsonParser` з Jackson: файл читається токен за
токеном, і в пам'яті формується лише **один** JSON-об'єкт за раз (`ObjectMapper.readTree(parser)`
читає рівно одне піддерево). Щойно об'єкт дочитаний, він перетворюється на `Film`, передається
у `StatisticsCollector` і стає доступним для збирання сміття. Файл цілком у пам'ять ніколи
не завантажується: обсяг пам'яті не залежить від розміру файлу, а лише від кількості
унікальних значень атрибута.

Перевірка: бенчмарк на 284 МБ даних (16 файлів × 150 000 фільмів) успішно виконано
з обмеженням купи `-Xmx256m`, тобто з купою меншою за обсяг вхідних даних.

## Багатопотоковість

`StatisticsService` створює `Executors.newFixedThreadPool(threads)` і для **кожного файлу**
подає окрему задачу. Усі потоки пишуть в один `StatisticsCollector`, де підрахунок зроблено
на `ConcurrentHashMap<String, LongAdder>` — без глобальних блокувань, `LongAdder` оптимізований
саме під конкурентні інкременти. Після завершення всіх `Future` результат знімається та сортується.

## Експеримент: кількість потоків

Запуск: `java -cp target/classes com.example.filmstats.benchmark.BenchmarkRunner [files] [filmsPerFile] [runs]`.
Генеруються синтетичні файли, виконується прогрів JIT, далі для 1/2/4/8 потоків
робиться по `runs` замірів статистики за `genres`.

### Результати на машині з 1 ядром CPU (16 файлів × 150 000 фільмів, 284 МБ, 3 прогони)

| Threads | Best, ms | Avg, ms | Speedup vs 1 thread |
|---------|----------|---------|---------------------|
|       1 |     1419 |    1580 |               1.00x |
|       2 |     1403 |    1501 |               1.05x |
|       4 |     1533 |    1608 |               0.98x |
|       8 |     1503 |    1544 |               1.02x |

Висновок для цього середовища: задача CPU-bound (розбір символів, виділення об'єктів),
тому на одному ядрі додаткові потоки нічого не дають — вони лише чергуються на тому самому
ядрі, а невеликі відхилення (±5 %) — у межах похибки вимірювання. Витрат на синхронізацію
практично не видно, бо `LongAdder` масштабується без блокувань.

### Очікувана поведінка на багатоядерній машині

Оскільки файли незалежні, а збирання результатів не має блокувань, на N фізичних ядрах
очікується прискорення, близьке до N, поки кількість потоків ≤ кількості ядер і файлів
(16 файлів дозволяють повністю завантажити 8 потоків). Далі приріст впирається у
кількість ядер та пропускну здатність диска; при 8 потоках на 4-ядерній машині приріст
відносно 4 потоків буде незначним або від'ємним через перемикання контексту.

> Щоб отримати цифри для своєї машини, запустіть бенчмарк і замініть таблицю вище:
> `java -cp target/classes com.example.filmstats.benchmark.BenchmarkRunner 16 150000 3`

## Обробка помилок

- Невалідний JSON або запис без `title` → `IOException` з назвою файлу та позицією помилки, код виходу 1.
- Непідтримуваний атрибут або неправильні аргументи → підказка з usage, код виходу 2.
- Порожня папка / не папка → повідомлення про помилку, код виходу 1.
- Помилка в будь-якому робочому потоці зупиняє пул (`shutdownNow`) і прокидається наверх.
