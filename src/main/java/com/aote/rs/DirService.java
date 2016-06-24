package com.aote.rs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;

//目录服务
@Path("dir")
public class DirService {
	static Logger log = Logger.getLogger(DirService.class);

	//获取所有卡文件的最后修改时间
	@GET
	public String files(@QueryParam("path") String path) {
		String result = "";
		if(path == null || path.trim().length() == 0)
			path="D:/card";
		log.debug(path);
		//递归获取所有文件的最后修改日期
		File file = new File(path);
		List<File> files = list(file);
		for(File f : files) {
			if(!result.equals("")) {
				result += "|";
			}
			result += f.getPath() + "," + f.lastModified();
		}
		log.debug(result);
		return result;
	}
	
	@POST
	public String getFile(@Context HttpServletResponse response, String fileName)
	{
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/octet-stream");
			// 把文件的内容送入响应流中
			log.debug(fileName);
			InputStream is = new FileInputStream(fileName);
			OutputStream os = new BufferedOutputStream(response
					.getOutputStream());
			transformStream(is, os);
			is.close();
			os.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	

	public void transformStream(InputStream is, OutputStream os) {
		try {
			byte[] buffer = new byte[1024];
			// 读取的实际长度
			int length = is.read(buffer);
			while (length != -1) {
				os.write(buffer, 0, length);
				length = is.read(buffer);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	//递归获取目录下所有文件，包括子目录
	private List<File> list(File dir) {
		List<File> result = new ArrayList<File>();
		File[] files = dir.listFiles();
		for(File file : files) {
			if(file.isDirectory()) {
				result.addAll(list(file));
			} else {
				result.add(file);
			}
		}
		return result;
	}
}
