package com.alibaba.fastjson2.issues_7800;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue7853 {
    public static class PagerBean {
        public String name;
    }

    public static class PagerDataBean<T> {
        public List<T> rows;
        public long total;
        public PagerBean pager;
    }

    public static class ResponseResult<T> {
        public int code;
        public String msg;
        public T result;
    }

    @Test
    public void test() {
        // first call binds the cached writer of the "result" field to PagerDataBean
        PagerDataBean<String> pagerData = new PagerDataBean<>();
        pagerData.rows = Collections.singletonList("x");
        pagerData.total = 1;

        ResponseResult<PagerDataBean<String>> pagerResult = new ResponseResult<>();
        pagerResult.code = 0;
        pagerResult.msg = "ok";
        pagerResult.result = pagerData;
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":{\"rows\":[\"x\"],\"total\":1}}",
                JSON.toJSONString(pagerResult));
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":{\"rows\":[\"x\"],\"total\":1}}",
                JSON.toJSON(pagerResult).toString());

        // then a different generic argument must not reuse the cached writer (ClassCastException)
        ResponseResult<Boolean> boolResult = new ResponseResult<>();
        boolResult.code = 0;
        boolResult.msg = "ok";
        boolResult.result = Boolean.TRUE;
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":true}",
                JSON.toJSON(boolResult).toString());
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":true}",
                JSON.toJSONString(boolResult));

        // a PagerDataBean with a different row type still serializes correctly
        PagerDataBean<Integer> intPagerData = new PagerDataBean<>();
        intPagerData.rows = Collections.singletonList(1);

        ResponseResult<PagerDataBean<Integer>> intResult = new ResponseResult<>();
        intResult.code = 0;
        intResult.msg = "ok";
        intResult.result = intPagerData;
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":{\"rows\":[1],\"total\":0}}",
                JSON.toJSON(intResult).toString());
        assertEquals("{\"code\":0,\"msg\":\"ok\",\"result\":{\"rows\":[1],\"total\":0}}",
                JSON.toJSONString(intResult));
    }
}
