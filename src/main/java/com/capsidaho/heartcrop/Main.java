/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package com.capsidaho.heartcrop;

import ij.IJ;
import ij.plugin.frame.RoiManager;
import io.scif.SCIFIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.scijava.io.IOService;
import org.scijava.ui.UIService;
import sc.iview.SciView;

import java.io.IOException;

/**
 * Launch ImageJ+SciView and run heart crop
 */
public final class Main {

	private Main() {
		// prevent instantiation of utility class
	}


	public static void main(final String... args) {
		SciView sciView = SciView.createSciView();

		final ImageJ ij = new ImageJ(sciView.getScijavaContext());
		ij.launch(args);

		RoiManager roiManager = RoiManager.getRoiManager();
		String filename = "/home/kharrington/Data/Anjalie/CJ_volume_for_Kyle/190417_4D_full_Z.tif";
        //String filename = "/home/kharrington/Data/Anjalie/C1-fish4_z_stack_red.tif";

        Dataset img = null;
        try {
        	img = (Dataset) ij.context().service(IOService.class).open(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ij.context().service(UIService.class).show(img);
        IJ.setTool("point");



        roiManager.runCommand("Open","/home/kharrington/Data/Anjalie/CJ_volume_for_Kyle/190417_4D_full_Z_roiset.zip");

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ij.command().run("com.capsidaho.heartcrop.HeartCrop", true, new Object[]{} );
	}

}
