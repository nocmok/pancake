package com.nocmok.pancake.utils;

public class Pair<A, B> {
    
    private A first;

    private B second;

    public Pair(A first, B second){
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(A first, B second){
        return new Pair<A, B>(first, second);
    }

    public A first(){
        return first;
    }

    public B second(){
        return second;
    }
}
