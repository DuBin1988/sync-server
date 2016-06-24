package com.aote.rs.dir;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class TestDirService extends TestCase{
	public void testOne(){
		try {
			String path="http://127.0.0.1:8081/sync-server/rs/dir?path=" + URLEncoder.encode("e:\\www\\").replace("+", "%20");

			HttpGet getMethod =new HttpGet(path);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(getMethod);
			String result = EntityUtils.toString(response.getEntity(), "UTF8");
			System.out.println(result);
			int code = response.getStatusLine().getStatusCode();
			assertEquals(200, code);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void testTwo(){
		try {
			String path="http://127.0.0.1:8081/sync-server/rs/dir";
			// POSTMethod
			HttpPost postMethod =new HttpPost(path);
			StringEntity se = new StringEntity("e:\\www\\1.html", "UTF-8");
			postMethod.setEntity(se);
			// 发送Post请求
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(postMethod);
			String actual = EntityUtils.toString(response.getEntity(), "UTF8");
			System.out.println(actual);
			int code = response.getStatusLine().getStatusCode();
			assertEquals(200, code);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
