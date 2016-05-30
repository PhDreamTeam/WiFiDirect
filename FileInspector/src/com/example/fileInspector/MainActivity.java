package com.example.fileInspector;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;


/**
 * The following aspects should be improved:
 * - ...
 */
public class MainActivity extends Activity {

    public static final String APP_MAIN_FILES_DIR_PATH = "/sdcard/Android/data/com.example.fileInspector";
    public static final String INTERNAL_STATE_FILENAME = "internalState.txt";
    public static final String TAG = "MainActivity";
    private LinearLayout llFavoriteFiles;
    private LinearLayout llFavoriteDirs;

    enum SHOW_STATUS {DIRS_AND_FILES, FAVORITE_DIRS, FAVORITE_FILES}

    ;

    SHOW_STATUS[] show_status_values = SHOW_STATUS.values();

    SHOW_STATUS show_status = SHOW_STATUS.DIRS_AND_FILES;

    private TextView tvFSRoot;
    private ListView lvFSDirs;

    private FileFilter ffDirs = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private FileFilter ffFiles = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    };

    private Comparator<File> sortAscending = new Comparator<File>() {
        public int compare(File f1, File f2) {
            return f1.toString().compareTo(f2.toString());
        }
    };

    private final ArrayList<FileGui> fileGuiContainer = new ArrayList<>();

    private final ArrayList<String> favoriteFiles = new ArrayList<>();
    private final ArrayList<String> favoriteDirectories = new ArrayList<>();

    private ArrayAdapter<String> fsDirsLVAdapter;
    private ListView lvFSFiles;
    private ArrayAdapter<String> fsFilesLVAdapter;
    private File currentDir = new File("/");
    private TextView tvFSPrevious;
    private Thread readFileThread;
    private LinearLayout llFSContents;
    private TextView tvFavoriteDir;
    private LinearLayout llDirsAndFiles;

    /**
     * Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(TAG, "onCreate");

        tvFSRoot = (TextView) findViewById(R.id.tvFSRoot);
        tvFSPrevious = (TextView) findViewById(R.id.tvFSPrevious);
        tvFavoriteDir = (TextView) findViewById(R.id.tvFavoriteDir);

        lvFSDirs = (ListView) findViewById(R.id.lvFSDirs);
        lvFSFiles = (ListView) findViewById(R.id.lvFSFiles);
        llDirsAndFiles = (LinearLayout) findViewById(R.id.llDirsAndFiles);
        llFavoriteDirs = (LinearLayout) findViewById(R.id.llFavoriteDirs);
        llFavoriteFiles = (LinearLayout) findViewById(R.id.llFavoriteFiles);
        llFSContents = (LinearLayout) findViewById(R.id.llFSContents);

        tvFSPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeToNewDir(currentDir.toString().equals("/") ? currentDir : currentDir.getParentFile());
            }
        });

        tvFavoriteDir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeCurrentDirectoryFavoriteState();
            }
        });

        final ArrayList<String> dirsArrayList = new ArrayList<>();
        ArrayList<String> filesArrayList = new ArrayList<>();

        // Dirs list view adapter
        fsDirsLVAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dirsArrayList);
        lvFSDirs.setAdapter(fsDirsLVAdapter);

        // directories onClickListener
        AdapterView.OnItemClickListener dirClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(200).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                changeToNewDir(new File(currentDir.toString() + "/" + item));
                                view.setAlpha(1);
                            }
                        });
            }
        };

        lvFSDirs.setOnItemClickListener(dirClickListener);


        // Files list view adapter
        fsFilesLVAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, filesArrayList);
        lvFSFiles.setAdapter(fsFilesLVAdapter);


        // directories onClickListener
        AdapterView.OnItemClickListener fsFilesLVAdapter = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(200).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                processFile(new File("/" + currentDir.toString() + "/" + item));
                                view.setAlpha(1);
                            }
                        });
            }
        };

        lvFSFiles.setOnItemClickListener(fsFilesLVAdapter);

        // loading state from disk
        if (!loadStateFromDisk()) {
            // start showing root files
            showFiles(currentDir);
        }
    }

    /**
     *
     */
    private void changeCurrentDirectoryFavoriteState() {
        if (isFavoriteDir(currentDir.toString()))
            removeFavoriteDir(currentDir.toString());
        else addFavoriteDir(currentDir.toString());
        setFavoriteDirGuiState();
        updateFavoriteDirectories();
    }

    /**
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
//        return super.onPrepareOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.action_favorites:
                Log.d(TAG, "Favorites menu clicked");

                // next scenario
                show_status = show_status_values[(show_status.ordinal() + 1) % show_status_values.length];

                switch (show_status) {
                    case DIRS_AND_FILES:
                        llFavoriteFiles.setVisibility(View.GONE);
                        llDirsAndFiles.setVisibility(View.VISIBLE);
                        break;
                    case FAVORITE_DIRS:
                        llDirsAndFiles.setVisibility(View.GONE);
                        llFavoriteDirs.setVisibility(View.VISIBLE);
                        updateFavoriteDirectories();
                        break;
                    case FAVORITE_FILES:
                        llFavoriteDirs.setVisibility(View.GONE);
                        llFavoriteFiles.setVisibility(View.VISIBLE);
                        updateFavoriteFiles();
                }
        }
        return true;
    }

    /**
     *
     */
    public void updateFavoriteDirectories() {
        for (int i = 1, size = llFavoriteDirs.getChildCount(); i < size; ++i)
            llFavoriteDirs.removeViewAt(1);

        for (final String favDir : favoriteDirectories) {
            if (!favDir.equals(favoriteDirectories.get(0))) {
                TextView tvSeparator = new TextView(this);
                tvSeparator.setBackgroundColor(0xff945774); // colors must stat with ff (transparent if not)
                llFavoriteDirs.addView(tvSeparator, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            }

            // linear layout
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setBackgroundColor(0xff190b7d); // colors must stat with ff (transparent if not)
            ll.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));


            TextView tv = new TextView(this);
            tv.setText(favDir);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            ll.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));

            TextView tvRemove = new TextView(this);
            tvRemove.setBackgroundColor(0xff750b72); // colors must stat with ff (transparent if not)
            tvRemove.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0); // presence_offline
            ll.addView(tvRemove, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvRemove.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    favoriteDirectories.remove(favDir);
                    updateFavoriteDirectories();
                    setFavoriteDirGuiState();
                }
            });

            TextView tvSpace = new TextView(this);
            tvSpace.setText("  ");
            ll.addView(tvSpace);

            // favorite button
            TextView tvGo = new TextView(this);
            tvGo.setBackgroundColor(0xff750b72);
            tvGo.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_set_as, 0, 0, 0); // presence_offline
            ll.addView(tvGo);
            tvGo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    changeToNewDir(new File(favDir));
                    setFavoriteDirGuiState();
                }
            });


            llFavoriteDirs.addView(ll);
        }
    }


    /**
     *
     */
    public void updateFavoriteFiles() {
        for (int i = 1, size = llFavoriteFiles.getChildCount(); i < size; ++i)
            llFavoriteFiles.removeViewAt(1);

        for (final String favFile : favoriteFiles) {
            if (!favFile.equals(favoriteFiles.get(0))) {
                TextView tvSeparator = new TextView(this);
                tvSeparator.setBackgroundColor(0xff945774); // colors must stat with ff (transparent if not)
                llFavoriteFiles.addView(tvSeparator, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            }

            // linear layout
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setBackgroundColor(0xff190b7d); // colors must stat with ff (transparent if not)
            ll.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));


            TextView tv = new TextView(this);
            tv.setText(favFile);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            ll.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));

            TextView tvRemove = new TextView(this);
            tvRemove.setBackgroundColor(0xff750b72); // colors must stat with ff (transparent if not)
            tvRemove.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0); // presence_offline
            ll.addView(tvRemove, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvRemove.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    favoriteFiles.remove(favFile);
                    // update this view
                    updateFavoriteFiles();
                    // update files shown view
                    updateFavoritesOnFilesShown();
                }
            });

            TextView tvSpace = new TextView(this);
            tvSpace.setText("  ");
            ll.addView(tvSpace);

            // View file
            TextView tvViewFile = new TextView(this);
            tvViewFile.setBackgroundColor(0xff750b72);
            tvViewFile.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_input_add, 0, 0, 0); // presence_offline
            ll.addView(tvViewFile);
            tvViewFile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // add file to viewed files
                    processFile(new File(favFile));
                }
            });

            llFavoriteFiles.addView(ll);
        }
    }


    /**
     *
     */
    private void updateFavoritesOnFilesShown() {
        for (FileGui fg : fileGuiContainer) {
            fg.setFavoriteState(favoriteFiles.contains(fg.getFile().toString()));
        }
    }

    /**
     *
     */
    private void changeToNewDir(File newDir) {

        if (showFiles(newDir)) {
            tvFSRoot.setText(newDir.toString());
            currentDir = newDir;
            setFavoriteDirGuiState();
        }
    }

    /**
     *
     */
    public void setFavoriteDirGuiState() {
        tvFavoriteDir.setCompoundDrawablesWithIntrinsicBounds(isFavoriteDir(currentDir.toString()) ?
                android.R.drawable.star_big_on :
                android.R.drawable.star_big_off, 0, 0, 0); // presence_offline
    }

    /**
     *
     */
    private boolean showFiles(File path) {

        Log.d(TAG, "Show files: Path " + path.toString());


        // get directories and sort them
        File[] dirs = path.listFiles(ffDirs);
        if (dirs == null)
            return false;

        fsDirsLVAdapter.clear();
        fsFilesLVAdapter.clear();


        Log.d(TAG, "show files: SubDirs = " + Arrays.toString(dirs));
        Arrays.sort(dirs, sortAscending);

        // put dirs on list view
        for (File subDir : Arrays.asList(dirs)) {
            fsDirsLVAdapter.add(subDir.getName());
        }

        // get files and sort them
        File[] files = path.listFiles(ffFiles);
        Arrays.sort(files, sortAscending);

        // put dirs on list view
        for (File fileOnDir : Arrays.asList(files)) {
            fsFilesLVAdapter.add(fileOnDir.getName());
        }

        // place scrolling to top
        lvFSDirs.setSelectionAfterHeaderView();
        lvFSFiles.setSelectionAfterHeaderView();

        return true;
    }

    /**
     *
     */
    private void processFile(File file) {
        Log.d(TAG, "Processing file: " + file.toString());

        // build file gui from file
        FileGui fileGui = new FileGui(file, llFSContents, this);

        // keep file gui in global array
        synchronized (fileGuiContainer) {
            fileGuiContainer.add(fileGui);
        }

        // check if working thread is running if not create and start it
        if (readFileThread == null) {
            readFileThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            readContinuousUpdateFiles();
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        Log.d(TAG, "ReadFileContinuously: Thread stopped...");
                    }
                }
            });
            readFileThread.start();
        }
    }

    /**
     *
     */
    private void readContinuousUpdateFiles() {
        synchronized (fileGuiContainer) {
            // check if no jobs, end thread
            if (fileGuiContainer.size() == 0) {
                readFileThread = null;
                Thread.currentThread().interrupt();
            }

            // do file read for every file continuously updated
            for (FileGui fileGui : fileGuiContainer) {
                if (fileGui.isContinuousUpdateFile()) {
                    readFileToTextView(fileGui.getFile(), fileGui.getTvFileContents());
                    continue;
                }
                if (!fileGui.isFileAlreadyShown()) {
                    readFileToTextView(fileGui.getFile(), fileGui.getTvFileContents());
                    fileGui.setFileAlreadyShown(true);
                }
            }
        }
    }


    /**
     *
     */
    private void readFileToTextView(final File file, final TextView tv) {
        try {
            Log.d(TAG, "Start reading file: " + file);
            final Scanner scan = new Scanner(file);
            StringBuilder fileContents = new StringBuilder();
            while (scan.hasNextLine()) {
                final String line = scan.nextLine();
                Log.d(TAG, "Read line from file: " + file + " -> " + line);
                fileContents.append(line).append("\n");
            }
            scan.close();
            final String finalContents = fileContents.toString();
            tv.post(new Runnable() {
                public void run() {
                    tv.setText(finalContents);
                }
            });
        } catch (final FileNotFoundException e) {
            tv.post(new Runnable() {
                public void run() {
                    tv.setText(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            });

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        saveStateToDisk();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    /**
     *
     */
    public void removeFileGui(FileGui fileGui) {
        // remove fileGui from global container
        synchronized (fileGuiContainer) {
            fileGuiContainer.remove(fileGui);
        }

        // remove gui view from app
        llFSContents.removeView(fileGui.getExternalLinearLayout());
    }

    /**
     *
     */
    private void saveStateToDisk() {
        // unsure path existence
        buildPath(MainActivity.APP_MAIN_FILES_DIR_PATH);

        File stateFile = new File(APP_MAIN_FILES_DIR_PATH, INTERNAL_STATE_FILENAME);

        Log.d(TAG, "Saving app state to file: " + stateFile.toString());

        try {
            PrintWriter pw = new PrintWriter(stateFile);

            // write main path
            pw.println("mainPath  = " + tvFSRoot.getText().toString());

            // first keep favoriteDirs
            synchronized (favoriteDirectories) {
                for (String favorite : favoriteDirectories) {
                    pw.println("favoriteDir  = " + favorite);
                }
            }

            // first keep favoriteFiles (then open files)
            synchronized (favoriteFiles) {
                for (String favorite : favoriteFiles) {
                    pw.println("favoriteFile  = " + favorite);
                }
            }

            // write open files
            synchronized (fileGuiContainer) {
                for (FileGui fileGui : fileGuiContainer) {
                    pw.println("openFile  = " + fileGui.getFile().toString());
                }
            }


            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    private boolean loadStateFromDisk() {
        File stateFile = new File(APP_MAIN_FILES_DIR_PATH, INTERNAL_STATE_FILENAME);

        Log.d(TAG, "Loading app state from file: " + stateFile.toString());

        try {
            Scanner scan = new Scanner(stateFile);

            // process lines
            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();
                if (line.length() == 0)
                    continue;

                Log.d(TAG, "Reading app state, read line: " + line);
                Scanner scanLine = new Scanner(line);
                String firstToken = scanLine.next();
                String secondToken = scanLine.next();
                String thirdToken = scanLine.next();
                scanLine.close();

                if (firstToken.equalsIgnoreCase("mainPath") && secondToken.equalsIgnoreCase("=")) {
                    currentDir = new File(thirdToken);
                    tvFSRoot.setText(currentDir.toString());
                    // start showing root files
                    showFiles(currentDir);
                }

                if (firstToken.equalsIgnoreCase("openFile") && secondToken.equalsIgnoreCase("=")) {
                    processFile(new File(thirdToken));
                }

                if (firstToken.equalsIgnoreCase("favoriteFile") && secondToken.equalsIgnoreCase("=")) {
                    addFavoriteFile(thirdToken);
                }

                if (firstToken.equalsIgnoreCase("favoriteDir") && secondToken.equalsIgnoreCase("=")) {
                    addFavoriteDir(thirdToken);
                }
            }

            setFavoriteDirGuiState();
            scan.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Internal state file not found");
            return false;
        }
        return true;
    }


    /**
     * Creates the received path. The path has to be constructed segment by
     * segment because some segments could have dots and those segment must
     * be create separately from others
     */
    public static void buildPath(String path) {

        Log.d("Logger", "Building path: " + path);

        String[] components = path.split("/");

        String buildPath = "";
        for (String s : components) {
            if (s.equals("")) {
                if (buildPath.length() != 0)
                    throw new IllegalStateException("Error creating LOG directory: " + buildPath);
                buildPath += '/';
            } else {
                if (buildPath.charAt(buildPath.length() - 1) != '/')
                    buildPath += '/';
                buildPath += s;
                File logDir = new File(buildPath);
                if (!logDir.mkdir() && !logDir.exists())
                    throw new IllegalStateException("Error creating LOG directory: " + buildPath);
            }
        }
    }


    /**
     *
     */
    public boolean isFavoriteFile(String fileName) {
        return favoriteFiles.contains(fileName);
    }

    /**
     *
     */
    public void addFavoriteFile(String favorite) {
        favoriteFiles.add(favorite);
    }

    /**
     *
     */
    public void removeFavoriteFile(String fileName) {
        favoriteFiles.remove(fileName);
    }

    /**
     *
     */
    public boolean isFavoriteDir(String dirName) {
        return favoriteDirectories.contains(dirName);
    }

    /**
     *
     */
    public void addFavoriteDir(String dirName) {
        favoriteDirectories.add(dirName);
    }

    /**
     *
     */
    public void removeFavoriteDir(String dirName) {
        favoriteDirectories.remove(dirName);
    }
}

