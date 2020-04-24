package com.capsidaho.heartcrop;

import net.imagej.ops.OpService;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultIntTable;
import org.scijava.table.Table;

import java.io.*;

@Plugin(type = Command.class, label = "Heart Crop - OpenPoints")
public class OpenPoints implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter(style="file")
    private File file;

    @Parameter(type = ItemIO.OUTPUT)
    private Table table;

    @Override
    public void run() {
        BufferedReader r;
        try {
            r = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        table = new DefaultIntTable();
        table.appendColumn("x");
        table.appendColumn("y");
        table.appendColumn("z");

        String line = "asdf";
        while( line != null && line.length() > 0 ) {
            int rc = table.getRowCount();
            try {
                line = r.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if( line != null && line.length() > 0 ) {
                String[] parts = line.split(",");

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                table.appendRow();
                table.set(0, rc, x);
                table.set(1, rc, y);
                table.set(2, rc, z);
            }
        }
    }

}
