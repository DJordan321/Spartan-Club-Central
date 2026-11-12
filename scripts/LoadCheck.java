/*
 * Spartan Club Central: quick performance check for NFR-04
 * ("search/filter results in under 2 seconds for 100 concurrent users").
 *
 * Needs only Java 17+ (no Maven, no extra tools). Start the app first, then run:
 *
 *     java scripts/LoadCheck.java
 *     java scripts/LoadCheck.java http://localhost:8080 100 5
 *                                  (address)            (users) (searches each)
 *
 * It sends the same mix of directory searches a class of students would,
 * all users at once, and prints the median, 95th-percentile and slowest
 * response times. PASS means 95% of searches finished in under 2 seconds
 * and none failed. Results depend on the laptop it runs on.
 *
 * Read-only: it never changes data.
 */
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadCheck {

    private static final String[] SEARCHES = {
        "/clubs?q=club&category=1", "/clubs?q=robotics", "/clubs?category=3",
        "/clubs?q=volunteer", "/clubs", "/clubs?q=chess&category=1", "/"
    };
    private static final long LIMIT_MS = 2000;

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0].replaceAll("/$", "") : "http://localhost:8080";
        int users = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        int perUser = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(Math.max(4, users / 4)))
                .build();

        System.out.printf("Checking %s with %d users x %d searches...%n", base, users, perUser);
        for (String path : SEARCHES) {                      // warm up (first requests are always slower)
            send(client, base + path);
        }

        List<Long> times = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(users);
        for (int u = 0; u < users; u++) {
            final int user = u;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perUser; i++) {
                        String path = SEARCHES[(user + i) % SEARCHES.length];
                        long t0 = System.nanoTime();
                        int status = send(client, base + path);
                        times.add((System.nanoTime() - t0) / 1_000_000);
                        if (status != 200) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
                return null;
            });
        }
        long wall = System.nanoTime();
        start.countDown();                                    // everyone starts at the same moment
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);
        wall = (System.nanoTime() - wall) / 1_000_000;

        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        if (sorted.isEmpty()) {
            System.out.println("FAIL: no responses. Is the app running at " + base + "?");
            System.exit(1);
        }
        long median = sorted.get(sorted.size() / 2);
        long p95 = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        long max = sorted.get(sorted.size() - 1);

        System.out.printf("Requests: %d   failed: %d   total time: %d ms%n", sorted.size(), failures.get(), wall);
        System.out.printf("Median: %d ms   95th percentile: %d ms   slowest: %d ms%n", median, p95, max);
        boolean pass = failures.get() == 0 && p95 < LIMIT_MS;
        System.out.println(pass ? "PASS: 95% of searches under 2 seconds (NFR-04)"
                                : "FAIL: too slow or some requests failed (NFR-04)");
        System.exit(pass ? 0 : 1);
    }

    private static int send(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "text/html").GET().build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
