/************************************************************************
 * Source filename: FileCompressor.java
 * <p/>
 * Creation date: Feb 14, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileCompressor {

    private static final Logger logger = Logger.getLogger(FileCompressor.class);

    /**
     * Compress (tar.gz) the input file (or directory) to the output file
     * &lt;p/&gt;
     *
     * In the case of a directory all files within the directory (and all nested
     * directories) will be added to the archive
     *
     * @param file The file(s if a directory) to compress
     * @param output The resulting output file (should end in .tar.gz)
     * @throws IOException
     */
    public static void compressFile(File file, File output)
            throws IOException {
        ArrayList<File> list = new ArrayList<File>(1);
        list.add(file);
        compressFiles(list, output);
    }

    /**
     * Compress (tar.gz) the input files to the output file
     *
     * @param files The files to compress
     * @param output The resulting output file (should end in .tar.gz)
     * @throws IOException
     */
    public static void compressFiles(Collection<File> files, File output)
            throws IOException {
        logger.info("Compressing " + files.size() + " to " + output.getAbsoluteFile());

        // Create the output stream for the output file
        FileOutputStream fos = new FileOutputStream(output);

        // Wrap the output file stream in streams that will tar and gzip everything
        TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(new BufferedOutputStream(fos)));

        // TAR has an 8 gig file limit by default, this gets around that
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR); // to get past the 8 gig limit

        // TAR originally didn't support long file names, so enable the support for it
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        // Get to putting all the files in the compressed output file
        for (File f : files) {
            addFilesToCompression(taos, f, ".");
        }

        // Close everything up
        taos.close();
        fos.close();
    }

    public static void uncompressFile(File input, File outputDir) throws IOException {
        logger.info("uncompressing " + input.getName());
        FileInputStream fis = new FileInputStream(input);
        TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(fis)));
        TarArchiveEntry entry = null;
        while ((entry = tais.getNextTarEntry()) != null) {
            File outputFile = new File(outputDir, entry.getName());
            if (entry.isDirectory()) {
                logger.info("Attempting to write output directory " + outputFile.getAbsolutePath());
                if (!outputFile.exists()) {
                    logger.info("Attempting to create output directory " + outputFile.getAbsolutePath());
                    if (!outputFile.mkdirs()) {
                        throw new IllegalStateException("Couldn't create directory " + outputFile.getAbsolutePath());
                    }
                }
            } else {
                logger.info(String.format("Creating output file " + outputFile.getAbsolutePath()));
                OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(tais, outputFileStream);
                outputFileStream.close();
            }
        }
        tais.close();
    }

    /**
     * Does the work of compression and going recursive for nested directories
     * <p/>
     *
     * Borrowed heavily from http://www.thoughtspark.org/node/53
     *
     * @param taos The archive
     * @param file The file to add to the archive
     * @param dir The directory that should serve as the parent directory in the archivew
     * @throws IOException
     */
    private static void addFilesToCompression(TarArchiveOutputStream taos,
                                              File file, String dir) throws IOException {

        String separator = System.getProperty("file.separator");

        // Create an entry for the file
        taos.putArchiveEntry(new TarArchiveEntry(file, dir + separator + file.getName()));

        if (file.isFile()) {

            // Add the file to the archive
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();
        } else if (file.isDirectory()) {

            // close the archive entry
            taos.closeArchiveEntry();

            // go through all the files in the directory and using recursion, add them to the archive
            for (File childFile : file.listFiles()) {
                addFilesToCompression(taos, childFile, file.getName());
            }
        }
    }
}
