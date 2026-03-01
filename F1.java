import java.util.*;
class F1
{
    public static int addTwoNums(int num1, int num2)
    {
        int sum=num1+num2;
        return sum;

    }
    public static void main(String args[])
    {
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter the first number:");
        int num1=sc.nextInt();
        System.out.println("Enter the second number:");
        int num2=sc.nextInt();
        int result=addTwoNums(num1,num2);
        System.out.println("The sum of the two numbers is: "+result);
    }
}