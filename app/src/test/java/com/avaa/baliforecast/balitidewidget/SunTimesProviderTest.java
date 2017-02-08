package com.avaa.baliforecast.balitidewidget;

import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.data.Ports;
import com.avaa.balitidewidget.data.SunTimes;
import com.avaa.balitidewidget.data.SunTimesProviderByWiki;
import com.avaa.balitidewidget.data.SunTimesProvider;

import org.junit.Test;

import static java.lang.System.out;

public class SunTimesProviderTest {
    private static final Ports ports = new Ports(null);

    @Test
    public static void test() throws Exception {
        SunTimesProviderByWiki.SunTimes sunTimesWiki = SunTimesProviderByWiki.get(ports.get("5382").position, 0);
        out.println(sunTimesWiki.toString());

        check("5382");
        check("5379");
        check("3484");
    }

    private static void check(String portID) {
        Port port = ports.get(portID);
        SunTimes sunTimes = SunTimesProvider.get(port.position, port.getTimeZone());
        out.println(port.getName() + ": \t" + sunTimes.toString());
    }
}