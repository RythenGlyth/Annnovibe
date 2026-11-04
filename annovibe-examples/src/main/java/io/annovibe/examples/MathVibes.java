package io.annovibe.examples;

import io.annovibe.AI;
import io.annovibe.vibe;

public class MathVibes {

    @AI(value = "calculate the factorial of n recursively; if n <= 1 return 1L", provider = "mock")
    public static long factorial(int n) {
        return 0L; // Stub body will be replaced at compile-time by Annovibe!
    }

    @vibe(value = "return true if the string is a palindrome ignoring case and spaces, else false", provider = "mock")
    public static boolean isPalindrome(String str) {
        return false; // Stub body
    }

    @AI(value = "reverse the string; return empty string if input is null", provider = "mock")
    public static String reverseString(String input) {
        return null; // Stub body
    }

    @vibe(value = "calculate sum of a and b", provider = "mock")
    public static int add(int a, int b) {
        return 0; // Stub body
    }

    @io.annovibe.annotations.Vibe(value = "return the maximum of two integers a and b", provider = "mock")
    public static int max(int a, int b) {
        return 0;
    }

    @vibe(value = "simulate syntax error then self heal to return 777", provider = "mock")
    public static int selfHealDemo() {
        return 0;
    }
}
