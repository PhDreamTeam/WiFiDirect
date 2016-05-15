package com.example.fileInspector;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 *
 */
public class FileGui {
    private final TextView tvFavorite;
    MainActivity mainActivity;
    private TextView tvFileContents;
    private LinearLayout parentLinearLayout;
    private File file;
    private Context context;
    private LinearLayout llExternal;

    private boolean isFavorite = false;

    private boolean continuousUpdateFile = true;

    private boolean fileAlreadyShown = false;

    /**
     *
     */
    public FileGui(File file, final LinearLayout parentLinearLayout, MainActivity mainActivity) {
        this.file = file;
        context = this.mainActivity = mainActivity;
        this.parentLinearLayout = parentLinearLayout;

        // external linear layout
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);

        llExternal = new LinearLayout(context);
        llExternal.setOrientation(LinearLayout.VERTICAL);
        llExternal.setLayoutParams(params1);

        // header linear layout
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout llHeader = new LinearLayout(context);
        llHeader.setOrientation(LinearLayout.HORIZONTAL);
        llHeader.setLayoutParams(params2);
        llHeader.setBackgroundColor(0xff190b7d); // colors must stat with ff (transparent if not)

        // header tv file, tv filename and Remove Button
        TextView tvFileNameAux = new TextView(context);
        //tvFileNameAux.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        tvFileNameAux.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        //tvFileNameAux.setTextColor(Color.WHITE);
        tvFileNameAux.setText("File: ");
        tvFileNameAux.setGravity(Gravity.CENTER_VERTICAL);
        llHeader.addView(tvFileNameAux);

        // tv file name
        TextView tvFileName = new TextView(context);
        tvFileName.setTextColor(Color.WHITE);
        llHeader.addView(tvFileName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        tvFileName.setGravity(Gravity.CENTER_VERTICAL);


        // remove button
        TextView tvRemoveFile = new TextView(context);
        tvRemoveFile.setBackgroundColor(0xff750b72);
        tvRemoveFile.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0); // presence_offline
        llHeader.addView(tvRemoveFile);
        tvRemoveFile.setOnClickListener(getRemoveClickListener());

        TextView tvSpace = new TextView(context);
        tvSpace.setText("  ");
        llHeader.addView(tvSpace);

        // favorite button
        tvFavorite = new TextView(context);
        tvFavorite.setBackgroundColor(0xff750b72);
        llHeader.addView(tvFavorite);
        tvFavorite.setOnClickListener(getFavoriteClickListener());

        llExternal.addView(llHeader);

        // scroll view with the tv  file contents
        ScrollView svFileContents = new ScrollView(context);
        svFileContents.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        svFileContents.setBackgroundColor(0xff134b13);

        tvFileContents = new TextView(context);
        svFileContents.addView(tvFileContents);

        llExternal.addView(svFileContents);

        parentLinearLayout.addView(llExternal);

        if (!file.isFile()) {
            tvFileContents.setText(file.toString() + " is not a file!!!!");
            continuousUpdateFile = false;
        }

        if (file.length() > 4 * 1024) {
            continuousUpdateFile = false;
        }

        isFavorite = mainActivity.isFavoriteFile(file.toString());
        updateFavoriteState();

        // set data
        tvFileName.setText(file.toString() + "    " + file.length() +
                /*" " + getFileLengthFromLsCommand(file.toString()) + */
                " " + (isContinuousUpdateFile() ? "AU" : ""));
    }


    /**
     *
     */
    public void updateFavoriteState() {
        tvFavorite.setCompoundDrawablesWithIntrinsicBounds(isFavorite ? android.R.drawable.star_big_on :
                android.R.drawable.star_big_off, 0, 0, 0); // presence_offline
    }

    /**
     *
     */
    private View.OnClickListener getRemoveClickListener() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                mainActivity.removeFileGui(FileGui.this);
            }
        };
    }

    /**
     *
     */
    private View.OnClickListener getFavoriteClickListener() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                if (isFavorite)
                    mainActivity.removeFavoriteFile(file.toString());
                else
                    mainActivity.addFavoriteFile(file.toString());

                isFavorite = !isFavorite;
                updateFavoriteState();
                mainActivity.updateFavoriteFiles();
            }
        };
    }

    /**
     *
     */
    public File getFile() {
        return file;
    }

    /**
     *
     */
    public TextView getTvFileContents() {
        return tvFileContents;
    }

    /**
     *
     */
    public boolean isContinuousUpdateFile() {
        return continuousUpdateFile;
    }

    /**
     *
     */
    public LinearLayout getExternalLinearLayout() {
        return llExternal;
    }

    /**
     *
     */
    public boolean isFileAlreadyShown() {
        return fileAlreadyShown;
    }

    /**
     *
     */
    public void setFileAlreadyShown(boolean fileAlreadyShown) {
        this.fileAlreadyShown = fileAlreadyShown;
    }

    /**
     *
     */
    public String toString() {
        return file.toString();
    }

    /**
     *
     */
    public void setFavoriteState(boolean isFavorite) {
        this.isFavorite = isFavorite;
        updateFavoriteState();
    }

    public static int getFileLengthFromLsCommand(String file) {
        String fileLengthStr = executeShellCommandAndGetOutput("ls -ls " + file);
        String[] tokens = fileLengthStr.split("\\s+");
        return Integer.parseInt(tokens[3]);
    }

    /**
     *
     */
    public static String executeShellCommandAndGetOutput(String command) {
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuilder output = new StringBuilder();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
