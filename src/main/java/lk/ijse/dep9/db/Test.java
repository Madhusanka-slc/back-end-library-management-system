package lk.ijse.dep9.db;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    public static void main(String[] args) {
//        System.out.println(UUID.randomUUID().toString());
        String str="/978-3-16-148410-0";
        Pattern pattern = Pattern.compile("^/([0-9]+-[0-9]+-[0-9]+-[0-9]+-[0-9]+)/?$");
        Matcher matcher = pattern.matcher(str);
        System.out.println(matcher.matches());
        System.out.println(matcher.group(1));
    }
}
