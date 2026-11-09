package com.civiclink.auth;

import com.civiclink.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {
    @TempDir Path temp;
    @Test void dataDirectoryOverrideStartsFileDatabase() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("relocated", Map.of(
                    "civiclink.data-dir", temp.toString().replace('\\', '/'),
                    "database.url", "jdbc:h2:file:" + temp.toString().replace('\\', '/') + "/civiclink",
                    "database.username", "sa", "database.password", "",
                    "publicdata.storage-dir", temp.resolve("decisions").toString(),
                    "CIVICLINK_ADMIN_USERNAME", "", "CIVICLINK_ADMIN_PASSWORD", "")));
            context.register(WebConfig.class);
            context.refresh();
            javax.sql.DataSource source = context.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection connection = source.getConnection()) {
                assertTrue(connection.getMetaData().getURL().contains(temp.toString().replace('\\', '/')));
            }
            assertTrue(java.nio.file.Files.exists(temp.resolve("civiclink.mv.db")));
        }
    }
    @Test void allControllersAndDatabaseStartTogether() {
        try(AnnotationConfigWebApplicationContext context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of(
                    "database.url","jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1",
                    "publicdata.storage-dir",temp.toString(),"CIVICLINK_ADMIN_USERNAME","","CIVICLINK_ADMIN_PASSWORD","")));
            context.register(WebConfig.class);context.refresh();
            assertNotNull(context.getBean(com.civiclink.ai.AiController.class));
            assertNotNull(context.getBean(com.civiclink.complaint.ComplaintController.class));
            assertNotNull(context.getBean(AuthController.class));
        }
    }
}
