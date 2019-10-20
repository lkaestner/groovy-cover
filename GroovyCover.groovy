//=============================================================================
// META
appName    = 'GroovyCover'
appDescr   = 'Creates Album-Covers using ImageMagick'
appVersion = '1.2.2'
appAuthor  = 'Lukas Kästner'
appLicense = 'SPDX: MIT'

//=============================================================================
// INIT

// grab dependencies
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')

// import classes
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files


//=============================================================================
// LOGGING
final logger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

final patternEncoder = new ch.qos.logback.classic.encoder.PatternLayoutEncoder()
patternEncoder.context = logger.loggerContext
patternEncoder.pattern = '%-5level - %message%n'
patternEncoder.start()

logger.getAppender('console').encoder = patternEncoder
logger.level = ch.qos.logback.classic.Level.INFO

logger.info("${appName}")
logger.info("Run this program with argument -h to view help and usage-information.")


//=============================================================================
// CLI ARGUMENTS

// read and parse CLI arguments, thus specifying the public API
final cli = new groovy.cli.commons.CliBuilder()
cli.with {
	h  longOpt: 'help', type: boolean, 'display help and usage-instructions'
	d  longOpt: 'debug', type: boolean, 'increase logging-verbosity and disable parallel file-processing'
	i  longOpt: 'inDir', convert: { Paths.get(it) }, defaultValue: 'in', 'relative or absolute path of the input-directory'
	o  longOpt: 'outDir', convert: { Paths.get(it) }, defaultValue: 'out', 'relative or absolute path of the output-directory'
	s  longOpt: 'skipExisting', type: boolean, 'do not create cover if output-file already exists'
	r  longOpt: 'outResolution', type: int, defaultValue: '1024', 'resolution of output image (longest edge)'
	tf longOpt: 'textFont', type: String, defaultValue: 'Arial-Bold', 'name of the font, e.g. Arial-Bold'
	tc longOpt: 'textColor', type: String, defaultValue: 'white', 'color of the font, e.g. white'
	th longOpt: 'textHeightFactor', type: float, defaultValue: '0.2', 'height of the text as factor of the output-resolution'
	tw longOpt: 'textWidthFactor', type: float, defaultValue: '0.7', 'width of the text as factor of the output-resolution'
}
final options = cli.parse(args)

// abort if options could not be parsed
if (!options) {
	System.exit(1)
}

// if requested, display help-text and exit
if (options.help) {
	cli.usage()
	System.exit(0)
}

// calculate the dimensions of the text
final int textHeight = options.outResolution * options.textHeightFactor
final int textWidth  = options.outResolution * options.textWidthFactor


//=============================================================================
// SCRIPT

// if requested, enable debugging and print basic debugging information
if (options.debug) {
	logger.level = ch.qos.logback.classic.Level.DEBUG
	logger.debug("Debugging is enabled")
	logger.debug("Date: ${java.time.LocalDateTime.now()}")
	logger.debug("Runtime: Groovy ${GroovySystem.version} on JDK ${System.properties['java.version']}")
}

// ensure that input and output directories exist
logger.debug("Input directory: ${options.inDir.toAbsolutePath()}")
Files.createDirectories(options.inDir)
logger.debug("Output directory: ${options.outDir.toAbsolutePath()}")
Files.createDirectories(options.outDir)

// collect input files into a list (for logging and determining suitable degree of parallelization)
logger.info('Determine list of input-files')
final fileList = []
options.inDir.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.(jpg|jpeg|png|bmp|tiff|webp)$/) { fileList << it }

// prematurely end if no input-files were found
if (fileList.empty) {
	logger.warn('No input files found. Exiting.')
	System.exit(1)
}

// process input-files in parallel
numberOfThreads = options.debug ? 1 : Math.min(fileList.size(), Runtime.getRuntime().availableProcessors())
logger.info("Processing ${fileList.size()} input-files using ${numberOfThreads} parallel threads.")
groovyx.gpars.GParsPool.withPool(numberOfThreads) {
	fileList.eachParallel { inFile ->
	
		// logging
		final logString = "[${fileList.indexOf(inFile)+1}/${fileList.size()}]: ${inFile}"
		logger.debugEnabled && logger.debug(logString)
		
		// determine file-name without extension (using regex)
		final cleanName = inFile.fileName.toString().replaceFirst(~/\.[^\.]+$/, '')
		logger.debug("Clean File-Name: ${cleanName}")
		
		// determine in-file's relative path to the in-directory
		final inFileRel = options.inDir.relativize(inFile)
		
		// determine out-file location, replicating the same relative path of the in-directory
		final outFile = options.outDir.resolve(inFileRel)
		logger.debug("Output File: ${outFile}")
		
		// prepare output directory
		Files.createDirectories(outFile.parent)
		
		// delete destination file or skip
		if (options.skipExisting && Files.exists(outFile)) {
			logger.debug("Skipping file: ${outFile}")
			return
		} else {
			Files.deleteIfExists(outFile)
		}
		
		// logging
		logger.debugEnabled || logger.info(logString)
		
		// build magick command
		logger.debug('Building Magick Command')
		final magickCommandBuilder = new StringBuilder()
		magickCommandBuilder.with {
			append 'magick convert'
			append ' -verbose'
			append ' -gravity center'
			append " -resize \"${options.outResolution}^>\""
			append " -crop \"${options.outResolution}x${options.outResolution}+0+0\""
			append ' -strip'
			append ' -background none'
			append " -font \"${options.textFont}\""
			append " -fill \"${options.textColor}\""
			append " -size \"${textWidth}x${textHeight}\""
			append " caption:\"${cleanName}\""
			append " \"${inFile}\""
			append ' +swap'
			append ' -composite'
			append " \"${outFile}\""
		}
		final magickCommand = magickCommandBuilder.toString()

		// execute magick command
		logger.debug("Executing Magick Command: ${magickCommand}")
		final proc = magickCommand.execute()
		proc.waitFor()
		
		// display execution result
		if (proc.exitValue() == 0) {
			logger.debug("Successfully created output-file: ${outFile}")
		} else {
			logger.error("ExitCode=${proc.exitValue()} | StdErr: ${proc.err.text} | StdOut: ${proc.in.text}")
		}
	}
}

logger.info('End')
