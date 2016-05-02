# wse-plugin-banstreams
BanStreams is a module for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) that allows you the ability to persistently ban published streams.

## Prerequisites

Wowza Streaming Engine 4.0.0 or later is required.

## Usage

This module provides the following functionality:

* Ban streams using an HTTP Provider (point and select the stream to ban)
* Each ban will persist through each Wowza reboot.  
* Allow previously banned streams to be whitelisted again.
* Supports banning both rtmp and rtsp publishers.

## API Reference

[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/WowzaStreamingEngine_ServerSideAPI.pdf)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/forums/content.php?759-How-to-extend-Wowza-Streaming-Engine-using-the-Wowza-IDE)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/resources/developers) to learn more about our APIs and SDK.

To use the compiled version of this module, see [How to mix audio and video from different live sources (MediaCachePreload)](https://www.wowza.com/forums/content.php?675-How-to-ban-a-stream-in-session-(ServerListenerBanStreams)).

## Contact

[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License

This code is distributed under the [Wowza Public License](https://github.com/WowzaMediaSystems/[jar-file-name]/blob/master/LICENSE.txt).
