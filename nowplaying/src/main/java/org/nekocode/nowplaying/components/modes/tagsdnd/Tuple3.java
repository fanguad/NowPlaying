/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

/**
 * A tuple with three elements.
 */
public class Tuple3<A, B, C> {
    public A a;
    public B b;
    public C c;

    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<A, B, C>(a, b, c);
    }

    public Tuple3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple3 trio = (Tuple3) o;

        if (a != null ? !a.equals(trio.a) : trio.a != null) return false;
        if (b != null ? !b.equals(trio.b) : trio.b != null) return false;
        if (c != null ? !c.equals(trio.c) : trio.c != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (c != null ? c.hashCode() : 0);
        return result;
    }
}
