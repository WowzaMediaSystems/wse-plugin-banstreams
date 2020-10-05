# BlocklistStreams
The **BlocklistStreams** server listener and HTTP Provider for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to persistently blocklist streams published to your Wowza media server.

This repo includes a [compiled version](/lib/wse-plugin-blackliststreams.jar).

## Prerequisites
Wowza Streaming Engine 4.0.0 or later is required.

## Usage
**BlocklistStreams** provides the following functionality:

* Blocklist stream names using an HTTP Provider (allows selection of streams to blocklist).
* Contents of blocklist persist through each Wowza Streaming Engine media server reboot.  
* Allows previously blocklisted stream names to be whitelisted again.
* Supports blocklisting of both RTMP and RTSP sources.

To blocklist a stream:

1. Start your Wowza Streaming Engine media server, and then open the following URL to the Stream Blocklists page in a web browser: <pre>http://[wowza-ip-address]:8086/blocklist</pre>

2. When prompted, enter the credentials that you use to sign in to Wowza Streaming Engine Manager.

3. Publish your first stream, and then in the **Stream Blocklists** page, click **Check for new streams**. Your new stream will be displayed in the **Published Streams** list.

4. Click the **Blocklist** link next to the published stream to add that stream to the blocklist. This will also stop the stream. Any subsequent attempts to publish to that [app-name]/[app-instance]/[stream-name] sequence are rejected until you remove the stream from the blocklist.

## More resources
To use the compiled version of this module, see [Blacklist a stream in session with a Wowza Streaming Engine server listener and HTTP provider](https://www.wowza.com/docs/how-to-blacklist-a-stream-in-session-blackliststreams).

[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/serverapi/)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/developer) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](/LICENSE.txt).
