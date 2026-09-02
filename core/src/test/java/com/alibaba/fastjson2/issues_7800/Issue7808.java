package com.alibaba.fastjson2.issues_7800;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guard test for gh-7808 (ArrayIndexOutOfBoundsException in JSONReaderASCII field-name decoding).
 *
 * <p>On main the bug comes from JSONReaderASCII inheriting readFieldNameHashCode0() from
 * JSONReaderUTF8, which decodes field names as UTF-8 and counts <em>characters</em> into
 * nameLength, while getFieldName() walks the same span as latin1 <em>bytes</em> and sizes its
 * output buffer from that same nameLength.
 *
 * <p>This branch is not affected: JSONReaderASCII overrides readFieldNameHashCode() and counts
 * one nameLength unit per unescaped character in its own latin1 loop, so nameLength always equals
 * the number of chars getFieldName() writes. Also, JSONReader.of(String) always builds a
 * JSONReaderUTF16 here, so JSONReaderASCII is only reachable through byte[] input decoded as
 * US_ASCII / ISO_8859_1 -- which is what the cases below exercise.
 */
class Issue7808 {
    // JSON source text for the field name: a backslash+t escape (2 source chars, unescaped by the
    // parser to one TAB) followed by 3 raw latin1 bytes (E0 AA AE) that a UTF-8 field-name decoder
    // would misread as a single 3-byte character, undercounting nameLength. See gh-7808.
    private static final String KEY_SOURCE = "\\t\u00e0\u00aa\u00ae";
    // The field name after JSON unescaping: TAB + the 3 latin1 characters.
    private static final String KEY_PARSED = "\t\u00e0\u00aa\u00ae";

    static JSONArray parseLatin1Array(String json) {
        byte[] bytes = json.getBytes(StandardCharsets.ISO_8859_1);
        return JSON.parseArray(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
    }

    static JSONObject parseLatin1Object(String json) {
        byte[] bytes = json.getBytes(StandardCharsets.ISO_8859_1);
        return JSON.parseObject(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1, JSONObject.class);
    }

    @Test
    public void fieldNameWithEscapeAndNonAsciiInsideArray_doesNotThrow() {
        String json = "[{\"" + KEY_SOURCE + "\":1}]";

        JSONArray array = assertDoesNotThrow(() -> parseLatin1Array(json));
        JSONObject obj = array.getJSONObject(0);
        assertEquals(1, obj.size());
        assertEquals(1, obj.getIntValue(KEY_PARSED));
    }

    @Test
    public void nonAsciiWithoutEscape_stillParses() {
        // Control: same non-ASCII bytes, no escape -- takes the byte-count fast path.
        JSONObject obj = parseLatin1Array("[{\"a\u00e0\u00aa\u00ae\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("a\u00e0\u00aa\u00ae"));
    }

    @Test
    public void escapeWithoutNonAscii_stillParses() {
        // Control: escape present, but no byte >= 0x80 -- nameLength was already correct.
        JSONObject obj = parseLatin1Array("[{\"\\tabc\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("\tabc"));
    }

    @Test
    public void sameFieldNameAtTopLevel_stillParses() {
        // Control: not inside an array -- doesn't route through ObjectReaderImplObject.
        JSONObject obj = parseLatin1Object("{\"" + KEY_SOURCE + "\":1}");
        assertEquals(1, obj.getIntValue(KEY_PARSED));
    }

    @Test
    public void multipleObjectsInArray_stillParse() {
        JSONArray array = parseLatin1Array("[{\"a\":1},{\"b\":2}]");
        assertEquals(1, array.getJSONObject(0).getIntValue("a"));
        assertEquals(2, array.getJSONObject(1).getIntValue("b"));
    }

    @Test
    public void unicodeEscapedFieldName_stillParses() {
        JSONObject obj = parseLatin1Array("[{\"\\u00e9\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("\u00e9"));
    }

    @Test
    public void backslashEscapedFieldName_stillParses() {
        JSONObject obj = parseLatin1Array("[{\"a\\\\b\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("a\\b"));
    }

    @Test
    public void quoteEscapedFieldName_stillParses() {
        JSONObject obj = parseLatin1Array("[{\"a\\\"b\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("a\"b"));
    }

    @Test
    public void emptyFieldName_stillParses() {
        JSONObject obj = parseLatin1Array("[{\"\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue(""));
    }

    @Test
    public void controlCharacterEscapes_stillParse() {
        JSONObject obj = parseLatin1Array("[{\"\\n\\t\\r\":1}]").getJSONObject(0);
        assertEquals(1, obj.getIntValue("\n\t\r"));
    }
}
