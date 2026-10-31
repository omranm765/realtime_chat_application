package de.htwsaar.messagebroker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {
    public int[] twoSum(int[] nums, int target) {
        int indices[] = new int[2];
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    indices[0] = i;
                    indices[1] = j;
                    return indices;
                }
            }
        }

        List<Integer> array = new ArrayList<>();
        array.add(45);
        array.add(23);
        array.stream().sorted();

        return null;

    }

    public static void main(String[] args) {
        char[][] array2 = new char[6][2];

        array2[0][0] = 's';
        array2[1][0] = 'e';
        

        for (int i = 0; i < array2.length; i++) {
            for (int j = 0; j < array2[i].length; j++) {
                System.out.print("[" + array2[i][j] + "]");
            }

            System.out.println();
        }
//        int[] nums = {1, 2, 3, 4};
//        int[] bla = new Solution().twoSum(nums, 5);
//        System.out.println(bla[0] + " : " + bla[1]);
//        System.out.println(5 / 10);
//        System.out.println(new Solution().isPalindrom(1221));
//        String name = "omran";
//        name.length();
//        int a = 1;
//        int b = 2;
//        System.out.println(add(a, b));
//        int c = 3;
//        int d = 4;
//        add(c,d);
//        add(a,d);
    }

    public boolean isPalindrom(int x) {
        int[] array = String.valueOf(x)
                .chars()
                .map(c -> c - '0')
                .toArray();
        for (int i = 0; i < Math.floor(array.length / 2); i++) {
            if (array[i] != array[array.length - i - 1]) {
                return false;
            }
        }
        return true;
    }


    public int removeElement(int[] nums, int val) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            list.add(nums[i]);
        }

        list = list.stream().filter(num -> num != val).toList();
        return 0;
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static String toLowerCase(String word) {
        StringBuilder sb = new StringBuilder(word);
        sb.reverse();
        return sb.toString().toLowerCase();
    }

    public static String reverse(String word) {
        return null;
    }

    public static String swap(String word) {
        String temp;
        return null;
    }
}