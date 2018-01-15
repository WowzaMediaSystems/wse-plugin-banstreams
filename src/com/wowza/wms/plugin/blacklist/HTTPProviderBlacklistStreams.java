/*
 * This code and all components (c) Copyright 2006 - 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.plugin.blacklist;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.http.HTTProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory; 
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.rtp.model.RTPStream;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;

public class HTTPProviderBlacklistStreams extends HTTProvider2Base
{
	public static final String MODULE_NAME = "HTTPProviderBlacklistStreams";

	private WMSLogger logger = WMSLoggerFactory.getLogger(getClass());

	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp)
	{
		if (!doHTTPAuthentication(vhost, req, resp))
			return;

		String postResponse = this.handlePost(req, vhost);

		String html = "";

		// get blacklisted streams
		html += this.getBlackListedStreams();

		// get published streams
		html += this.getPublishedStreams(vhost);

		//refresh button
		html += this.getRefresh();

		String postMsg = "";
		if (postResponse != null)
		{
			postMsg = this.getMsgHeader(postResponse, req);
		}
		String retStr = this.getHtmlPage(html, postMsg);
		try
		{
			OutputStream out = resp.getOutputStream();
			byte[] outBytes = retStr.getBytes();
			out.write(outBytes);
		}
		catch (Exception e)
		{
			logger.error(MODULE_NAME + ".onHTTPRequest()", e);
		}
	}

	private String getRefresh()
	{
//		String html = "<script type='text/javascript'>setTimeout(\"location.reload(true);\",30);</script>";
		String html = "<br /><br /><input type='button' name='refresh' value='Check for New Streams' onclick=\"location.reload(true);\" />";
		return html;
	}

	private String getBlackListedStreams()
	{
		String html = "";
		ArrayList<String> streams = BlackListUtils.getBlackListedStreams();
		if (streams.size() > 0)
		{

			html += this.divHeader("Blacklisted") + this.divSectionStart();
			html += "<table>";
			for (int i = 0; i < streams.size(); i++)
			{
				String[] parts = streams.get(i).split(":");
				if (parts.length == 3)
				{
					String applicationName = parts[0];
					String appInstanceName = parts[1];
					String streamName = parts[2];
					html += this.addRow(applicationName, appInstanceName, streamName);
				}
			}
			html += "</table>";
			html += this.divSectionEnd();
		}

		return html;
	}

	private String getPublishedStreams(IVHost vhost)
	{
		String html = "";
		if (vhost != null)
		{
			html += this.divHeader("Published Streams");
			html += this.divSectionStart();
			html += this.startSection(vhost.getName());
			List<String> appNames = vhost.getApplicationNames();
			if (appNames.size() > 0)
			{
				Iterator<String> appNameIterator = appNames.iterator();
				while (appNameIterator.hasNext())
				{
					String applicationName = appNameIterator.next();
					IApplication application = vhost.getApplication(applicationName);

					List<String> appInstances = application.getAppInstanceNames();
					if (appInstances.size() > 0)
					{
						Iterator<String> iterAppInstances = appInstances.iterator();
						while (iterAppInstances.hasNext())
						{
							String appInstanceName = iterAppInstances.next();
							IApplicationInstance appInstance = application.getAppInstance(appInstanceName);

							List<String> publishedStreams = appInstance.getPublishStreamNames();
							if (publishedStreams.size() > 0)
							{
								Iterator<String> publishedStreamIterator = publishedStreams.iterator();
								while (publishedStreamIterator.hasNext())
								{
									String streamName = publishedStreamIterator.next();
									if(!BlackListUtils.isStreamBlackListed(applicationName, appInstanceName, streamName)){
										html += this.addRow(applicationName, appInstanceName, streamName);
									}
								}
							}
						}
					}
				}
			}
			else
			{
				html += this.addEmptyRow("No streams published..");
			}
			html += this.endSection();
			html += this.divSectionEnd();
		}
		return html;
	}

	private boolean blacklistStream(IVHost vhost, String applicationName, String appInstanceName, String streamName)
	{
		try
		{
			IApplicationInstance appInstance = vhost.getApplication(applicationName).getAppInstance(appInstanceName);
			if (appInstance != null)
			{

				// force client disconnect
				List<IClient> rtmpClients = appInstance.getClients();
				List<IHTTPStreamerSession> httpClients = appInstance.getHTTPStreamerSessions();
				List<RTPSession> rtpClients = appInstance.getRTPSessions();

				// http
				Iterator<IHTTPStreamerSession> httpClientIter = httpClients.iterator();
				IHTTPStreamerSession httpClient;
				while (httpClientIter.hasNext())
				{
					try
					{
						httpClient = httpClientIter.next();
						if (httpClient == null)
							continue;

						if (streamName.equals(httpClient.getStream().getName()))
						{
							httpClient.rejectSession();
						}
					}
					catch (Exception e)
					{
						logger.error(MODULE_NAME + ".blacklistStream()", e);
					}

				}

				//rtmp
				Iterator<IClient> rtmpClientIter = rtmpClients.iterator();
				IClient rtmpClient;
				while (rtmpClientIter.hasNext())
				{
					try
					{
						rtmpClient = rtmpClientIter.next();
						if (rtmpClient == null)
							continue;

						List<IMediaStream> Names = rtmpClient.getPlayStreams();

						if (Names.size() > 0)
						{
							Iterator<IMediaStream> iter = Names.iterator();
							while (iter.hasNext())
							{
								IMediaStream stream = (IMediaStream)iter.next();

								if (streamName.equals(stream.getName()))
								{
									rtmpClient.setShutdownClient(true);
								}
							}
						}
					}
					catch (Exception e)
					{
						logger.error(MODULE_NAME + ".blacklistStream()", e);
					}
				}

				//rtp
				Iterator<RTPSession> rtpClientIter = rtpClients.iterator();
				RTPSession rtpClient;
				while (rtpClientIter.hasNext())
				{
					try
					{
						rtpClient = rtpClientIter.next();
						if (rtpClient == null)
							continue;

						if (streamName.equals(rtpClient.getRTSPStream().getStreamName()))
						{
							appInstance.getVHost().getRTPContext().shutdownRTPSession(rtpClient);
						}
					}
					catch (Exception e)
					{
						logger.error(MODULE_NAME + ".blacklistStream()", e);
					}
				}
			}

			//shutdown stream
			IMediaStream publishedStream = appInstance.getStreams().getStream(streamName);
			if(publishedStream != null)
			{
				IClient client = publishedStream.getClient();
				if (client != null)
				{
					client.setShutdownClient(true);
				}
				else
				{
					RTPStream rtpStream = publishedStream.getRTPStream();
					if (rtpStream != null)
					{
						appInstance.getVHost().getRTPContext().shutdownRTPSession(rtpStream.getSession());
					}
				}
			}
			return true;
		}
		catch (Exception ex)
		{
			logger.error(MODULE_NAME + ".blacklistStream()", ex);
		}
		return false;
	}

	private String handlePost(IHTTPRequest req, IVHost vhost)
	{
		if (req.getMethod().equalsIgnoreCase("post"))
		{
			req.parseBodyForParams(true);
		}

		Map<String, List<String>> params = req.getParameterMap();
		System.out.println(params.toString());
		if (params.containsKey("blacklist"))
		{
			String appPath = params.get("application").get(0) + "/" + params.get("appInstance").get(0) + "/" + params.get("stream").get(0);
			if (params.get("blacklist").get(0).equalsIgnoreCase("1"))
			{
				BlackListUtils.blackListStream(params.get("application").get(0), params.get("appInstance").get(0), params.get("stream").get(0));
				if (this.blacklistStream(vhost, params.get("application").get(0), params.get("appInstance").get(0), params.get("stream").get(0)))
				{
//					BlackListed.removeStreamFromList(params.get("application").get(0), params.get("appInstance").get(0), params.get("stream").get(0));
					return appPath + " has been added to the blacklist";
				}
				return appPath + " failed to add to blacklist.";
			}
			else
			{
				BlackListUtils.removeStreamFromList(params.get("application").get(0), params.get("appInstance").get(0), params.get("stream").get(0));
				return appPath + " has been removed from the blacklist";
			}
		}
		return null;
	}

	/*
	 * HTML Helpers
	 */

	private String divSectionStart()
	{
		return "<div style='padding: 10px 10px 10px 10px; width: 500px; border: 1px solid #ccc; background-color: #eee; border-radius: 8px;'>";
	}

	private String divSectionEnd()
	{
		return "</div>";
	}

	private String divHeader(String title)
	{
		return "<div style='clear:both; width: 500px;font-weight:bold; border-radius: 8px; background-color: #66CCFF; border: 1px solid #ccc;padding: 10px 10px 10px 10px; margin-top:10px;  margin-bottom: 5px'>" + title + "</div>";
	}

	private String getMsgHeader(String msg, IHTTPRequest req)
	{
		if(!msg.isEmpty()){
			String requestURL = req.getRequestURL();
			String[] parts = requestURL.split("\\/");
			String filterName = parts[parts.length-1];
			msg += " <a href='/"+filterName+"'>clear</a>";
		}
		return "<div style='clear:both; background-color: #FFFFCC; border: 1px solid #ccc;padding: 10px 10px 10px 10px'>" + msg + "</div>";
	}

	private String getHtmlPage(String body, String msg)
	{
		return "<html>\n" + "<head>\n" + "<title>Blacklist Streams</title>\n" + "<script type='text/javascript'>\n" + "function formSubmit(blacklist, application, appInstance, stream)\n" + "{\n" + " document.forms[0].blacklist.value = blacklist;\n" + "				  document.forms[0].application.value = application;\n"
				+ "				  document.forms[0].appInstance.value = appInstance;\n" + "				  document.forms[0].stream.value = stream;\n" + "				  document.forms[0].submit();\n" + "				}\n" + "				</script>\n" + "			</head>\n"
				+ "			<body style='font-family: verdana;'>\n<h2 style='margin-left: 50px;'>Stream Blacklists</h2>" + "				<div style='margin-left: 50px'>" + "				" + msg + "				" + body + " " + "				<form name='blacklistform' method='post'> " + "					<input type='hidden' name='blacklist' value='' /> \n"
				+ "					<input type='hidden' name='application' value='' /> " + "					<input type='hidden' name='appInstance' value='' /> " + "					<input type='hidden' name='stream' value='' /> " + "				</form> " + "				</div>" + "			</body>" + "</html>";
	}

	private String startSection(String vhost)
	{
		return "<div style='font-size: 16px;font-weight:bold; padding: 10px 10px 10px 10px;border: 1px solid #585858; background-color: #ccc;width: 150px; text-align: center;'>" + vhost + "</div><table style='margin-top: 10px;'><tr style='padding: 10px 10px 10px 10px; background-color: #ccc;'>"
				+ "	<td style='padding: 10px 10px 10px 10px;'>Application</td>" + "	<td style='padding: 10px 10px 10px 10px;'>AppInstance</td>" + "	<td style='padding: 10px 10px 10px 10px;'>Stream</td>" + "	<td style='padding: 10px 10px 10px 10px;'>Action</td>" + "</tr>";
	}

	private String endSection()
	{
		return "</table>";
	}

	private String addEmptyRow(String msg)
	{
		return "<tr><td colspan='4'><i>" + msg + "</i></td></tr>";
	}

	private String addRow(String appName, String appInstance, String streamName)
	{

		String blacklist = "1";
		String blacklistedTitle = "Blacklist";
		if (BlackListUtils.isStreamBlackListed(appName, appInstance, streamName))
		{
			blacklistedTitle = "Whitelist";
			blacklist = "0";
		}
		return "<tr  onmouseover=\"this.bgColor='#FFFFCC'\" onmouseout=\"this.bgColor='#EEE'\" >" + "	<td style='width: 120px;padding: 10px 10px 10px 10px;'>" + appName + "</td>" + "	<td style='width: 120px;padding: 10px 10px 10px 10px;'>" + appInstance + "</td>"
				+ "	<td style='width: 120px;padding: 10px 10px 10px 10px;'>" + streamName + "</td>" + "	<td><a style='padding: 10px 10px 10px 10px; cursor:pointer; color: blue;' onclick=\"formSubmit('" + blacklist + "','" + appName + "','" + appInstance + "','" + streamName + "');\">" + blacklistedTitle
				+ "</a> </td>" + "</tr>";
	}
}
