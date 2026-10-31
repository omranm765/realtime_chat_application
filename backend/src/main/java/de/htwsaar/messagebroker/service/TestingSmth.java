package de.htwsaar.messagebroker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestingSmth {
    public static void main(String[] args) {
        String word = "PSCA8NL4ZSPD";
        System.out.println(String.join("  ", word.split("")));
        new TestingSmth().insertSpacesIntoWord(word);
        new TestingSmth().insertSpacesIntoWord("PFY1S40I88T9");
        new TestingSmth().insertSpacesIntoWord("MageNicolesPuzzle");
        new TestingSmth().insertSpacesIntoWord("NMI20MAJGIBP");
        new TestingSmth().insertSpacesIntoWord("YMYD76U85Z1U");
        System.out.println(new TestingSmth().countTheLetterA("hallo"));
        System.out.println(new TestingSmth().reverseString("hallo"));
        System.out.println("Count Vocals: " + new TestingSmth().countsVocals("hallo"));
        System.out.println("Is it a Palindrom? + " + new TestingSmth().isPalindrom("level"));
    }

    public void insertSpacesIntoWord(String word){
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            result.append(word.charAt(i));
/**
 * java: java.lang.ExceptionInInitializerError
 * com.sun.tools.javac.code.TypeTag :: UNKNOWN
 */
            if (i < word.length() - 1) {
                result.append("  ");
            }
        }

        System.out.println(result.toString());
    }

    public int countTheLetterA(String word){
        int counter = 0;
        for (int i = 0; i < word.length(); i++){
            if (word.charAt(i) == 'a') {
                counter++;
            }
        }
        return counter;
    }

    public String reverseString(String word){
        StringBuilder sb = new StringBuilder(word);
        return sb.reverse().toString();
    }

    public int countsVocals(String word){
        int counter = 0;
        String regex = "[aeiou]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(word);
        for (int i = 0; i < word.length(); i++){
            if (matcher.find()) {
                counter++;
            }
        }
        return counter;
    }

    public boolean isPalindrom(String word){
        if (word.length() <= 1){
            return true;
        }

        if (word.toLowerCase().charAt(0) != word.toLowerCase().charAt(word.length()-1)){
            return false;
        }
        return isPalindrom(word.substring(1, word.length()-1));
    }
}
