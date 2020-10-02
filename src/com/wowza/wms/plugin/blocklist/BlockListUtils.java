/*
 * This code and all components (c) Copyright 2006 - 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */
package com.wowza.wms.plugin.blocklist;

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

public class BlockListUtils
{
	private static ArrayList<String> streamblockList = new ArrayList<String>();
	private static String blocklistConfigPath = Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/conf/blocklist.txt";
	static String separatorChar = ":";
	private static WMSLogger logger = WMSLoggerFactory.getLogger(BlockListUtils.class);

	public static void setConfigPath(String config)
	{
		BlockListUtils.blocklistConfigPath = config;
	}

	public static void setSeparatorChar(String separatorChar)
	{
		BlockListUtils.separatorChar = separatorChar;
	}

	public static void blockListStream(String application, String appInstance, String streamName)
	{
		BlockListUtils.mergeData();

		synchronized(BlockListUtils.streamblockList)
		{
			String streamKey = application + separatorChar + appInstance + separatorChar + streamName;
			if (!BlockListUtils.streamblockList.contains(streamKey))
			{
				BlockListUtils.streamblockList.add(streamKey);
				BlockListUtils.printContents();
				BlockListUtils.saveFile();
			}
		}
	}

	public static void removeStreamFromList(String application, String appInstance, String streamName)
	{
		BlockListUtils.mergeData();
		String streamKey = application + separatorChar + appInstance + separatorChar + streamName;
		if (ServerListenerBlocklistStreams.debug)
			logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Stream is initiated ");

		synchronized(BlockListUtils.streamblockList)
		{
			if (BlockListUtils.streamblockList.contains(streamKey))
			{
				if (BlockListUtils.streamblockList.remove(streamKey))
				{
					BlockListUtils.saveFile();
					BlockListUtils.printContents();

					logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Completed ");
				}
				else
				{
					logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Does not exist-2 ");
				}
			}
			else
			{
				logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".removeStreamFromList[" + streamKey + "] Does not exist ");
			}
		}
	}

	public static boolean isStreamblockListed(String application, String appInstance, String streamName)
	{
		BlockListUtils.mergeData();
		String streamKey = application + separatorChar + appInstance + separatorChar + streamName;
		return BlockListUtils.streamblockList.contains(streamKey);
	}

	public static ArrayList<String> getblockListedStreams()
	{
		BlockListUtils.mergeData();
		synchronized(BlockListUtils.streamblockList)
		{
			return BlockListUtils.streamblockList;
		}
	}

	public static void mergeData()
	{
		synchronized(BlockListUtils.streamblockList)
		{
			ArrayList<String> fileStored = BlockListUtils.getStoredblocklistItems();
			if (fileStored.size() > 0)
			{
				for (int i = 0; i < fileStored.size(); i++)
				{
					if (!BlockListUtils.streamblockList.contains(fileStored.get(i)))
					{
						BlockListUtils.streamblockList.add(fileStored.get(i));
					}
				}
			}
		}
	}

	private static ArrayList<String> getStoredblocklistItems()
	{
		ArrayList<String> blockList = new ArrayList<String>();
		File tmpFile = new File(BlockListUtils.blocklistConfigPath);
		if (tmpFile.exists())
		{
			FileInputStream in = null;
			BufferedReader br = null;
			try
			{
				in = new FileInputStream(BlockListUtils.blocklistConfigPath);
				br = new BufferedReader(new InputStreamReader(in));
				String cacheItem;

				while ((cacheItem = br.readLine()) != null)
				{
					cacheItem = cacheItem.trim();
					if (!cacheItem.startsWith("#") && cacheItem.length() > 0)
					{
						blockList.add(cacheItem);
					}
				}
			}
			catch (Exception e)
			{
				logger.error(ServerListenerBlocklistStreams.MODULE_NAME + ".getStoredblocklistItems() ", e);
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
			logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".getStoredblocklistItems could not find block list items list");
		}
		return blockList;
	}

	private static void saveFile()
	{
		synchronized(BlockListUtils.streamblockList)
		{
			ArrayList<String> bli = BlockListUtils.streamblockList;
			try
			{
				File f = new File(blocklistConfigPath);
				if (!f.exists())
				{
					f.createNewFile();
				}

				FileWriter tmpFile = new FileWriter(blocklistConfigPath, false);
				StringBuilder b = new StringBuilder();
				for (int i = 0; i < bli.size(); i++)
				{
					logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".saveFile " + "Line: " + bli.get(i));
					b.append(bli.get(i) + "\n");
				}
				tmpFile.write(b.toString());
				tmpFile.close();
			}
			catch (IOException ioe)
			{
				logger.error(ServerListenerBlocklistStreams.MODULE_NAME + ".saveFile " + "IOException: " + ioe.getMessage(), ioe);
			}
		}
	}

	private static void printContents()
	{
		ArrayList<String> bli = BlockListUtils.getblockListedStreams();
		for (int i = 0; i < bli.size(); i++)
		{
			logger.info(ServerListenerBlocklistStreams.MODULE_NAME + ".printContents " + "bli.get(i): " + bli.get(i));
		}

	}

}
