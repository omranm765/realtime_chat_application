package de.htwsaar.messagebroker.service;

import java.util.Arrays;
import java.util.Scanner;

public class test{
    public static void main(String[] args){
//        System.out.println(isPalindrome(12121));
        //Scanner sc = new Scanner(System.in);
        //System.out.println("zahl eingeben");
        //print1(sc.nextInt());
        System.out.println("أهل");
    }

    public static void print(int n){
        String starCount = "";
        for(int i = 0; i < n; i++) {
            starCount += "*";
        }
        for (int i = 0; i < starCount.length(); i++) {
            System.out.println(starCount);
        }
    }

    public static void print1(int zahl) {
        StringBuilder space = new StringBuilder();
        for (int i = 1; i <= zahl; i++){
            System.out.println();
            System.out.print(space.toString());
            for (int j = i; j <= zahl; j++) {
                System.out.print((j+1-i));
            }
            space.append(" ");
        }
    }

    public static boolean isPalindrome(int x) {
        if (x/10 == 0) {
            return true;
        }

        while(x/10 != 0) {
            int length = String.valueOf(x).length();
            int pow = (int) Math.pow(10, length-1);
            if (x%10 != x/pow) {
                System.out.println("in if = " + x);
                return false;
            }
            System.out.println("nicht in if = " + x);
            x = x/10;
            StringBuilder sb = new StringBuilder(String.valueOf(x));
            sb.reverse();
            x = Integer.parseInt(sb.toString());
            x = x/10;
        }
        return true;
    }
}