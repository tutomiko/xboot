/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.io.Source;
import com.xahico.boot.lang.html.HTMLDocument;
import com.xahico.boot.lang.html.HTMLException;
import com.xahico.boot.lang.html.HTMLNode;
import com.xahico.boot.lang.html.HTMLParser;
import com.xahico.boot.lang.html.HTMLStandardType;
import com.xahico.boot.lang.html.HTMLUtilities;
import com.xahico.boot.lang.html.fx.HTFXParser;
import com.xahico.boot.lang.jsox.JSOXVariant;
import com.xahico.boot.reflection.ClassFactory;
import java.io.IOException;
import com.xahico.boot.pilot.ServiceFactorizer;
import com.xahico.boot.pilot.ServiceInitializer;
import com.xahico.boot.pilot.ServiceProvider;
import com.xahico.boot.util.CollectionUtilities;
import com.xahico.boot.util.Exceptions;
import com.xahico.boot.util.FileCache;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.net.ssl.KeyManagerFactory;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public final class GWXServiceProvider <T extends GWXSession> extends ServiceProvider {
	private static final int    DEFAULT_SESSION_TIMEOUT_SECONDS = 10;
	private static final String KEYWORD_ERROR = "error";
	private static final String KEYWORD_RETURNS = "returns";
	private static final String KEYWORD_STATUS = "status";
	private static final String PATH_BASE_API = "/api";
	private static final String PATH_BASE_DOWNLOAD = "/get";
	private static final int    PORT_INSECURE = 80;
	private static final int    PORT_SECURE = 443;
	
	
	
	@ServiceFactorizer
	protected static GWXServiceProvider createService (final GWXService service, final ClassFactory<? extends GWXSession> instanceFactory){
		try {
			return new GWXServiceProvider(instanceFactory);
		} catch (final IOException ex) {
			throw new Error(ex);
		}
	}
	
	@ServiceInitializer
	protected static void initializeService (final GWXService service, final GWXServiceProvider serviceProvider) throws Throwable {
		serviceProvider.setBindPort(service.port());
		serviceProvider.setSessionTimeout(service.timeout());
	}
	
	private static HttpContent preparedChunk (){
		return new DefaultHttpContent(Unpooled.copiedBuffer(new byte[]{'\n'}));
	}
	
	
	
	private Charset                              charset = StandardCharsets.UTF_8;
	private boolean                              enableActions = false;
	private boolean                              enableEvents = false;
	private File                                 interfaceRoot = null;
	private int                                  port = -1;
	private boolean                              retainHandles = false;
	private boolean                              retainEvents = false;
	private KeyManagerFactory                    ssl = null;
	private boolean                              useCachedRendering = true;
	private File                                 webClassRoot = null;
	private File                                 webInterfaceFile = null;
	private File                                 webRoot = null;
	
	private ServerBootstrap                      boot = null;
	private ServerBootstrap                      bootSSL = null;
	private EventLoopGroup                       boss = null;
	private GWXCallTable                         callTable = null;
	private final GWXNamespace                   globalNamespace = new GWXNamespace();
	private final ClassFactory<T>                instanceFactory;
	private GWXAPIInterfaceManager               interfaceManager = null;
	private final AttributeKey<String>           keyAuth = AttributeKey.valueOf("auth");
	private final AttributeKey<GWXEventListener> keyListener = AttributeKey.valueOf("websocket");
	private final AttributeKey<T>                keySession = AttributeKey.valueOf("session");
	private Channel                              listener = null;
	private Channel                              listenerSSL = null;
	private final GWXResourceManager             rcm;
	private Map<String, ?>                       renderingCache = null;
	private FileCache                            renderingStaticCache = null;
	private final Map<String, T>                 sessions = new ConcurrentHashMap<>();
	private int                                  sessionTimeout = 0;
	private boolean                              useSSL = false;
	private GWXWebInterface                      webInterface = null;
	private final GWXNamespace                   webNamespace = new GWXNamespace(globalNamespace);
	private EventLoopGroup                       worker = null;
	
	
	
	public GWXServiceProvider (final Class<T> instanceClass) throws IOException {
		this(ClassFactory.getClassFactory(instanceClass));
	}
	
	public GWXServiceProvider (final ClassFactory<T> instanceFactory) throws IOException {
		super();
		
		this.instanceFactory = instanceFactory;
		this.rcm = new GWXResourceManager(this, instanceFactory.getProductionClass());
	}
	
	
	
	@Override
	protected void cleanup (){
		if (null != listener) try {
			listener.closeFuture().sync();
		} catch (final InterruptedException ex) {
			Exceptions.ignore(ex);
		} finally {
			listener = null;
		}
		
		if (null != listenerSSL) try {
			listenerSSL.closeFuture().sync();
		} catch (final InterruptedException ex) {
			Exceptions.ignore(ex);
		} finally {
			listenerSSL = null;
		}
		
		if (null != worker) {
			worker.shutdownGracefully();
			worker = null;
		}
		
		if (null != boss) {
			boss.shutdownGracefully();
			boss = null;
		}
		
		if (null != interfaceManager) {
			interfaceManager.destroy();
			interfaceManager = null;
		}
	}
	
	private T createInstance (){
		final T session;
		
		session = this.instanceFactory.newInstance();
		session.assignCallTable(callTable);
		session.assignCallbacks(new GWXSession.Callbacks() {
			@Override
			public void onAuthenticate (final GWXSession session){
				System.out.println("created auth session for '" + session.getToken() + "'");
				sessions.put(session.getToken(), ((T)session));
			}
			
			@Override
			public void onAuthenticateReset (final GWXSession session){
				System.out.println("reset auth for "+session.getToken());
				sessions.remove(session.getToken());
			}
			
			@Override
			public void onDestroy (final GWXSession session){
				if (null != session.getToken()) {
					sessions.remove(session.getToken());
				}
			}
			
			@Override
			public void onEventListenerConnect (final GWXSession session, final GWXEventListener listener){
				System.out.println("Connected event listener to session " + session);
				
				listener.attach(session);
			}
			
			@Override
			public void onEventListenerDisconnect (final GWXSession session, final GWXEventListener listener){
				System.out.println("Disconnected event listener from session " + session);
				
				listener.detach();
			}
		});
		session.attachExecutor(this.getExecutor());
		session.setRetainEvents(this.retainEvents);
		session.setRetainHandles(this.retainHandles);
		session.initLocals(rcm);
		session.initNamespace(webNamespace);
		session.timeout(this.sessionTimeout);
		session.onCreate();
		
		return session;
	}
	
	private void destroyInstance (final T session){
		session.destroy();
	}
	
	private void dispatchIdleCountdown (final T session){
		this.getExecutor().execute(new Runnable() {
			final long dispatchedFor = session.getLastContact();
			
			@Override
			public void run (){
				if (session.getLastContact() == dispatchedFor) {
					if (session.lastContactSecondsElapsed() < sessionTimeout) {
						GWXServiceProvider.this.getExecutor().execute(this);
					} else if (! session.isEventListenerPresent()) {
						destroyInstance(session);
					}
				}
			}
		});
	}
	
	public Charset getCharset (){
		return this.charset;
	}
	
	public boolean getEnableActions (){
		return this.enableActions;
	}
	
	public boolean getEnableEvents (){
		return this.enableEvents;
	}
	
	public File getInterfaceRoot (){
		return this.interfaceRoot;
	}
	
	public boolean getRetainEvents (){
		return this.retainEvents;
	}
	
	public KeyManagerFactory getSSL (){
		return this.ssl;
	}
	
	@Override
	protected void initialize () throws Throwable {
		final SslContext        ctxSSL;
		final KeyManagerFactory kmfSSL;
		
		if (useCachedRendering) {
			renderingCache = new ConcurrentHashMap<>();
			renderingStaticCache = FileCache.createConcurrentTextFileCache();
		}
		
		if (null != interfaceRoot) {
			interfaceManager = new GWXAPIInterfaceManager(rcm, interfaceRoot, this.getExecutor());
		}
		
		if (null != webInterfaceFile) {
			webInterface = new GWXWebInterface(webInterfaceFile, () -> {
				webNamespace.clear();
				
				for (final var variable : webInterface.getVariables()) {
					webNamespace.set(variable.key, variable.value);
				}
			});
		}
		
		globalNamespace.set(GWXWebBridgeBuilder.REF_SELF, GWXWebBridgeBuilder.VAR_SELF);
		
		kmfSSL = this.getSSL();
		
		if (null != kmfSSL) {
			useSSL = true;
		}
		
//		SelfSignedCertificate ssc = new SelfSignedCertificate();
//		ctxSSL = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

		if (useSSL) 
			ctxSSL = SslContextBuilder.forServer(kmfSSL).build();
		else {
			ctxSSL = null;
		}
		
		boss = new NioEventLoopGroup(1);
		worker = new NioEventLoopGroup();
		
		System.out.println("UseSSL " + useSSL);
		System.out.println("Port " + port);
		
		if (!useSSL || (port == 0)) {
			boot = new ServerBootstrap();
			boot.group(boss, worker);
			boot.channel(NioServerSocketChannel.class);
			boot.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel (final SocketChannel channel){
					final ChannelPipeline pipeline;

					pipeline = channel.pipeline();

					pipeline.addLast("gzip", new HttpContentCompressor());
					pipeline.addLast("chunked", new ChunkedWriteHandler());
					pipeline.addLast("httpCodec", new HttpServerCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));

					if (useSSL && (port == 0)) {
						pipeline.addLast(new WebSocketForceInsecureUpgradeHandler());
						pipeline.addLast(new HttpInsecureRedirectHandler());

						return;
					}

					if (enableEvents) {
						pipeline.addLast(new WebSocketServerProtocolHandler(PATH_BASE_API));
					}

					// authentication (HTTP only, before WebSocket)
					pipeline.addLast(new AuthHandler());

					if (enableEvents) {
						// websock handshake handler
						pipeline.addLast(new WebSocketUpgradeHandler());
						
						// websock keepalive handler
						pipeline.addLast(new WebSocketKeepAliveHandler());
						
						if (null != webRoot) {
							// sse event handler
							pipeline.addLast(new SseEventHandler());
						}
					}
					
					if (enableActions) {
						// http api requests
						pipeline.addLast(new HttpApiHandler());
					}
					
					if (null != webInterface) {
						// http downloads
						pipeline.addLast(new HttpDownloadHandler());
					}
					
					if (null != webRoot) {
						// http web
						pipeline.addLast(new HttpWebHandler());
					}
					
					// lifecycle (cleanup)
					pipeline.addLast(new SessionLifecycleHandler());
				}
			});
		}
		
		if (useSSL) {
			bootSSL = new ServerBootstrap();
			bootSSL.group(boss, worker);
			bootSSL.channel(NioServerSocketChannel.class);
			bootSSL.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel (final SocketChannel channel){
					final ChannelPipeline pipeline;
					
					pipeline = channel.pipeline();
					
					pipeline.addLast("ssl", ctxSSL.newHandler(channel.alloc()));
					pipeline.addLast("gzip", new HttpContentCompressor());
					pipeline.addLast("chunked", new ChunkedWriteHandler());
					pipeline.addLast("httpCodec", new HttpServerCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
					
					if (enableEvents) {
						pipeline.addLast(new WebSocketServerProtocolHandler(PATH_BASE_API));
					}
					
					// authentication (HTTP only, before WebSocket)
					pipeline.addLast(new AuthHandler());
					
					if (enableEvents) {
						// websock handshake handler
						pipeline.addLast(new WebSocketUpgradeHandler());
						
						// websock keepalive handler
						pipeline.addLast(new WebSocketKeepAliveHandler());
						
						if (null != webRoot) {
							// sse event handler
							pipeline.addLast(new SseEventHandler());
						}
					}
					
					if (enableActions) {
						// http api requests
						pipeline.addLast(new HttpApiHandler());
					}
					
					if (null != webInterface) {
						// http downloads
						pipeline.addLast(new HttpDownloadHandler());
					}
					
					if (null != webRoot) {
						// http web
						pipeline.addLast(new HttpWebHandler());
					}
					
					// lifecycle (cleanup)
					pipeline.addLast(new SessionLifecycleHandler());
				}
			});
		}
		
		callTable = new GWXCallTable() {
			
		};
	}
	
	@Override
	public boolean isIdle (){
		return false;
	}
	
	@Override
	public boolean isStepper (){
		return false;
	}
	
	@Override
	protected void run (){
		try {
			if (null != boot) {
				final int bindPort;
				
				if (this.port != 0) {
					bindPort = this.port;
				} else {
					bindPort = PORT_INSECURE;
				}
				
				listener = boot.bind(bindPort).sync().channel();
				
				System.out.println("Serving raw on port " + bindPort);
			}
			
			if (null != bootSSL) {
				final int bindPort;
				
				if (this.port != 0) {
					bindPort = this.port;
				} else {
					bindPort = PORT_SECURE;
				}
				
				listenerSSL = bootSSL.bind(bindPort).sync().channel();
				
				System.out.println("Serving SSL on port " + bindPort);
			}
		} catch (final InterruptedException ex) {
			Exceptions.ignore(ex);
		}
	}
	
	public void setBindPort (final int port){
		this.port = port;
	}
	
	public void setCharset (final Charset charset){
		this.charset = charset;
	}
	
	public void setEnableActions (final boolean enableActions){
		this.enableActions = enableActions;
	}
	
	public void setEnableEvents (final boolean enableEvents){
		this.enableEvents = enableEvents;
	}
	
	public void setInterfaceRoot (final File interfaceRoot){
		this.interfaceRoot = interfaceRoot;
	}
	
	public void setRetainEvents (final boolean retainEvents){
		this.retainEvents = retainEvents;
	}
	
	public void setRetainHandles (final boolean retainHandles){
		this.retainHandles = retainHandles;
	}
	
	public void setSessionTimeout (final int timeoutSeconds){
		this.sessionTimeout = (timeoutSeconds != 0 ? timeoutSeconds : DEFAULT_SESSION_TIMEOUT_SECONDS);
	}
	
	public void setSSL (final KeyManagerFactory ssl){
		this.ssl = ssl;
	}
	
	public void setUseCachedRendering (final boolean useCachedRendering){
		this.useCachedRendering = useCachedRendering;
	}
	
	public void setWebClassRoot (final File webClassRoot){
		this.webClassRoot = webClassRoot;
	}
	
	public void setWebInterface (final File webInterface){
		this.webInterfaceFile = webInterface;
	}
	
	public void setWebRoot (final File webRoot){
		this.webRoot = webRoot;
	}
	
	
	
	private class AuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			try{
			final T      session;
			final String token;
			final String tokenCookieName;
			boolean      tokenFromCookie = false;
			final String tokenHeader;
			
			tokenCookieName = GWXUtilities.buildTokenIdentity(instanceFactory.getProductionClass());
			
			tokenHeader = request.headers().get("Authorization");
			
			if (null != tokenHeader) {
				if ((tokenHeader.length() <= 7) || !tokenHeader.startsWith("Bearer ")) {
					token = null;
				} else {
					token = tokenHeader.substring(7);
				}
			} else if (null != webRoot) {
				final String cookieHeader;
				
				cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
				
				if (null != cookieHeader) {
					final Cookie      cookie;
					final Set<Cookie> cookies;
					
					cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
					
					cookie = CollectionUtilities.seek(cookies, (__) -> __.name().equals(tokenCookieName), false, null);
					
					if (null != cookie) {
						token = cookie.value();
						
						tokenFromCookie = true;
					} else {
						token = null;
					}
				} else {
					token = null;
				}
			} else {
				token = null;
			}
			
			if (null == token) {
				session = createInstance();
				
				ctx.channel().attr(keySession).set(session);
			} else {
				ctx.channel().attr(keyAuth).set(token);
				
				session = sessions.get(token);
				
				if (null != session) {
					session.updateLastContact();
					
					ctx.channel().attr(keySession).set(session);
				}
			}
			
			if (null == session) {
				final HttpResponse response;
				
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
				
				if (tokenFromCookie) {
					response.headers().set(HttpHeaderNames.SET_COOKIE, "%s=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=None".formatted(tokenCookieName));
				}
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			} else {
				ctx.channel().attr(keySession).set(session);
				
				ctx.channel().attr(keyAuth).set(token);
				
				ctx.fireChannelRead(request.retain());
			}
			}catch (Throwable t){
				t.printStackTrace();
			}
		}
		
		private void completeResponse (final HttpRequest request, final HttpResponse response){
			final String origin;
			
			origin = request.headers().get(HttpHeaderNames.ORIGIN);
			
			if (null != origin) {
				response.headers()
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,OPTIONS,POST,PUT")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Origin,Accept,X-Requested-With,Content-Type,Authorization");
			}
		}
	}
	
	private class HttpApiHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			try{
			final GWXAPIInterface.Handle interfaceHandle;
			final String                 method;
			final GWXRoute               route;
			final T                      session;
			final long                   whenBegin;
			
			whenBegin = System.currentTimeMillis();
			
			try {
				route = GWXRoute.parseFullString(new QueryStringDecoder(request.uri()).path(), "/");
			} catch (final GWXInvalidRouteException ex) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (! route.root.equalsIgnoreCase(PATH_BASE_API)) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			session = ctx.channel().attr(keySession).get();
			
			if (null == session) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			method = request.method().name();

			interfaceHandle = session.getInterface(route.version, () -> {
				return interfaceManager.requireInterface(route.version);
			});
			
			if (null == interfaceHandle) {
				final HttpResponse response;
				
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);

				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			} else {
				final GWXPermission          interfaceAccess;
				final GWXAPIInterface.Method interfaceMethod;
				
				interfaceAccess = GWXPermission.transformHttpMethod(method);
				
				interfaceMethod = interfaceHandle.lookupMethod(interfaceAccess, route.path);
				
				if (null == interfaceMethod) {
					final HttpResponse response;
					
					if (! retainHandles) {
						interfaceHandle.release();
					}
					
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				} else if (interfaceMethod.authorized && !session.isAuthenticated()) {
					final HttpResponse response;
					
					if (! retainHandles) {
						interfaceHandle.release();
					}
					
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				} else {
					final GWXContext  context;
					final GWXExchange exchange;
					final GWXInstance instance;
					final ByteBuf     requestBuffer;
					final String      requestString;
					
					context = rcm.buildContext(session, interfaceMethod.pattern, route.path, interfaceAccess);
					
					if (! context.checkAccess(session, interfaceMethod.require)) {
						final HttpResponse response;
						
						if (! retainHandles) {
							interfaceHandle.release();
						}
						
						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
						
						ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
						
						return;
					}
					
					exchange = new GWXExchange(ctx, method);
					exchange.request.assume(GWXUtilities.parseQueryString(request.uri()));
					
					requestBuffer = request.content();
					requestString = requestBuffer.toString(charset);
					
					if (! requestString.isBlank()) {
						exchange.request.assume(requestString, false);
					}
					
					instance = new GWXInstance(rcm, interfaceHandle.version(), session);
					
					try {
						final GWXObject result;
						
						result = interfaceMethod.call(context, instance, exchange);
						
						if (! interfaceMethod.async) {
							exchange.ready(result, null);
						}
					} catch (final GWXException ex) {
						exchange.ready(null, ex);
					} catch (Throwable t) {
						t.printStackTrace();
					}
					
					exchange.ready((result, error) -> {
						final HttpResponse response;
						final long         whenEnd;
						
						if (! retainHandles) {
							interfaceHandle.release();
						}
						
						if (null == error) {
							exchange.response.putString(KEYWORD_STATUS, GWXStatus.SUCCESS.name());
						} else {
							// if an error occurred then none of the response content matters
							// so we may confidently clear anything that may have been put into it
							exchange.response.clear();
							exchange.response.putString(KEYWORD_ERROR, error.getMessage());
							exchange.response.putString(KEYWORD_STATUS, error.status().name());
						}
						
						if (null == result) {
							final byte[] xhead;
							
							exchange.response.putInteger(KEYWORD_RETURNS, 0);
							
							xhead = exchange.response.toJSONStringCompact().getBytes(charset);
							
							response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
							
							response.headers()
								  .set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(GWXSupportedMimeType.NDJSON, charset))
								  .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
							
							ctx.write(response);
							ctx.write(Unpooled.copiedBuffer(xhead));
							ctx.write(preparedChunk());
							ctx.flush();
							
							ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
						} else if (result instanceof GWXNode serializable) {
							final byte[] xhead;
							final byte[] xbody;
							
							xhead = exchange.response.toJSONStringCompact().getBytes(charset);
							
							if (! session.checkAccess(serializable, GWXPermission.READ)) {
								exchange.response.putInteger(KEYWORD_RETURNS, 0);
								
								xbody = new JSOXVariant().toJSONStringCompact().getBytes(charset);
							} else {
								exchange.response.putInteger(KEYWORD_RETURNS, 1);
								
								xbody = serializable.snapshot().toJSONStringCompact().getBytes(charset);
							}
							
							response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
							
							response.headers()
								  .set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(GWXSupportedMimeType.NDJSON, charset))
								  .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
							
							ctx.write(response);
							ctx.write(Unpooled.copiedBuffer(xhead));
							ctx.write(preparedChunk());
							ctx.write(Unpooled.copiedBuffer(xbody));
							ctx.write(preparedChunk());
							
							ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
						} else if (result instanceof GWXNodeCollection<?> serializable) {
							final boolean requireCheckExplicitAccess;
							final byte[]  xhead;
							
							if (session.isPrivileged() || session.isOwnerOf(serializable) || session.checkAccess(serializable, GWXPermission.READ)) {
								requireCheckExplicitAccess = false;
							} else {
								requireCheckExplicitAccess = true;
							}
							
							if (! requireCheckExplicitAccess) {
								exchange.response.putInteger(KEYWORD_RETURNS, serializable.size());
							} else {
								exchange.response.putInteger(KEYWORD_RETURNS, -1);
							}
							
							xhead = exchange.response.toJSONStringCompact().getBytes(charset);
							
							response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
							
							response.headers()
								  .set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(GWXSupportedMimeType.NDJSON, charset))
								  .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
							
							ctx.write(response);
							ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer(xhead)));
							ctx.write(preparedChunk());
							ctx.flush();
							
							serializable.walk((element) -> {
								if (!requireCheckExplicitAccess || session.checkAccess(element, GWXPermission.READ)) {
									final byte[] datax;
									
									datax = element.snapshot().toJSONStringCompact().getBytes(charset);

									ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer(datax)));
									ctx.write(preparedChunk());
									ctx.flush();
								}
							}, () -> {
								ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
							});
						} else {
							response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
							
							ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
						}
						
						whenEnd = System.currentTimeMillis();
						
						System.out.println("@HttpApiHandler took %d millisecond(s)".formatted(whenEnd - whenBegin));
					});
				}
			}
			} catch (Throwable t){
				t.printStackTrace();
			}
		}
	}
	
	private class HttpDownloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			final String               method;
			final HttpResponse         response;
			final GWXRoute             route;
			final ChannelFuture        sendFileFuture;
			final T                    session;
			final Path                 target;
			final long                 targetSize;
			final GWXSupportedMimeType targetType;
			final long                 whenBegin;
			
			whenBegin = System.currentTimeMillis();
			
			try {
				route = GWXRoute.parseSemiString(URLDecoder.decode(request.uri(), charset), "/", -1);
			} catch (final GWXInvalidRouteException ex) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (! route.root.equalsIgnoreCase(PATH_BASE_DOWNLOAD)) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			method = request.method().name();
			
			if (! request.method().equals(HttpMethod.GET)) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			session = ctx.channel().attr(keySession).get();
			
			if (null == session) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (null != webInterface) {
				final GWXContext                      context;
				final GWXWebInterface.ArtifactHandler handler;
				final GWXInstance                     instance;
				final String                          result;
				
				webInterface.update();
				
				if (! webInterface.available()) {
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
					
					return;
				}
				
				handler = webInterface.lookupArtifactHandler(route.path, session.isAuthenticated());
				
				if (null == handler) {
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
					
					return;
				}
				
				instance = new GWXInstance(rcm, 0, session);
				
				context = rcm.buildContext(session, handler.pattern, route.path);
				
				result = handler.call(context, instance);
				
				if (null == result) {
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
					
					return;
				}
				
				target = Paths.get(result);
			} else {
				target = webRoot.toPath().resolve(route.path.toString()).normalize();
			}
			
			if (null == target) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			try {
				targetSize = Files.size(target);
			} catch (final FileNotFoundException | NoSuchFileException ex) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			} catch (final IOException ex) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			try {
				targetType = GWXUtilities.detectMimeType(target, GWXSupportedMimeType.BINARY_STREAM);
			} catch (final FileNotFoundException | NoSuchFileException ex) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			} catch (final IOException ex) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			
			HttpUtil.setContentLength(response, targetSize);
			
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, targetType.toString());
			
			ctx.write(response);
			
			sendFileFuture = ctx.write(new DefaultFileRegion(target.toFile(), 0, targetSize), ctx.newProgressivePromise());
			
			sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
				@Override
				public void operationProgressed (final ChannelProgressiveFuture future, final long progress, final long total){
					// optional: progress log
				}
				
				@Override
				public void operationComplete (final ChannelProgressiveFuture future){
					final long whenEnd;
					
					whenEnd = System.currentTimeMillis();
					
					System.out.println("@HttpDownloadHandler took %d millisecond(s)".formatted(whenEnd - whenBegin));
				}
			});
			
			// Write last content to mark end of response
			ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		}
	}
	
	private class HttpInsecureRedirectHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			final String       host;
			final String       location;
			final HttpResponse response;
			
				System.out.println("@HttpInsecureRedirectHandler");
			host = request.headers().get(HttpHeaderNames.HOST);
			
			location = ("https://" + host + request.uri());
			
			response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);

			response.headers().set(HttpHeaderNames.LOCATION, location);
			response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
			
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private class HttpWebHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			try{
			final String               host;
			final GWXRoute             route;
			final T                    session;
			final Path                 target;
			final long                 targetSize;
			final GWXSupportedMimeType targetType;
			final long                 whenBegin;
			
			whenBegin = System.currentTimeMillis();
			
			try {
				route = GWXRoute.parseMiniString(URLDecoder.decode(request.uri(), charset), "/", interfaceManager.detectLatestVersion());
			} catch (final GWXInvalidRouteException ex) {
				ex.printStackTrace();
				
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (! request.method().equals(HttpMethod.GET)) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			session = ctx.channel().attr(keySession).get();
			
			if (null == session) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (null != webInterface) {
				final GWXContext                  context;
				final GWXWebInterface.PathHandler handler;
				final GWXInstance                 instance;
				final String                      result;
				
				webInterface.update();
				
				if (! webInterface.available()) {
					final HttpResponse response;
					
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
					
					return;
				}
				
				handler = webInterface.lookupPathHandler(route.path, session.isAuthenticated());
				
				if (null == handler) {
					final HttpResponse response;
					
					if (! webInterface.existsPathHandler(route.path)) {
						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
					} else {
						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
						response.headers().set(HttpHeaderNames.LOCATION, "/");
					}
					
					completeResponse(request, response);
					
					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
					
					return;
				}
				
				instance = new GWXInstance(rcm, 0, session);
				
				context = rcm.buildContext(session, handler.pattern, route.path);
				
				result = handler.call(context, instance);
				
				if (null != result) {
					target = Paths.get(webRoot.getAbsolutePath(), result);
				} else {
					target = null;
				}
			} else {
				target = webRoot.toPath().resolve(route.path.toString()).normalize();
			}
			
			if (null == target) {
				final HttpResponse response;
				
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			try {
				targetType = GWXUtilities.detectMimeType(target, GWXSupportedMimeType.TEXT);
			} catch (final FileNotFoundException | NoSuchFileException ex) {
				final HttpResponse response;
				
				ex.printStackTrace();
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			} catch (final IOException ex) {
				final HttpResponse response;
				
				ex.printStackTrace();
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			host = request.headers().get(HttpHeaderNames.HOST);
			
			//System.out.println("Serving '%s' (%s)".formatted(target.toAbsolutePath(), targetType));
			
			if (targetType == GWXSupportedMimeType.HTML) {
				GWXServiceProvider.this.getExecutor().execute(() -> {
					final HttpResponse response;
					final String       sdata;
					final byte[]       xdata;
					
					try {
						sdata = this.loadDocument(session, target.toFile(), interfaceManager.requireInterface(route.version), host);
					} catch (final FileNotFoundException | NoSuchFileException ex) {
						ex.printStackTrace();

						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

						completeResponse(request, response);

						ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

						return;
					} catch (final IOException ex) {
						ex.printStackTrace();

						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);

						completeResponse(request, response);

						ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

						return;
					}

					xdata = sdata.getBytes(charset);

					response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

					completeResponse(request, response);

					response.headers()
						  .set(HttpHeaderNames.CACHE_CONTROL, "no-cache");

					HttpUtil.setContentLength(response, xdata.length);

					response.headers()
						  .set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(GWXSupportedMimeType.HTML, charset));
					
					ctx.executor().execute(() -> {
						ctx.write(response);
						ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer(xdata)));
						ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener((final ChannelFuture future) -> {
							long whenEnd = System.currentTimeMillis();

							System.out.println("@HttpWebHandler Serving '%s' (%s) took %d millisecond(s)".formatted(target.toAbsolutePath(), targetType, whenEnd - whenBegin));

							future.channel().close(); // close after completion
						});
					});
				});
			} else if ((targetType == GWXSupportedMimeType.CSS) || (targetType == GWXSupportedMimeType.JAVASCRIPT)) {
				GWXServiceProvider.this.getExecutor().execute(() -> {
					final HttpResponse response;
					final String       sdata;
					final byte[]       xdata;
					
					try {
						sdata = this.loadResource(session, target.toFile());
					} catch (final FileNotFoundException | NoSuchFileException ex) {
						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

						ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

						return;
					} catch (final IOException ex) {
						response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);

						ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

						return;
					}

					xdata = sdata.getBytes(charset);

					response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

					HttpUtil.setContentLength(response, xdata.length);

					completeResponse(request, response);

					response.headers()
						  .set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(targetType, charset));
					
					ctx.executor().execute(() -> {
						ctx.write(response);
						ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer(xdata)));
						ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener((final ChannelFuture future) -> {
							long whenEnd = System.currentTimeMillis();

							System.out.println("@HttpWebHandler Serving '%s' (%s) took %d millisecond(s)".formatted(target.toAbsolutePath(), targetType, whenEnd - whenBegin));

							future.channel().close(); // close after completion
						});
					});
				});
			} else {
				final HttpResponse  response;
				final ChannelFuture sendFileFuture;
				
				try {
					targetSize = Files.size(target);
				} catch (final FileNotFoundException | NoSuchFileException ex) {
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

					return;
				} catch (final IOException ex) {
					response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);

					ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

					return;
				}
				
				response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				
				HttpUtil.setContentLength(response, targetSize);
				
				completeResponse(request, response);
				
				response.headers().set(HttpHeaderNames.CONTENT_TYPE, GWXUtilities.formatMimeType(targetType, charset));
				
				ctx.write(response);
				
				sendFileFuture = ctx.write(new DefaultFileRegion(target.toFile(), 0, targetSize), ctx.newProgressivePromise());
				
				sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
					@Override
					public void operationProgressed (final ChannelProgressiveFuture future, final long progress, final long total){
						// optional: progress log
					}
					
					@Override
					public void operationComplete (final ChannelProgressiveFuture future){
						final long whenEnd;
						
						whenEnd = System.currentTimeMillis();
						
						System.out.println("@HttpWebHandler Serving '%s' (%s) took %d millisecond(s)".formatted(target.toAbsolutePath(), targetType, whenEnd - whenBegin));
					}
				});
				
				// Write last content to mark end of response
				ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		private void completeResponse (final HttpRequest request, final HttpResponse response){
			final String origin;
			
			origin = request.headers().get(HttpHeaderNames.ORIGIN);
			
			if (null != origin) {
				response.headers()
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,OPTIONS,POST,PUT")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Origin,Accept,X-Requested-With,Content-Type,Authorization");
			}
		}
		
		private String createClientSDK (final T session, final GWXAPIInterface iface, final String host){
			final GWXWebBridgeBuilder builder;
			final int                 callSuggestPort;
			final String              callURL;
			final StringBuilder       callURLBuilder;
			
			if (port != 0) 
				callSuggestPort = port;
			else {
				callSuggestPort = (useSSL ? PORT_SECURE : PORT_INSECURE);
			}
			
			callURLBuilder = new StringBuilder();
//			callURLBuilder.append(useSSL ? "https" : "http");
//			callURLBuilder.append("://");
//			callURLBuilder.append(host);
//			callURLBuilder.append(":");
//			callURLBuilder.append(callSuggestPort);
			callURLBuilder.append(PATH_BASE_API);
			callURLBuilder.append("/");
			callURLBuilder.append("v");
			callURLBuilder.append((int)iface.version);
//			callURLBuilder.append("/");
			
			// resolves to e.g. 'http://localhost:80/api/v1/'
			callURL = callURLBuilder.toString();
			
			builder = new GWXWebBridgeBuilder(session, iface, callURL);
			
			return builder.build();
		}
		
		private String loadDocument (final T session, final File file, final GWXAPIInterface iface, final String host) throws IOException {
			final String       data;
			final HTMLDocument document;
			HTMLNode           documentHead;
			final HTMLNode     documentRoot;
			
			if (null != webClassRoot) {
				final HTFXParser documentLoader;
				
				documentLoader = new HTFXParser();
				documentLoader.setClassDirectory(webClassRoot.getPath());
				
				if (useCachedRendering) {
					documentLoader.setCache(renderingCache);
				}
				
				if (null != renderingStaticCache) {
					documentLoader.setSource(Source.wrapString(renderingStaticCache.load(file)));
				} else {
					documentLoader.setSource(Source.wrapFile(file));
				}
				
				try {
					document = documentLoader.parse();
				} catch (final HTMLException ex) {
					throw new Error(ex);
				}
			} else {
				try {
					final String datax;
					
					if (null != renderingStaticCache) {
						datax = renderingStaticCache.load(file);
					} else {
						datax = Files.readString(file.toPath());
					}
					
					document = HTMLParser.parseString(datax);
				} catch (final HTMLException ex) {
					throw new Error(ex);
				}
				
				document.removeSpecialElements();
				document.removeComments();
			}
			
			documentRoot = document.lookupFirst("html");
			
			documentHead = document.lookupFirst(HTMLStandardType.HEAD, -1);
			
			if ((null == documentHead) && (null != documentRoot)) {
				documentHead = new HTMLNode(HTMLStandardType.HEAD);
				documentRoot.getChildren().add(documentHead);
			}
			
			if ((enableActions || enableEvents) && (null != documentHead)) {
				documentHead.addChild(HTMLUtilities.createScript(createClientSDK(session, iface, host)));
			}
			
			document.removeSpecialElements();
			document.removeComments();
			
			data = document.toHTMLString();
//			data = document.toHTMLStringHumanUnreadable();
			
			return GWXImporter.importString(data, session.namespace);
		}
		
		private String loadResource (final T session, final File file) throws IOException {
			final String datax;
			
			if (null != renderingStaticCache) {
				datax = renderingStaticCache.load(file);
			} else {
				datax = Files.readString(file.toPath());
			}
			
			return GWXImporter.importString(datax, session.namespace);
		}
	}
	
	private class SessionLifecycleHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelInactive (final ChannelHandlerContext ctx) throws Exception {
			final GWXEventListener            listener;
			final Attribute<GWXEventListener> listenerAttrib;
			final T                           session;
			final Attribute<T>                sessionAttrib;
			
			sessionAttrib = ctx.channel().attr(keySession);
			
			if (null != sessionAttrib) {
				session = sessionAttrib.get();
				
				if (null != session) {
					listenerAttrib = ctx.channel().attr(keyListener);
					
					if (null != listenerAttrib) {
						listener = listenerAttrib.get();
					} else {
						listener = null;
					}
					
					if (null != listener) {
						session.detachEventListener(listener);
					} else if (!session.isAuthenticated()) {
						destroyInstance(session);
					}
					
					if (! session.isEventListenerPresent()) {
						dispatchIdleCountdown(session);
					}
				}
			}
			
			super.channelInactive(ctx);
		}
	}
	
	private class SseEventHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			try{
			final GWXAPIInterface.Handle interfaceHandle;
			final GWXEventListener       listener;
			final HttpResponse           response;
			final GWXRoute               route;
			final T                      session;
			final long                   whenBegin;
			final long                   whenEnd;
			
			whenBegin = System.currentTimeMillis();
			
			if (! GWXSupportedMimeType.SSE.toString().equals(request.headers().get(HttpHeaderNames.ACCEPT))) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			try {
				route = GWXRoute.parseFullString(URLDecoder.decode(request.uri(), charset), ".");
			} catch (final GWXInvalidRouteException ex) {
				ex.printStackTrace();
				
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			session = ctx.channel().attr(keySession).get();
			
			if (null == session) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (! session.checkAccess(route.path.withoutExtension(), GWXPermission.OBSERVE)) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			interfaceHandle = session.getInterface(route.version, () -> {
				return interfaceManager.requireInterface(route.version);
			});
			
			if (null == interfaceHandle) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				completeResponse(request, response);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			listener = new GWXEventListener(rcm, ctx.channel(), GWXEventListener.Type.SSE, interfaceHandle, route.path);
			listener.attach(session);
			
			ctx.channel().attr(keyListener).set(listener);
			
			response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			
			completeResponse(request, response);
			
			response.headers()
			        .set(HttpHeaderNames.CONTENT_TYPE, GWXSupportedMimeType.SSE.toString())
			        .set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
			        .set(HttpHeaderNames.CONNECTION, "keep-alive");
			
			ctx.write(response);
			
			listener.ping();
			
			session.attachEventListener(listener);
			
			System.out.println("Created SSE Event Handler");
			
			whenEnd = System.currentTimeMillis();
			
			System.out.println("@SseEventHandler took %d millisecond(s)".formatted(whenEnd - whenBegin));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		}
		
		private void completeResponse (final HttpRequest request, final HttpResponse response){
			final String origin;
			
			origin = request.headers().get(HttpHeaderNames.ORIGIN);
			
			if (null != origin) {
				response.headers()
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
					  .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,OPTIONS,POST,PUT")
				        .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Origin,Accept,X-Requested-With,Content-Type,Authorization");
			}
		}
	}
	
	private class WebSocketForceInsecureUpgradeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			final HttpResponse response;
			
				System.out.println("@WebSocketForceInsecureUpgradeHandler");
			if (! "websocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE))) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UPGRADE_REQUIRED);
			response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
			
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private class WebSocketKeepAliveHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
		protected void channelRead0 (final ChannelHandlerContext ctx, final WebSocketFrame frame) {
				System.out.println("@WebSocketKeepAliveHandler");
			if (frame instanceof PongWebSocketFrame) {
				// pong received, can log or ignore
			} else {
				// pass through other frames
				ctx.fireChannelRead(frame.retain());
			}
		}
		
		@Override
		public void exceptionCaught (final ChannelHandlerContext ctx, final Throwable cause){
		    cause.printStackTrace();
		    
		    ctx.close();
		}
	}
	
	private class WebSocketUpgradeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
		@Override
		protected void channelRead0 (final ChannelHandlerContext ctx, final FullHttpRequest request){
			final GWXAPIInterface.Handle           interfaceHandle;
			final GWXEventListener                 listener;
			final HttpResponse                     response;
			final GWXRoute                         route;
			final T                                session;
			final long                             whenBegin;
			final long                             whenEnd;
			final WebSocketServerHandshakerFactory wsFactory;
			final WebSocketServerHandshaker        wsHandshaker;
			final String                           wsURL;
			
			whenBegin = System.currentTimeMillis();
			
			if (! "websocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE))) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			try {
				route = GWXRoute.parseFullString(URLDecoder.decode(request.uri(), charset), ".");
			} catch (final GWXInvalidRouteException ex) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			session = ctx.channel().attr(keySession).get();
			
			if (null == session) {
				ctx.fireChannelRead(request.retain());
				
				return;
			}
			
			if (! session.checkAccess(route.path.withoutExtension(), GWXPermission.OBSERVE)) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			interfaceHandle = session.getInterface(route.version, () -> {
				return interfaceManager.requireInterface(route.version);
			});
			
			if (null == interfaceHandle) {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
				
				return;
			}
			
			listener = new GWXEventListener(rcm, ctx.channel(), GWXEventListener.Type.WS, interfaceHandle, route.path);
			listener.attach(session);
			
			ctx.channel().attr(keyListener).set(listener);
			
			wsURL = request.uri(); //"%s://0.0.0.0:%d/v%d/".formatted(wsProtocol, port, route.version);
			
			wsFactory = new WebSocketServerHandshakerFactory(wsURL, null, true);
			wsHandshaker = wsFactory.newHandshaker(request);
			
			if (wsHandshaker == null) {
				WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				wsHandshaker.handshake(ctx.channel(), request);
			}
			
			session.attachEventListener(listener);
			
			listener.ping();
			
			whenEnd = System.currentTimeMillis();
			
			System.out.println("@WebSocketUpgradeHandler took %d millisecond(s)".formatted(whenEnd - whenBegin));
		}
	}
}