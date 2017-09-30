/**
 *    Copyright 2016, 2017 Peter Zybrick and others.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * 
 * @author  Pete Zybrick
 * @version 1.0.0, 2017-09
 * 
 */
package com.pzybrick.iote2e.ws.validic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.server.ServerContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import com.pzybrick.iote2e.common.config.MasterConfig;
import com.pzybrick.iote2e.ws.route.RouteValidicByteBuffer;


/**
 * The Class ThreadEntryPointValidic.
 */
public class ThreadEntryPointValidic extends Thread {
	
	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger(ThreadEntryPointValidic.class);
	
	/** The Constant serverSideSocketByteBuffer. */
	public static final Map<String, ServerSideSocketValidic> serverSideSocketByteBuffer = new ConcurrentHashMap<String, ServerSideSocketValidic>();
	
	/** The Constant fromClientByteArrays. */
	public static final ConcurrentLinkedQueue<ByteBuffer> fromClientByteArrays = new ConcurrentLinkedQueue<ByteBuffer>();
	
	/** The route validic byte buffer. */
	private RouteValidicByteBuffer routeValidicByteBuffer;
	
	/** The server. */
	private Server server;
	
	/** The connector. */
	private ServerConnector connector;
	
	/** The master config. */
	public static MasterConfig masterConfig;
	
	
	/**
	 * Instantiates a new thread entry point validic.
	 *
	 * @param masterConfig the master config
	 */
	public ThreadEntryPointValidic( MasterConfig masterConfig ) {
		ThreadEntryPointValidic.masterConfig = masterConfig;
	}


	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run( ) {
		logger.info(masterConfig.toString());
		try {
			String routerImplClassName = masterConfig.getWsValidicRouterImplClassName();
			if( null == routerImplClassName || routerImplClassName.length() == 0 ) 
				throw new Exception("WS Validic routerImplClassName is required entry in MasterConfig but is null");
			Class clazz = Class.forName(routerImplClassName);
			routeValidicByteBuffer = (RouteValidicByteBuffer)clazz.newInstance();
			routeValidicByteBuffer.init(masterConfig);

			server = new Server();
			connector = new ServerConnector(server);
			connector.setPort(masterConfig.getWsValidicServerListenPort());
			server.addConnector(connector);
	
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			server.setHandler(context);
	
			try {
				ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
				wscontainer.addEndpoint(ServerSideSocketValidic.class);
				ThreadFromClientValidic threadFromClientValidic = new ThreadFromClientValidic( routeValidicByteBuffer );
				threadFromClientValidic.start();
	
				logger.info("Server starting");
				server.start();
				logger.info("Server started");
				server.join();
				threadFromClientValidic.shutdown();
				threadFromClientValidic.join(15 * 1000L);
	
			} catch (Throwable t) {
				logger.error("Server Exception",t);
			} finally {
			}
		} catch( Exception e ) {
			logger.error(e.getMessage(), e);
		}
	}

	
	/**
	 * The Class ThreadFromClientValidic.
	 */
	public class ThreadFromClientValidic extends Thread {
		
		/** The shutdown. */
		private boolean shutdown;
		
		/** The route validic byte buffer. */
		private RouteValidicByteBuffer routeValidicByteBuffer;

		/**
		 * Instantiates a new thread from client validic.
		 *
		 * @param routeValidicByteBuffer the route validic byte buffer
		 */
		public ThreadFromClientValidic( RouteValidicByteBuffer routeValidicByteBuffer ) {
			super();
			this.routeValidicByteBuffer = routeValidicByteBuffer;
		}

		/**
		 * Shutdown.
		 */
		public void shutdown() {
			logger.info("Shutdown");
			shutdown = true;
			interrupt();
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			logger.info("ThreadFromClientValidic Run");
			List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
			try {
				while (true) {
					byteBuffers.clear();
					while (!fromClientByteArrays.isEmpty()) {
						ByteBuffer byteBuffer = fromClientByteArrays.poll();
						if( byteBuffer != null ) byteBuffers.add(byteBuffer);
					}
					for (ByteBuffer byteBuffer : byteBuffers) {
						logger.debug(">>> bytebuffer length {}",  byteBuffer.array().length );
						routeValidicByteBuffer.routeToTarget(byteBuffer);
					}
					try {
						sleep(500L);
					} catch (InterruptedException e) {
					}
					if (shutdown) {
						logger.debug("exiting due to shutdown");
						break;
					}
				}
			} catch (Exception e) {
				logger.error("Exception in source thread processing", e);
			} catch (Throwable t) {
				logger.error("Exception in source thread processing {}", t.getMessage(),t);
			} finally {
			}
			logger.info("Exit");
		}
	}
}