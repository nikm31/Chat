import java.awt.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class QueueWork {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Random random = new Random();
        int[] randomNumbers = {4,1,6,8,5,0,2,9,3,7};
        System.out.println(Arrays.toString(randomNumbers));

        Queue<Integer> queue = new PriorityQueue<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });

        for (int i = 0; i < randomNumbers.length; i++) {
            queue.add(randomNumbers[i]);
            if (queue.size() > 5) {
                queue.remove();
            }
        }
        for (int i = 0; i < 5; i++) {
            Integer in = queue.poll();
            System.out.println("отсортировано " + in);
        }

    }
}

