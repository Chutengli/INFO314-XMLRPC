package edu.uw.info314.xmlrpc.server;

public class Calc {
    public int add(int... args) {
        long result = 0;
        for (int arg : args) { result += arg; }
        if(result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
            throw new ArithmeticException("Overflow!");
        }
        return (int) result;
    }
    public int subtract(int lhs, int rhs) { return lhs - rhs; }
    public int multiply(int... args) {
        long result = 1;
        for (int arg : args) { result *= arg; }
        if(result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
            throw new ArithmeticException("Overflow!");
        }
        return (int) result;
    }
    public int divide(int lhs, int rhs) { return lhs / rhs; }
    public int modulo(int lhs, int rhs) { return lhs % rhs; }
}
