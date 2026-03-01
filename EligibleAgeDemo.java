import java.util.Scanner;
class EligibleAgeDemo{
	static void eligibleAge(int n){
		if(n>=18)
		System.out.println("Eligible for vote");
		else
		System.out.println("Not Eligible for vote");
	}
public static void main(String args[]){
	Scanner sc= new Scanner(System.in);
	System.out.println("Enter the age:");
	int n=sc.nextInt();
	eligibleAge(n);
	}
}
		