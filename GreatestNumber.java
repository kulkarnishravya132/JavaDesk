import java.util.Scanner;
class GreatestNumber{
	
	static void GreatNum(int n1,int n2){
			if(n1>n2)
			System.out.println("n1 is greater than n2");
			else
			System.out.println("n2 is greater than n1");
	}
public static void main(String args[]){
		int n1;
		int n2;
		Scanner sc=new Scanner(System.in);
		n1=sc.nextInt();
		n2=sc.nextInt();
		GreatNum(n1,n2);
	}
}

		
		