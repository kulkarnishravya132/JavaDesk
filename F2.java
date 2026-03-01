import java.util.*;
class F2
{
    public static int MultiplyTwoNums(int num1, int num2)
    {
        int product=num1*num2;
        return product;
    }
    public static void main(String args[])
    {
        Scanner sc= new Scanner(System.in);
      	 int  num1= sc.nextInt();
       	int num2= sc.nextInt();
        int mul = MultiplyTwoNums(num1,num2);
        System.out.println("Multiplication of two numbers is " +mul);
        
    }
}