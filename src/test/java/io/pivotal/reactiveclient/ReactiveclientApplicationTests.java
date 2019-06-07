package io.pivotal.reactiveclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ReactiveclientApplication.class,
        initializers = ConfigFileApplicationContextInitializer.class)

public class ReactiveclientApplicationTests {

    @Autowired
    private ReactiveclientApplication reactiveclientApp = new ReactiveclientApplication();

    @Test
    public void contextLoads() {
    }

    @Test
    public void canGeSecuredtHelloText() {
        Assert.assertEquals("Hello Rest-User!", reactiveclientApp.getSecureHello());
    }

}
