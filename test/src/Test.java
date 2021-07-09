import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Test {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<String> future = executorService.submit(() -> {
            System.out.println("Асинхронный вызов");
            int x = 10 / 0;
            return "Результат из потока";
        });
        try {
            System.out.println("future.get() = " + future.get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        executorService.shutdown();

    }
}
