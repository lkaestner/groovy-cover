# GroovyCover
A small Groovy Script for quickly generating many Album-Covers.

## Features and Usage
Assume you want to create a couple of Album or Playlist Covers, e.g. for music-streaming services.
All you have to do is:
1. Find some nice images, and store these in the `in` directory.
2. Name each image-file - e.g. `My Playlist.jpg` and `Mixtape.jpeg`.
3. Run GroovyCover using: `groovy GroovyCover.groovy`
4. GroovyCover creates the album-covers in the `out` directory, which are correctly sized, cropped and have the file-name (without extension) overlayed in bold.

*Remarks:*
1. GroovyCover is multi-threaded and scales with the host's core-count.
2. The source-code is really small at ca. 150 LLOC.
3. You can customize the text-color, font-type and other aspects via CLI arguments. Call with -h to view usage-information.

## Screenshots
Side-by-side view of inputs and outputs:

![Screenshot 1](doc/groovy-cover-screenshot-1.png "Screenshot 1")

Sample console-output:

![Screenshot 2](doc/groovy-cover-screenshot-2.png "Screenshot 2")

## Prerequisites
GroovyCover uses [Groovy](http://groovy-lang.org) and [ImageMagick](https://www.imagemagick.org).

It doesn't matter how you install these, but some suggestions are:
* Windows: Get [Chocolatey](https://chocolatey.org) and run "choco install groovy imagemagick".
* Ubuntu:  Run "sudo apt-get install groovy imagemagick".

## Authors & Affiliations
* **Lukas Kästner** - *Initial work* - [lkaestner.com](https://lkaestner.com)
* no affiliations to ImageMagick or Groovy

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

