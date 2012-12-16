package org.kari.album.ui;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ThemeResource;

/**
 * Hierarchical folder container
 *
 * @author kari
 */
public final class FolderHierarchicalContainer extends HierarchicalContainer {
    public static final String PROP_NAME = "Name";
    public static final String PROP_ICON = "Icon";
    
    private final File mRootDir;
    private final Set<File> mExpanded = new HashSet<File>();
    private final ThemeResource mIconResource = new ThemeResource("../runo/icons/16/folder.png");

    private final Set<String> mNotAllowedDirs = new HashSet<String>();
    
    
    public FolderHierarchicalContainer(File pRootDir) {
        mRootDir = pRootDir;
     
        addContainerProperty(PROP_NAME, String.class, null);
        addContainerProperty(PROP_ICON, 
                ThemeResource.class,
                mIconResource);

        Item item = addItem(mRootDir);
        item.getItemProperty(PROP_NAME).setValue(mRootDir.getName());
//        item.getItemProperty(PROP_ICON).setValue(mIconResource);
        
        mNotAllowedDirs.add("backup");
        mNotAllowedDirs.add("comment");
        mNotAllowedDirs.add("lib");
        mNotAllowedDirs.add("misc");
        mNotAllowedDirs.add("thumbnail");
        mNotAllowedDirs.add("trashbin");
        mNotAllowedDirs.add("private");
        mNotAllowedDirs.add("Private");
        mNotAllowedDirs.add("work");
        
        expandFolder(mRootDir, true);
    }

    public File getRootDir() {
        return mRootDir;
    }

    private File toDir(Object pItemId) {
        return (File)pItemId;
    }

    @Override
    public boolean areChildrenAllowed(Object pItemId) {
        return hasChildren(pItemId);
    }

    @Override
    public Collection<?> getChildren(Object pItemId) {
        return super.getChildren(pItemId);
//        File dir = toDir(pItemId);
//        
//        List<File> result = new ArrayList<File>();
//        File[] files = dir.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isDirectory() && !isFilteredOut(file)) {
//                    result.add(file);
//                }
//            }
//        }
//        Collections.sort(result);
//        return result;
    }

    @Override
    public Object getParent(Object pItemId) {
        return toDir(pItemId).getParentFile();
    }

    @Override
    public boolean hasChildren(Object pItemId) {
        return super.hasChildren(pItemId);
//        File dir = toDir(pItemId);
//        File[] files = dir.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isDirectory() && !isFilteredOut(file)) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }

    @Override
    public boolean isRoot(Object pItemId) {
        return mRootDir.equals(pItemId);
    }

    private void expandFolder(File pDir, boolean pRecursive) {
        if (!mExpanded.contains(pDir) && !isFilteredOut(pDir)) {
            mExpanded.add(pDir);
            File[] files = pDir.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    if (file.isDirectory() && !isFilteredOut(file)) {
                        Item item = addItem(file);
                        item.getItemProperty(PROP_NAME).setValue(file.getName());
//                        item.getItemProperty(PROP_ICON).setValue(mIconResource);
                        setParent(file, pDir);
                    }
                }
             
                if (pRecursive) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            expandFolder(file, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return true if dir is not allowed
     */
    public boolean isFilteredOut(File pDir) {
        String name = pDir.getName();
        return mNotAllowedDirs.contains(name);
    }
}
