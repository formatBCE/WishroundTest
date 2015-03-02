package ua.pp.formatbce.wishroundtest;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Created by format on 01.03.2015
 */
public class Brackets {

    static Stack<Character> brackets = new Stack<>();
    static List<Character> openArr = Arrays.asList('(', '[', '{');
    static List<Character> closedArr = Arrays.asList(')', ']', '}');

    public static boolean check(String t) {
        for (char c : t.toCharArray()) {
            if (openArr.contains(c)) {
                brackets.push(c);
            } else if (closedArr.contains(c)) {
                if (!areMatchForClosing(brackets.pop(), c)) {
                    return false;
                }
            }
        }
        return brackets.isEmpty();
    }

    private static boolean areMatchForClosing(Character pop, char c) {
        if (pop == null) {
            return false;
        }
        for (int i = 0; i < closedArr.size(); i++) {
            if (closedArr.get(i) == c) {
                return openArr.get(i) == pop;
            }
        }
        return false;
    }
}
