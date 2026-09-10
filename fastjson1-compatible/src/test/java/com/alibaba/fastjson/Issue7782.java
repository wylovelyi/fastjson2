package com.alibaba.fastjson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Issue7782 {
    static class User {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testExtraFieldIgnored() {
        JSONObject jo = new JSONObject();
        jo.put("name", "test");
        // An extra key that does not exist on the target bean. On some JDK/architecture
        // combinations (e.g. ARM JDK8 with a key like "/*") this used to raise
        // IntrospectionException. fastjson 1.2.83 silently ignored such fields; we should too.
        // See https://github.com/alibaba/fastjson2/issues/7782
        jo.put("/*", "some_value");

        User user = JSON.toJavaObject(jo, User.class);
        assertNotNull(user);
        assertEquals("test", user.getName());
    }
}
