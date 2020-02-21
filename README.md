# BlacklistStreams
The **BlacklistStreams** server listener and HTTP Provider for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to persistently blacklist streams published to your Wowza media server.

## Prerequisites
Wowza Streaming Engine 4.0.0 or later is required.

## Usage
**BlacklistStreams** provides the following functionality:

* Blacklist stream names using an HTTP Provider (allows selection of streams to blacklist).
* Contents of blacklist persist through each Wowza Streaming Engine media server reboot.  
* Allows previously blacklisted stream names to be whitelisted again.
* Supports blacklisting of both RTMP and RTSP sources.

To blacklist a stream:

1. Start your Wowza Streaming Engine media server, and then open the following URL to the Stream Blacklists page in a web browser: <pre>http://[wowza-ip-address]:8086/blacklist</pre>

2. When prompted, enter the credentials that you use to sign in to Wowza Streaming Engine Manager.

3. Publish your first stream, and then in the **Stream Blacklists** page, click **Check for new streams**. Your new stream will be displayed in the **Published Streams** list.

4. Click the **Blacklist** link next to the published stream to add that stream to the blacklist. This will also stop the stream. Any subsequent attempts to publish to that [app-name]/[app-instance]/[stream-name] sequence are rejected until you remove the stream from the blacklist.

## More resources
[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/serverapi/)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/forums/content.php?759-How-to-extend-Wowza-Streaming-Engine-using-the-Wowza-IDE)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/resources/developers) to learn more about our APIs and SDK.

To use the compiled version of this module, see [How to blacklist a stream in session (BlacklistStreams)](https://www.wowza.com/forums/content.php?675-How-to-blacklist-a-stream-in-session-%28BlacklistStreams%29).

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](https://github.com/WowzaMediaSystems/wse-plugin-blackliststreams/blob/master/LICENSE.txt).
