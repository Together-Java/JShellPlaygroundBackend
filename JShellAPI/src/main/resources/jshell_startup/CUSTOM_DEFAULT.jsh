import java.nio.charset.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import java.time.temporal.*;
import java.time.zone.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.random.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.prefs.*;
import java.util.regex.*;
import java.util.stream.*;

void print(Object o) { System.out.print(o); }
void println(Object o) { System.out.println(o); }
void printf(String s, Object... args) { System.out.printf(s, args); }

Iterable<Integer> range(int startInclusive, int endExclusive) {
    return IntStream.range(startInclusive, endExclusive)::iterator;
}
