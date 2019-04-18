//=============================================================================
// META
appName    = 'GroovyCover'
appDescr   = 'Creates Album-Covers using ImageMagick'
appVersion = '1.0.0'
appAuthor  = 'Lukas Kästner'
appLicense = 'SPDX: MIT'

//=============================================================================
// INIT

// dependencies and imports:
@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.16')
@Grab(group='com.google.guava', module='guava', version='27.1-jre')

import org.slf4j.impl.SimpleLogger
import java.nio.file.Files
import java.nio.file.Paths
import com.google.common.io.MoreFiles

//=============================================================================
// SETTINGS

final debug = false

final inDir  = Paths.get('in')
final outDir = Paths.get('out')

final String textFont   = 'Arial-Bold'
final String textColor  = 'white'
final int outResolution = 1024
final int outTextWidth  = outResolution * 0.7
final int outTextHeight = outResolution * 0.2

final allowedExtensions = ['jpg', 'jpeg', 'png']


//=============================================================================
// LOGGING
System.properties[SimpleLogger.LOG_FILE_KEY] = 'System.out'
System.properties[SimpleLogger.SHOW_THREAD_NAME_KEY] = 'false'
System.properties[SimpleLogger.DEFAULT_LOG_LEVEL_KEY ] = debug ? 'debug' : 'info'
final log = new SimpleLogger('')
log.info("${appName} - ${java.time.LocalDateTime.now()}")


//=============================================================================
// SCRIPT

// create directories
log.debug("Input directory: ${inDir.toAbsolutePath()}")
Files.createDirectories(inDir);
log.debug("Output directory: ${outDir.toAbsolutePath()}")
Files.createDirectories(outDir);

// collect input files
log.info('Determine list of input-files')
final fileList = []
inDir.eachFileRecurse(groovy.io.FileType.FILES) { currentFile ->
	allowedExtensions.contains(MoreFiles.getFileExtension(currentFile)) && fileList << currentFile
}
fileList.empty && log.warn('No input files found.')

// process input-files in parallel
numberOfThreads = debug ? 1 : Math.min(fileList.size(), Runtime.getRuntime().availableProcessors())
log.info("Processing ${fileList.size()} input-files using ${numberOfThreads} parallel threads.")
groovyx.gpars.GParsPool.withPool(numberOfThreads) {
	fileList.eachParallel { inFile ->
		final position = "[${fileList.indexOf(inFile)+1}/${fileList.size()}]"
		log.info("${position}: ${inFile}")
		
		// determine file-name without extension
		final cleanName = MoreFiles.getNameWithoutExtension(inFile)
		log.debug("Clean File-Name: ${cleanName}")
		
		// determine output-file location
		final outFile = outDir.resolve(inFile.fileName.toString());
		log.debug("Output File: ${outFile}")
		Files.deleteIfExists(outFile)
		
		// build magick command
		log.debug('Building Magick Command')
		final magickCommand = """
		magick convert
		-verbose
		-gravity center
		-resize ${outResolution}^>
		-crop ${outResolution}x${outResolution}+0+0 -strip
		-background none -font \"${textFont}\" -fill \"${textColor}\" -size ${outTextWidth}x${outTextHeight} caption:\"${cleanName}\"
		\"${inFile}\" +swap -composite \"${outFile}\"
		"""
		
		// execute magick command
		log.debug("Executing Magick Command:\n${magickCommand}")
		final proc = magickCommand.execute()
		proc.waitFor()
		
		// display execution result
		final exitCode = proc.exitValue()
		if (exitCode == 0) {
			log.debug("Successfully created output-file: ${outFile}")
		} else {
			log.error("""ExitCode: ${proc.exitValue()}
						 StdErr:   ${proc.err.text}
						 StdOut:   ${proc.in.text}""")
		}

	}
}

log.info('End')