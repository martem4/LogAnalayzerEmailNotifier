package century.loganalyzermailnotifier;

import org.junit.Test;

public class TestStringRegex {

    @Test
    public void testStringRegex() {
        String Str = new String("Добро пожаловать на ProgLang.su");

        System.out.print("Возвращаемое значение: " );
        System.out.println(Str.matches("(.*)ProgLang(.*)"));

        System.out.print("Возвращаемое значение: " );
        System.out.println(Str.matches("ProgLang"));

        System.out.print("Возвращаемое значение: " );
        System.out.println(Str.matches("Добро пожаловать(.*)"));
    }
}
