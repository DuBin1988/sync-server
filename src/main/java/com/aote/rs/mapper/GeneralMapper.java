package com.aote.rs.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralMapper implements ExceptionMapper<Exception> {
	public Response toResponse(Exception ex) {
	    return Response.status(500).
	      entity("服务器内部错误").
	      type("text/plain").
	      build();
	}
}
