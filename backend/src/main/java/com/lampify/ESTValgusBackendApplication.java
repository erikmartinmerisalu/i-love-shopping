package com.lampify;

import com.lampify.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ESTValgusBackendApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(ESTValgusBackendApplication.class, args);
    }

}
