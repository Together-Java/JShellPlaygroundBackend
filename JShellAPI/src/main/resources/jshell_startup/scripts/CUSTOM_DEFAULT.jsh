void print(Object o) { System.out.print(o); }
void println(Object o) { System.out.println(o); }
void printf(String s, Object... args) { System.out.printf(s, args); }

Iterable<Integer> range(int startInclusive, int endExclusive) {
    return IntStream.range(startInclusive, endExclusive)::iterator;
}
