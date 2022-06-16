import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class JsonParser {
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("k:mm");

    public static void main(String[] args) throws IOException {
        File file = new File("tickets.json");
        List<Ticket> tickets = parse(file);
        List<Long> durations = getDurationsInSeconds(tickets);
        Duration avgDuration = avgTime(durations);
        System.out.printf("Среднее время между Владивостоком и Тель-Авивом: часов - %d, минут - %d%n", avgDuration.toHours(), avgDuration.toMinutesPart());
        Duration durationInPercentile = inPercentile(durations, 90);
        System.out.printf("Время в 90 персентиле: часов - %d, минут - %d", durationInPercentile.toHours(), durationInPercentile.toMinutesPart());
    }

    private static List<Ticket> parse(File file) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        try (Reader reader = new FileReader(file)) {
            return (gson.fromJson(reader, Tickets.class)).getTickets();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Duration avgTime(List<Long> durations) {
        int sum = durations.stream().mapToInt(Long::intValue).sum();
        return Duration.ofSeconds(sum / durations.size());
    }

    private static Duration inPercentile(List<Long> durations, double percentile) {
        List<Long> sortedDurations = durations.stream().sorted().toList();
        double index = percentile / 100 * (durations.size() + 1);
        long durationInPercentile;
        if (isInteger(index) || index >= sortedDurations.size() - 1) {
            durationInPercentile = sortedDurations.get((int) index);
        } else {
            int firstIndex = (int) Math.floor(index);
            int secondIndex = (int) Math.ceil(index);
            durationInPercentile = (sortedDurations.get(firstIndex) + sortedDurations.get(secondIndex)) / 2;
        }
        return Duration.ofSeconds(durationInPercentile);
    }

    private static boolean isInteger(double index) {
        int a = (int) index;
        return a == index;
    }

    private static List<Long> getDurationsInSeconds(List<Ticket> tickets) {
        List<Long> durations = new ArrayList<>();
        for (Ticket ticket : tickets) {
            LocalDateTime departureDateTime = parseDateTime(ticket.getDepartureDate(), ticket.getDepartureTime());
            LocalDateTime arrivalDateTime = parseDateTime(ticket.getArrivalDate(), ticket.getArrivalTime());
            long seconds = ChronoUnit.SECONDS.between(departureDateTime, arrivalDateTime);
            durations.add(seconds);
        }
        return durations;
    }

    public static LocalDateTime parseDateTime(String date, String time) {
        LocalDate localDate = LocalDate.parse(date, dateFormatter);
        LocalTime localTime = LocalTime.parse(time, timeFormatter);
        return LocalDateTime.of(localDate, localTime);
    }
}
