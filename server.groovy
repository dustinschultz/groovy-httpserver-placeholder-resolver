/**
 * Copyright (C) [2013] [dustinschultz]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Grapes([
	@Grab(group='javax.servlet', module='javax.servlet-api', version='3.0.1'),
	@Grab(group='org.eclipse.jetty.aggregate', module='jetty-all-server', version='8.1.11.v20130520', transitive=false),
	@Grab(group='org.springframework', module='spring-core', version='3.2.3.RELEASE'),
	@Grab(group='org.springframework', module='spring-beans', version='3.2.3.RELEASE')
])

import javax.servlet.ServletException
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.util.PropertyPlaceholderHelper
import org.springframework.core.io.ClassPathResource

def resourceHandler = new PlaceHolderFilter()
resourceHandler.with {
	resourceBase = './'
}

def server = new Server(8080)
def handlers = new HandlerList();

handlers.with {
	setHandlers([resourceHandler] as Handler[]);
}

server.with {
	setHandler(handlers)
	start()
}

class PlaceHolderFilter extends ResourceHandler {

	private def urlProperties = new Properties()

	PlaceHolderFilter() {
		def propertiesStream = new ClassPathResource('placeholders.properties').inputStream
		urlProperties.load(propertiesStream)
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
		def capturedResponse = new CaptureResponseWrapper(response)
		super.handle(target, baseRequest, request, capturedResponse);
		def placeHolderResolver = new PropertyPlaceholderHelper(PropertyPlaceholderConfigurer.DEFAULT_PLACEHOLDER_PREFIX, PropertyPlaceholderConfigurer.DEFAULT_PLACEHOLDER_SUFFIX);
		def replacedResponse = placeHolderResolver.replacePlaceholders(capturedResponse.toString(), urlProperties)
		response.writer.print(replacedResponse)
	}
}

class CaptureResponseWrapper extends HttpServletResponseWrapper {
	private def baos = new ByteArrayOutputStream();

	public CaptureResponseWrapper(HttpServletResponse response) {
		super(response)
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				baos.write(b)
			}
		}
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(baos)
	}

	@Override
	public String toString() {
		return baos.toString();
	}
}
