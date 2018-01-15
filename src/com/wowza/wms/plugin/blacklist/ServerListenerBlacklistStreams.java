/*
 * This code and all components (c) Copyright 2006 - 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.plugin.blacklist;

import com.wowza.util.StringUtils;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.IApplicationInstanceNotify;
import com.wowza.wms.application.IApplicationNotify;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.module.ModuleCore; 
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.rtp.model.RTPRequestStatus;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.rtp.model.RTPStream;
import com.wowza.wms.server.IServer;
import com.wowza.wms.server.IServerNotify2;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify;
import com.wowza.wms.stream.IMediaStreamActionNotify2;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider2;
import com.wowza.wms.stream.IMediaStreamNotify;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.util.MediaCasterUtils;
import com.wowza.wms.vhost.IVHost;
import com.wowza.wms.vhost.IVHostNotify;
import com.wowza.wms.vhost.VHostSingleton;

public class ServerListenerBlacklistStreams extends ModuleBase implements IServerNotify2
{ 
	private class VHostNotifier implements IVHostNotify
	{
		private final ApplicationNotify listener = new ApplicationNotify();

		@Override
		public void onVHostCreate(IVHost vhost)
		{
			vhost.addApplicationListener(listener);
		}

		@Override
		public void onVHostInit(IVHost vhost)
		{
		}

		@Override
		public void onVHostShutdownStart(IVHost vhost)
		{
		}

		@Override
		public void onVHostShutdownComplete(IVHost vhost)
		{
			vhost.removeApplicationListener(listener);
		}

		@Override
		public void onVHostClientConnect(IVHost vhost, IClient inClient, RequestFunction function, AMFDataList params)
		{
		}

	}

	class ApplicationNotify implements IApplicationNotify
	{
		private final ApplicationInstanceNotify listener = new ApplicationInstanceNotify();

		public void onApplicationCreate(IApplication application)
		{
			application.addApplicationInstanceListener(listener);
		}

		public void onApplicationDestroy(IApplication application)
		{
			application.removeApplicationInstanceListener(listener);
		}
	}

	class ApplicationInstanceNotify implements IApplicationInstanceNotify
	{
		private final StreamListener listener = new StreamListener();

		public void onApplicationInstanceCreate(IApplicationInstance appInstance)
		{
			if (ServerListenerBlacklistStreams.debug)
			{
				logger.info(MODULE_NAME + ".onApplicationInstanceCreate[" + appInstance.getName() + "] Stream Listener is initiated");
			}
			
			IMediaStreamNameAliasProvider currentAliasProvider = appInstance.getStreamNameAliasProvider();
			appInstance.setStreamNameAliasProvider(new StreamAliasProvider(currentAliasProvider));
			appInstance.addMediaStreamListener(listener);
		}

		public void onApplicationInstanceDestroy(IApplicationInstance appInstance)
		{
			if (ServerListenerBlacklistStreams.debug)
			{
				logger.info(MODULE_NAME + ".onApplicationInstanceDestroy[" + appInstance.getName() + "] Stream listener is removed");
			}
			appInstance.removeMediaStreamListener(listener);
		}
	}

	class StreamListener implements IMediaStreamNotify
	{
		private final IMediaStreamActionNotify actionNotify = new StreamManager();

		@Override
		public void onMediaStreamCreate(IMediaStream stream)
		{

			if (ServerListenerBlacklistStreams.debug)
			{
				logger.info(MODULE_NAME + ".onApplicationInstanceCreate[" + stream.getName() + "] Stream is initiated ");
			}
			stream.addClientListener(actionNotify);
		}

		@Override
		public void onMediaStreamDestroy(IMediaStream stream)
		{
			if (actionNotify != null)
			{
				stream.removeClientListener(actionNotify);
			}
		}
	}

	class StreamManager implements IMediaStreamActionNotify2
	{

		/*
		 * (non-Javadoc)
		 * This will handle the publish event and determine if stream is blacklisted.
		 */
		@Override
		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			IApplicationInstance appInstance = stream.getStreams().getAppInstance();
			
			// check if the stream being published has been generated locally. They can be controlled elsewhere.
			// MediaCaster
			if(appInstance.getMediaCasterStreams().getMediaCaster(streamName) != null)
				return;
			// transcoder output
			if(stream.isTranscodeResult())
				return;
			// Publisher API
			if(stream.isPublisherStream())
				return;

			if (ServerListenerBlacklistStreams.debug)
			{
				logger.info(MODULE_NAME + ".onApplicationInstanceCreate[" + streamName + "] Checking stream for blacklist");
			}
			
			String appName = appInstance.getApplication().getName();
			String appInstName = appInstance.getName();
			
			if(!BlackListUtils.isStreamBlackListed(appName, appInstName, streamName))
				return;
			
			if (stream.getClient() != null)
			{
					sendStreamOnStatusError(stream, "NetStream.Publish.BadName", "The publisher's Stream was not white listed");
					stream.getClient().setShutdownClient(true);

					logger.info(MODULE_NAME + ".onPublish[" + streamName + "] Client Rejected (NetStream.Publish.BadName), black listed " + appName + "/" + appInstName + "/" + streamName);
			}
			else if(stream.getRTPStream() != null)
			{
				RTPSession session = stream.getRTPStream().getSession();
				if(session != null)
				{
					appInstance.getVHost().getRTPContext().shutdownRTPSession(session);
					logger.info(MODULE_NAME + ".onPublish[" + streamName + "] RTP Rejected, black listed. Stream: " + appName + "/" + appInstName + "/" + streamName);
				}
			}
		}

		@Override
		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset)
		{
		}

		@Override
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
		}

		@Override
		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
		}

		@Override
		public void onSeek(IMediaStream stream, double location)
		{
		}

		@Override
		public void onStop(IMediaStream stream)
		{
		}

		@Override
		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket)
		{
		}

		@Override
		public void onPauseRaw(IMediaStream stream, boolean isPause, double location)
		{
		}
	}

	class StreamAliasProvider implements IMediaStreamNameAliasProvider2
	{

		private final IMediaStreamNameAliasProvider currentAliasProvider;

		public StreamAliasProvider(IMediaStreamNameAliasProvider currentAliasProvider)
		{
			this.currentAliasProvider = currentAliasProvider;
		}

		@Override
		public String resolvePlayAlias(IApplicationInstance appInstance, String name)
		{
			if (BlackListUtils.isStreamBlackListed(appInstance.getApplication().getName(), appInstance.getName(), name))
				return null;
			return name;
		}

		@Override
		public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client)
		{
			if(currentAliasProvider != null)
			{
				if(currentAliasProvider instanceof IMediaStreamNameAliasProvider2)
					name = ((IMediaStreamNameAliasProvider2)currentAliasProvider).resolvePlayAlias(appInstance, name, client);
				name = currentAliasProvider.resolveStreamAlias(appInstance, name);
			}

			if (name == null)
				return null;

			if (BlackListUtils.isStreamBlackListed(appInstance.getApplication().getName(), appInstance.getName(), name))
				return null;
			return name;
		}

		@Override
		public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession)
		{
			if(currentAliasProvider != null)
			{
				if(currentAliasProvider instanceof IMediaStreamNameAliasProvider2)
					name = ((IMediaStreamNameAliasProvider2)currentAliasProvider).resolvePlayAlias(appInstance, name, httpSession);
				name = currentAliasProvider.resolveStreamAlias(appInstance, name);
			}

			if (name == null)
				return null;

			if (BlackListUtils.isStreamBlackListed(appInstance.getApplication().getName(), appInstance.getName(), name))
				return null;
			return name;
		}

		@Override
		public String resolvePlayAlias(IApplicationInstance appInstance, String name, RTPSession rtpSession)
		{
			if(currentAliasProvider != null)
			{
				if(currentAliasProvider instanceof IMediaStreamNameAliasProvider2)
					name = ((IMediaStreamNameAliasProvider2)currentAliasProvider).resolvePlayAlias(appInstance, name, rtpSession);
				name = currentAliasProvider.resolveStreamAlias(appInstance, name);
			}

			if (name == null)
				return null;

			if (BlackListUtils.isStreamBlackListed(appInstance.getApplication().getName(), appInstance.getName(), name))
				return null;
			return name;
		}

		@Override
		public String resolvePlayAlias(IApplicationInstance appInstance, String name, ILiveStreamPacketizer liveStreamPacketizer)
		{
			if(currentAliasProvider != null)
			{
				if(currentAliasProvider instanceof IMediaStreamNameAliasProvider2)
					name = ((IMediaStreamNameAliasProvider2)currentAliasProvider).resolvePlayAlias(appInstance, name, liveStreamPacketizer);
				name = currentAliasProvider.resolveStreamAlias(appInstance, name);
			}

			if (name == null)
				return null;

			if (BlackListUtils.isStreamBlackListed(appInstance.getApplication().getName(), appInstance.getName(), name))
				return null;
			return name;
		}

		@Override
		public String resolveStreamAlias(IApplicationInstance appInstance, String name)
		{
			if(currentAliasProvider != null)
				return currentAliasProvider.resolveStreamAlias(appInstance, name);
			return name;
		}

		@Override
		public String resolveStreamAlias(IApplicationInstance appInstance, String name, IMediaCaster mediaCaster)
		{
			if(currentAliasProvider != null)
			{
				if(currentAliasProvider instanceof IMediaStreamNameAliasProvider2)
					return ((IMediaStreamNameAliasProvider2)currentAliasProvider).resolveStreamAlias(appInstance, name, mediaCaster);
				return currentAliasProvider.resolveStreamAlias(appInstance, name);
			}
			return name;
		}
	}

	public static final String MODULE_NAME = "ServerListenerBlacklistStreams";
	private static final String PROP_NAME_PREFIX = "blacklistStreams";

	public static boolean debug = false;

	private WMSLogger logger = WMSLoggerFactory.getLogger(getClass());
	private String configPath = null;

	@Override
	public void onServerConfigLoaded(IServer server)
	{
		VHostSingleton.addVHostListener(new VHostNotifier());
	}

	@Override
	public void onServerCreate(IServer server)
	{
		debug = server.getProperties().getPropertyBoolean(PROP_NAME_PREFIX + "DebugLog", debug);
		if (logger.isDebugEnabled())
			debug = true;

		this.configPath = server.getProperties().getPropertyStr(ServerListenerBlacklistStreams.PROP_NAME_PREFIX + "ConfigPath", this.configPath);
		if (!StringUtils.isEmpty(this.configPath))
			BlackListUtils.setConfigPath(this.configPath);
		String separatorChar = server.getProperties().getPropertyStr(ServerListenerBlacklistStreams.PROP_NAME_PREFIX + "SeparatorChar");
		if (!StringUtils.isEmpty(separatorChar))
			BlackListUtils.setSeparatorChar(separatorChar);
	}

	@Override
	public void onServerInit(IServer server)
	{
	}

	@Override
	public void onServerShutdownComplete(IServer server)
	{
	}

	@Override
	public void onServerShutdownStart(IServer server)
	{
	}
}
