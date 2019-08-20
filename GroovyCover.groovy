//=============================================================================
// META
appName    = 'GroovyCover'
appDescr   = 'Creates Album-Covers using ImageMagick'
appVersion = '1.1.1'
appAuthor  = 'Lukas Kästner'
appLicense = 'SPDX: MIT'

//=============================================================================
// INIT

// grab dependencies
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='commons-io', module='commons-io', version='2.6')

// import classes
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import org.apache.commons.io.FilenameUtils


//=============================================================================
// LOGGING
final logger = org.slf4j.LoggerFactory.getLogger(appName)
final encoder = new ch.qos.logback.classic.encoder.PatternLayoutEncoder()
encoder.context = logger.loggerContext
encoder.pattern = '%-5level - %msg%n'
encoder.start()

final appender = new ch.qos.logback.core.ConsoleAppender()
appender.context = logger.loggerContext
appender.encoder = encoder
appender.start()

logger.addAppender(appender)
logger.additive = false
logger.level = ch.qos.logback.classic.Level.INFO

logger.info("${appName}")
logger.info("Run this program with argument -h to view help and usage-information.")


//=============================================================================
// CLI ARGUMENTS

// read and parse CLI arguments, thus specifying the public API
final cli = new groovy.cli.commons.CliBuilder()
cli.h(longOpt:'help', type: boolean, 'display help and usage-instructions')
cli.d(longOpt:'debug', type: boolean, 'increase logging-verbosity and disable parallel file-processing')
cli.i(longOpt:'in-dir', convert: { Paths.get(it) }, defaultValue: 'in', 'relative or absolute path of the input-directory')
cli.o(longOpt:'out-dir', convert: { Paths.get(it) }, defaultValue: 'out', 'relative or absolute path of the output-directory')
cli.f(longOpt:'text-font', type: String, defaultValue: 'Arial-Bold', 'name of the font, e.g. Arial-Bold')
cli.c(longOpt:'text-color', type: String, defaultValue: 'white', 'color of the font, e.g. white')
cli.r(longOpt:'out-resolution', type: int, defaultValue: '1024', 'resolution of output image (longest edge)')
final options = cli.parse(args)

// if requested, display help-text and exit
if (options.'help') {
	cli.usage()
	System.exit(0)
}

// finalize CLI arguments and other constants
final boolean debug          = options.'debug'
final Path    inDir          = options.'in-dir'
final Path    outDir         = options.'out-dir'
final String  textColor      = options.'text-color'
final String  textFont       = options.'text-font'
final int     outResolution  = options.'out-resolution'
final int     outTextWidth   = outResolution * 0.7
final int     outTextHeight  = outResolution * 0.2
final allowedExtensions      = ['jpg', 'jpeg', 'png']


//=============================================================================
// SCRIPT

// if requested, enable debugging and print basic debugging information
if (debug) { logger.level=ch.qos.logback.classic.Level.DEBUG }
logger.debug("Debugging is enabled")
logger.debug("Date: ${java.time.LocalDateTime.now()}")
logger.debug("Runtime: Groovy ${GroovySystem.version} on JDK ${System.properties['java.version']}")

// ensure that input and output directories exist
logger.debug("Input directory: ${inDir.toAbsolutePath()}")
Files.createDirectories(inDir)
logger.debug("Output directory: ${outDir.toAbsolutePath()}")
Files.createDirectories(outDir)

// collect input files into a list (for logging and determining suitable degree of parallelization)
logger.info('Determine list of input-files')
final fileList = []
inDir.eachFileRecurse(groovy.io.FileType.FILES) { currentFile ->
	if (allowedExtensions.contains(FilenameUtils.getExtension(currentFile.toString()))) { fileList << currentFile }
}
if (fileList.empty) { logger.warn('No input files found.') }

// process input-files in parallel
numberOfThreads = debug ? 1 : Math.min(fileList.size(), Runtime.getRuntime().availableProcessors())
logger.info("Processing ${fileList.size()} input-files using ${numberOfThreads} parallel threads.")
groovyx.gpars.GParsPool.withPool(numberOfThreads) {
	fileList.eachParallel { inFile ->
		final position = "[${fileList.indexOf(inFile)+1}/${fileList.size()}]"
		logger.info("${position}: ${inFile}")
		
		// determine file-name without extension
		final cleanName = FilenameUtils.getBaseName(inFile.toString())
		
		logger.debug("Clean File-Name: ${cleanName}")
		
		// determine file's relative path to the input-directory
		final inFileRel = inDir.relativize(inFile)
		
		// determine output-file location, replicating the same relative path of the input-directory
		final outFile = outDir.resolve(inFileRel)
		logger.debug("Output File: ${outFile}")
		
		// prepare output directory
		Files.createDirectories(outFile.parent)
		Files.deleteIfExists(outFile)
		
		// build magick command
		logger.debug('Building Magick Command')
		final magickCommand = ""
		.concat('magick convert')
		.concat(' -verbose')
		.concat(' -gravity center')
		.concat(" -resize \"${outResolution}^>\"")
		.concat(" -crop \"${outResolution}x${outResolution}+0+0\"")
		.concat(' -strip')
		.concat(' -background none')
		.concat(" -font \"${textFont}\"")
		.concat(" -fill \"${textColor}\"")
		.concat(" -size \"${outTextWidth}x${outTextHeight}\"")
		.concat(" caption:\"${cleanName}\"")
		.concat(" \"${inFile}\"")
		.concat(' +swap')
		.concat(' -composite')
		.concat(" \"${outFile}\"")
		
		// execute magick command
		logger.debug("Executing Magick Command: ${magickCommand}")
		final proc = magickCommand.execute()
		proc.waitFor()
		
		// display execution result
		final exitCode = proc.exitValue()
		if (exitCode == 0) {
			logger.debug("Successfully created output-file: ${outFile}")
		} else {
			logger.error("ExitCode=${proc.exitValue()} | StdErr: ${proc.err.text} | StdOut: ${proc.in.text}")
		}
	}
}

logger.info('End')
