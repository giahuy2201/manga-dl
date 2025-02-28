package com.giahuy2201.manga_dl;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Downloader class to work with Selenium container
 */
public class Downloader {

	/**
	 * A worker thread to download image
	 */
	private class ImageFetch implements Callable<String> {
		private String uri;
		private String fileName;

		ImageFetch(String uri, String fileName) {
			this.uri = uri;
			this.fileName = fileName;
		}

		@Override
		public String call() throws IOException {
			// Try 3 times if fails
			long attempts = 0;
			boolean failed = true;
			do {
				try {
					MangaDL.logger.info("Retrieving: " + uri);
					URLConnection request = new URL(uri).openConnection();
					request.setRequestProperty("referer",MangaDL.extractor.getBaseURL());
					request.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

					InputStream initialStream = request.getInputStream();
					byte[] buffer = IOUtils.toByteArray(initialStream);

					File output = new File(MangaDL.extractor.getMangaDirectory(), fileName);
					OutputStream outStream = new FileOutputStream(output);
					outStream.write(buffer);

					failed = false;
					MangaDL.logger.fine("Retrieved successfully!");
					break;
				} catch (IOException e) {
					e.printStackTrace();
					MangaDL.logger.warning("Failed to retrieve: " + uri);
					MangaDL.logger.info("Trying a " + ++attempts + "th time ...");
				}
			} while (attempts < 3);

			if (failed) {
				return uri;
			}
			return null;
		}
	}

	private int resumingIndex;
	private final List<String> remainingPNGs;
	private final List<Integer> remainingChaptersSizes;
	private List<Future<String>> results;

	public Downloader() {
		readDownloadedChapterIndex();
		List<List<String>> chaptersPNGs = MangaDL.extractor.getChaptersPNGs();
		// collect remaing chaptes' sizes
		this.remainingPNGs = new ArrayList<>();
		this.remainingChaptersSizes = new ArrayList<>();
		for (int i = resumingIndex; i < chaptersPNGs.size(); i++) {
			remainingPNGs.addAll(chaptersPNGs.get(i));
			remainingChaptersSizes.add(chaptersPNGs.get(i).size());
		}
		if (resumingIndex != 0) {
			System.out.println("Resuming at " + MangaDL.extractor.getChaptersNames().get(resumingIndex));
			MangaDL.logger.info("Resuming at " + MangaDL.extractor.getChaptersNames().get(resumingIndex));
		}
	}

	/**
	 * Download images multithreading
	 * @throws IOException
	 */
	public void download() throws Exception {
		// Give out tasks
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MangaDL.nThreads);
		this.results = new ArrayList<>();
		int chapterIndex = resumingIndex, pngIndex = 0;
		for (String chapterPNG : remainingPNGs) {

			String frameFileName = formatIndex(chapterIndex) + "-" + formatIndex(pngIndex) + ".png";
			Future<String> promise = threadPoolExecutor.submit(new ImageFetch(chapterPNG, frameFileName));
			results.add(promise);
			// Recalculate chapterIndex
			if (pngIndex + 2 - remainingChaptersSizes.get(chapterIndex - resumingIndex) > 0) {
				pngIndex = -1; // compensate for the increment
				chapterIndex++;
			}
			pngIndex++;
		}
		// Collect results
		ProgressBar progressBar = null;
		if (!MangaDL.verbose) {
			progressBar = new ProgressBar("Downloading", remainingPNGs.size());
		}
		int nDoneTasks = 0;
		while (nDoneTasks != remainingPNGs.size()) {
			nDoneTasks = finishedDownloading();
			if (progressBar != null) {
				progressBar.stepTo(nDoneTasks);
			}
		}
		if (progressBar != null) {
			progressBar.close();
		}
		threadPoolExecutor.shutdown();
		threadPoolExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);

		MangaDL.logger.finest("DOWNLOADING finished\n");
	}

	private int finishedDownloading() throws Exception {
		int nDones = 0;
		for (Future<String> promise : results) {
			if (promise.isDone()) {
				nDones++;
				String failed = (String) promise.get();
				if (failed != null) {
					MangaDL.logger.severe("Cannot retrieve " + failed);
					throw new IOException("Cannot retrieve " + failed);
				}
			}
		}
		return nDones;
	}

	/**
	 * Find the latest chapter number to resume
	 */
	private void readDownloadedChapterIndex() {
		File mangaDirectory = MangaDL.extractor.getMangaDirectory();
		MangaDL.logger.info("Scanning " + mangaDirectory + " ...");
		File[] mangaFrames = mangaDirectory.listFiles(
				pathname -> pathname.getName().matches("\\d{3}-\\d{3}.png$")
		);
		if (mangaFrames == null || mangaFrames.length == 0) {
			this.resumingIndex = 0;
		} else {
			File latestFile = Collections.max(Arrays.asList(mangaFrames));
			this.resumingIndex = Integer.parseInt(latestFile.getName().substring(0, 3));
			MangaDL.logger.fine("Found latest frame " + latestFile);
		}
	}

	/**
	 * Format index number into ### for better EPUB bundling. Eg. 1 -> 001
	 * @param count decimal number
	 */
	private String formatIndex(int count) {
		final int LENGTH = 3;
		String index = count + "";
		int countLength = index.length();
		for (int i = 0; i < LENGTH - countLength; i++) {
			index = "0" + index;
		}
		return index;
	}

}
