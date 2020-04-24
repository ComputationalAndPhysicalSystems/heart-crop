package com.capsidaho.heartcrop;

import net.imagej.ops.OpService;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultIntTable;
import org.scijava.table.Table;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import sc.iview.SciViewService;

import java.io.*;

@Plugin(type = Command.class, label = "HCTest")
public class TestCommand implements Command {
    @Parameter
    private DisplayService dService;

    @Parameter
    private UIService uiService;

    @Parameter
    private IOService ioService;

    @Parameter
    private OpService opService;

    @Parameter
    private LogService logService;

    @Parameter
    private CommandService command;

    @Parameter
    private Context context;

    @Parameter
    private ThreadService thread;

//    @Parameter
//    private SciViewService sciViewService;

//    @Parameter
//    private File imageFile = new File("");
//
//    @Parameter(callback = "dummy")
//    private Button openImage;
//
//    @Parameter(callback = "dummy")
//    private Button savePoints;
//
//    @Parameter(callback = "dummy")
//    private Button loadPoints;
//
//    @Parameter(callback = "dummy")
//    private Button createMesh;
//
//    @Parameter(callback = "dummy")
//    private Button visualize;
//
//    @Parameter(callback = "dummy")
//    private Button generateMask;
//
//    @Parameter(callback = "dummy")
//    private Button crop;

    public void dummy() {
        System.out.println("dummy call");
    }

    @Override
    public void run() {
        System.out.println("logService " + logService);
        System.out.println("opService " + opService);
    }

}
