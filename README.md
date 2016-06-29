# futility-server

This is a server hosting the front end and analysis libraries for the [futility analysis tool](https://f-utility.hms.harvard.edu/). It is a ring server which packages the futility-lib jar and the futility-js front-end.

## Installation

### Install futility-lib

For now, futility-lib is an unvended JAR, which this server picks up from a local maven repository. Check out [futility-lib](https://github.com/blandflakes/futility-libI), and run `mvn clean install`.

### Install futility-js
The most recent version of futility-js should be built and installed into resources/public. For more instructions, check out [futility-js](https://github.com/blandflakes/futility-js).

### Build futility-server

    $ lein uberjar

The most recent versions of futility-lib and futility-js are tracked in this repository

## Usage

    $ java -jar futility-server-0.1.0-standalone.jar [args]

You can optionally send a port number as the first arg in the case that the default port (9000) is already occupied.

Visit [localhost:9000/futility.html](localhost:9000/futility.html).

## License

Copyright © 2016 Brian Fults

Do whatever you want with this code. If it's helpful, send me a beer. If it's horrible, pretend I didn't write it, but in any case please give me credit if your work is any sort of derivation.
