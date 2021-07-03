import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
    String test = "I love Java in 666";
    String[] test2 = test.split("\\s+", 2);
    System.out.println(Arrays.toString(test2));
        }
    }
