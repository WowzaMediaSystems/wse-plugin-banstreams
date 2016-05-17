/*
 * This code and all components (c) Copyright 2006 - 2016, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.plugin.blacklist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.wowza.wms.bootstrap.Bootstrap;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory; 

public class BlackListUtils
{
	private static ArrayList<String> streamBlackList = new ArrayList<String>();
	private static String blacklistConfigPath = Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/conf/blacklist.txt";
	private static WMSLogger logger = WMSLoggerFactory.getLogger(BlackListUtils.class);

	public static void setConfigPath(String config)
	{
		BlackListUtils.blacklistConfigPath = config;
	}

	public static void blackListStream(String application, String appInstance, String streamName)
	{
		BlackListUtils.mergeData();

		synchronized(BlackListUtils.streamBlackList)
		{
			String streamKey = application + ":" + appInstance + ":" + streamName;
			if (!BlackListUtils.streamBlackList.contains(streamKey))
			{
				BlackListUtils.streamBlackList.add(streamKey);
				BlackListUtils.printContents();
				BlackListUtils.saveFile();
			}
		}
	}

	public static void removeStreamFromList(String application, String appInstance, String streamName)
	{
		BlackListUtils.mergeData();
		String streamKey = application + ":" + appInstance + ":" + streamName;
		if (ServerListenerBlacklistStreams.debug)
			logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Stream is initiated ");

		synchronized(BlackListUtils.streamBlackList)
		{
			if (BlackListUtils.streamBlackList.contains(streamKey))
			{
				if (BlackListUtils.streamBlackList.remove(streamKey))
				{
					BlackListUtils.saveFile();
					BlackListUtils.printContents();

					logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Completed ");
				}
				else
				{
					logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Does not exist-2 ");
				}
			}
			else
			{
				logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Does not exist ");
			}
		}
	}

	public static boolean isStreamBlackListed(String application, String appInstance, String streamName)
	{
		BlackListUtils.mergeData();
		String streamKey = application + ":" + appInstance + ":" + streamName;
		return BlackListUtils.streamBlackList.contains(streamKey);
	}

	public static ArrayList<String> getBlackListedStreams()
	{
		BlackListUtils.mergeData();
		synchronized(BlackListUtils.streamBlackList)
		{
			return BlackListUtils.streamBlackList;
		}
	}

	public static void mergeData()
	{
		synchronized(BlackListUtils.streamBlackList)
		{
			ArrayList<String> fileStored = BlackListUtils.getStoredBlacklistItems();
			if (fileStored.size() > 0)
			{
				for (int i = 0; i < fileStored.size(); i++)
				{
					if (!BlackListUtils.streamBlackList.contains(fileStored.get(i)))
					{
						BlackListUtils.streamBlackList.add(fileStored.get(i));
					}
				}
			}
		}
	}

	private static ArrayList<String> getStoredBlacklistItems()
	{
		ArrayList<String> blackList = new ArrayList<String>();
		File tmpFile = new File(BlackListUtils.blacklistConfigPath);
		if (tmpFile.exists())
		{
			FileInputStream in = null;
			BufferedReader br = null;
			try
			{
				in = new FileInputStream(BlackListUtils.blacklistConfigPath);
				br = new BufferedReader(new InputStreamReader(in));
				String cacheItem;

				while ((cacheItem = br.readLine()) != null)
				{
					cacheItem = cacheItem.trim();
					if (!cacheItem.startsWith("#") && cacheItem.length() > 0)
					{
						blackList.add(cacheItem);
					}
				}
			}
			catch (Exception e)
			{
				logger.error(ServerListenerBlacklistStreams.MODULE_NAME + ".getStoredBlacklistItems() ", e);
			}
			finally
			{
				try
				{
					if (br != null)
						br.close();
				}
				catch (IOException e)
				{
				}
				br = null;
				try
				{
					if (in != null)
						in.close();
				}
				catch (IOException e)
				{
				}
				in = null;
			}
		}
		else
		{
			logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".getStoredBlacklistItems could not find black list items list");
		}
		return blackList;
	}

	private static void saveFile()
	{
		synchronized(BlackListUtils.streamBlackList)
		{
			String blackListTargetPath = Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/conf/blacklist.txt";

			ArrayList<String> bli = BlackListUtils.streamBlackList;
			try
			{
				File f = new File(blackListTargetPath);
				if (!f.exists())
				{
					f.createNewFile();
				}

				FileWriter tmpFile = new FileWriter(blackListTargetPath, false);
				StringBuilder b = new StringBuilder();
				for (int i = 0; i < bli.size(); i++)
				{
					logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".saveFile " + "Line: " + bli.get(i));
					b.append(bli.get(i) + "\n");
				}
				tmpFile.write(b.toString());
				tmpFile.close();
			}
			catch (IOException ioe)
			{
				logger.error(ServerListenerBlacklistStreams.MODULE_NAME + ".saveFile " + "IOException: " + ioe.getMessage(), ioe);
			}
		}
	}

	private static void printContents()
	{
		ArrayList<String> bli = BlackListUtils.getBlackListedStreams();
		for (int i = 0; i < bli.size(); i++)
		{
			logger.info(ServerListenerBlacklistStreams.MODULE_NAME + ".printContents " + "bli.get(i): " + bli.get(i));
		}

	}

}
