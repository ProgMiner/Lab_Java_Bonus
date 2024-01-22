# server-architecture-tester

Приложение для тестирования производительности различных серверных архитектур на языке Java.

Поддерживаются три вида архитектур:

- `BLOCK` — обычная блокирующая архитектура с двумя потоками на клиента: для считывания
  и для записи

- `NONBLOCK` — неблокирующая архитектура с `Selector`, используется два потока на все соединения

- `ASYNC` — блокирующая асинхронная архитектура с использованием асинхронных каналов из Java NIO

Все архитектуры используют для вычисления задачи пул потоков одинакового фиксированного размера.

## Сборка

Для сборки требуется:

- Java 8
- Maven
- Компилятор Protobuf (`protoc`)

Команда сборки:

```bash
mvn package
```

Запускаемым артефактом сборки является файл
`./target/servertester-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Запуск

Для запуска используется команда:

```bash
java -jar ./target/servertester-1.0-SNAPSHOT-jar-with-dependencies.jar [args...]
```

Программа настраивается с помощью опционального файла `./config.properties` и опциональных
аргументов командной строки. Аргументы командной строки задаются в формате `--key=value` и
переопределяют значения, указанные в файле конфигурации.

Доступные параметры конфигурации:

- `arch` — архитектура сервера для тестирования (из списка выше, регистр не важен)

- `client_requests` — число запросов, посылаемых одним клиентом

- `array_size` — размер массива (переменная)

- `clients` — число клиентов (переменная)

- `request_delta` — ожидание клиента между получением ответа на запрос и отправкой следующего
  запроса в миллисекундах (переменная)

- `output_dir` — путь до директории, в которую будут записаны результаты теста

Те параметры, которые помечены как переменная, могут быть заданы в формате `begin..end, step`
(пробелы не важны). Всего должен быть ровно один переменный параметр.

Все параметры обязательные.

В результате работы создаётся директория, указанная в параметре `output_dir`, и файлы в ней:

- `config.txt` — конфигурация теста

- `data.csv` — CSV файл с результатами тестирования

Результаты тестирования представлены в виде таблицы со следующими столбцами:

- Переменный параметр (имя столбца зависит от выбранного переменного параметра)

- `computation time` — время выполнения вычислительной задачи на сервере (мс)

- `server request time` — время выполнения запроса на сервере от момента начала получения запроса
  до момента отправки ответа (мс)

- `client request time` — время ожидания обработки одного запроса на клиенте (мс)

Все метрики усредняются по клиентам и запросам.

## Скрипты запуска и построения графиков

В репозитории подготовлен файл скрипта запуска тестов по всем параметрам и соответствующий ему
файл конфигурации. Для запуска используется команда:

```bash
./run.sh
```

Детали внутри файла скрипта.

Также в репозитории подготовлен скрипт для построения графиков, согласованный со скриптом запуска.
Он вызывается командой:

```bash
./plot.py
```

Скрипт использует Python 3 и следующие зависимости:

- numpy

- pandas

- matplotlib


# Результаты тестирования

Тестирование проводилось для следующих значений параметров:

```
client_requests=20
array_size=50000
clients=50
request_delta=50
```

При этом, каждый из переменных параметров варьировался в отдельности по следующим правилам:

```
array_size:10000..100000,1000
clients:1..100,1
request_delta:0..100,10
```

Были получены следующие графики (при нажатии открываются в большом размере):

| Переменный параметр | Computation time | Server request time | Client request time |
|:--------------------|:----------------:|:-------------------:|:-------------------:|
| Array size | <img src="./plots/array_size/computation time.png"> | <img src="./plots/array_size/server request time.png"> | <img src="./plots/array_size/client request time.png"> |
| Clients | <img src="./plots/clients/computation time.png"> | <img src="./plots/clients/server request time.png"> | <img src="./plots/clients/client request time.png"> |
| Request delta | <img src="./plots/request_delta/computation time.png"> | <img src="./plots/request_delta/server request time.png"> | <img src="./plots/request_delta/client request time.png"> |

## Наблюдения и выводы

- Самый первый результат во всех измерениях является выбросом и может быть следствием прогрева JVM.

- При варьировании размера массива ожидаемо линейно увеличивается время выполнения задачи, но
  также явно видно, что почти сразу начинает нелинейно возрастать время обработки запроса на
  сервере, что, скорее всего, является следствием ожидания постановки задачи на выполнения в пул
  потоков.

- На больших размерах массива неблокирующая и асинхронная архитектуры начинают заметно проигрывать
  в скорости блокирующей архитектуре, потому что вычисление задачи становится нетривиально более
  длительным, чем ввод-вывод. Причём неблокирующая и асинхронная архитектуры почти не отличаются
  в скорости, потому что в них обеих используется константное число потоков на всех клиентов.

- При увеличении числа клиентов время вычисления задачи почти не меняется, но заметна ступенька
  в области 20 клиентов. Скорее всего в этот момент параллелизм компьютера достигает того предела,
  когда переключение контекста выполнения начинает существенно влиять на скорость вычислений.
  Интересно, что явная ступенька образуется только для блокирующей архитектуры. Для других двух
  наблюдается плавное, почти одинаковое, замедление, что опять может быть связано с константным
  числом потоков, использующихся для ввода-вывода (не очень понятно как именно).

- Также при большом числе клиентов (более 90) можно заметить резкое замедление неблокирующей
  архитектуры при обработке сообщений. Скорее всего дело в том, что из-за неблокирующего получения
  данных операционная система не успевает загружать их с достаточной скоростью, в следствие чего
  поток чтения постоянно считывает по маленьким частям сообщения от всех клиентов, затрачивая
  больше времени на чтение конкретного сообщения.

- В области 20 клиентов время обработки сообщения ведёт себя также, как и время вычисления задачи.

- При увеличении промежутка времени между запросами ожидаемо падает время обработки запросов,
  потому что сиюсекундная нагрузка на сервер падает.

- Время ожидания клиента отличается от времени обработки запроса на сервере на константу, потому
  что тесты проводятся на одном компьютере и время сетевой задержки определяется скоростью передачи
  данных в операционной системе.
