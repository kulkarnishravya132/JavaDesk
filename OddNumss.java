import java.util.Scanner;

class OddNumss {

    static void sumOfOddNumbers(int n) {
        int count = (n + 1) / 2;   // number of odd numbers
        int sum = count * count;  // k^2 formula
        System.out.println("Sum of odd numbers from 1 to " + n + " is: " + sum);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the value of n");
        int n = sc.nextInt();

        sumOfOddNumbers(n);
    }
}
