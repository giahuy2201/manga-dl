package com.giahuy2201.manga_dl;

import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to create EPUB file from PNGs
 */

public class Bundler {

	/**
	 * A Thread to measure output file size for ProgressBar
	 */
	private class ProgressBarRunnable implements Runnable {
		private boolean finished = false;

		public void run() {
			long max = (long) fileSize(MangaDL.extractor.getMangaDirectory());
			long current = 0;
			ProgressBar progressBar = new ProgressBar("Bundling", max);
			while (current < max) {
				current = (long) fileSize(new File(MangaDL.extractor.getTitle() + "." + MangaDL.format.toLowerCase()));
				if (finished) {
					current = max;
				}
				progressBar.stepTo(current);
			}
			progressBar.close();
		}

		private double fileSize(File dir) {
			if (!dir.exists()) {
				return 0;
			}
			if (dir.isFile()) {
				return dir.length() / Math.pow(1024, 2);
			}
			double folderSize = 0;
			for (File file : dir.listFiles()) {
				folderSize += fileSize(file);
			}
			return folderSize;
		}
	}

	private Packable packer;

	Bundler() throws Exception {
		if (MangaDL.format.equalsIgnoreCase("epub")) {
			this.packer = new EPUB();
		} else if (MangaDL.format.equalsIgnoreCase("pdf")) {
			this.packer = new PDF();
		} else {
			System.out.println(MangaDL.format);
			throw new CommandLine.ParameterException(MangaDL.cli, "Unsupported format");
		}
	}

	/**
	 * Bundle book
	 * @throws Exception
	 */
	public void pack() throws Exception {
		File mangaDirectory = MangaDL.extractor.getMangaDirectory();
		if (new File(mangaDirectory, "cover.png").exists()) {
			MangaDL.logger.fine("Found cover image ");
			MangaDL.extractor.setCover("cover");
		}

		MangaDL.logger.info("Adding metadata ...");
		packer.addMetadata();
		MangaDL.logger.info("Copying resources ...");
		packer.addResources(collectPNGs());

		MangaDL.logger.info("Writing to file " + MangaDL.extractor.getTitle() + ".epub");
		if (MangaDL.verbose || packer.selfTracking()) {
			packer.saveBook();
		} else {
			// A thread for progress bar
			ProgressBarRunnable progressBar = new ProgressBarRunnable();
			Thread progressThread = new Thread(progressBar);
			progressThread.start();
			packer.saveBook();
			progressBar.finished = true;
			progressThread.join();
		}
		MangaDL.logger.finest("BUNDLING finished\n");
	}

	/**
	 * Collect images
	 * @return A list of images separated by chapters
	 */
	private List<List<File>> collectPNGs() throws Exception {
		File[] files = MangaDL.extractor.getMangaDirectory().listFiles();
		MangaDL.logger.info("Collecting frames");
		List<List<File>> PNGs = new ArrayList<>();
		for (File file : files) {
			// Skip cover PNG and non PNGs
			if (!file.getName().endsWith(".png") || file.getName().equals(MangaDL.extractor.getCover() + ".png")) {
				continue;
			}
			int groupIndex = Integer.parseInt(file.getName().substring(0, 3));
			// Fill in new chapter lists
			for (int i = PNGs.size(); i <= groupIndex; i++) {
				PNGs.add(new ArrayList<>());
			}
			PNGs.get(groupIndex).add(file);
		}
		for (List<File> list : PNGs) {
			Collections.sort(list);
		}
		return PNGs;
	}

}