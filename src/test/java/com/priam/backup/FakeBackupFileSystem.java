package com.priam.backup;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.priam.aws.S3BackupPath;
import com.priam.backup.AbstractBackupPath.BackupFileType;

public class FakeBackupFileSystem implements IBackupFileSystem
{
    private List<AbstractBackupPath> flist;
    public Set<String> downloadedFiles;
    public Set<String> uploadedFiles;

    @Inject
    Provider<S3BackupPath> pathProvider;

    public void setupTest(List<String> files)
    {
        clearTest();
        flist = new ArrayList<AbstractBackupPath>();
        for (String file : files)
        {
            S3BackupPath path = pathProvider.get();
            path.parseRemote(file);
            flist.add(path);
        }
        downloadedFiles = new HashSet<String>();
        uploadedFiles = new HashSet<String>();
    }

    public void setupTest()
    {
        clearTest();
        flist = new ArrayList<AbstractBackupPath>();
        downloadedFiles = new HashSet<String>();
        uploadedFiles = new HashSet<String>();
    }

    public void clearTest()
    {
        if (flist != null)
            flist.clear();
        if (downloadedFiles != null)
            downloadedFiles.clear();
    }

    public void addFile(String file)
    {
        S3BackupPath path = pathProvider.get();
        path.parseRemote(file);
        flist.add(path);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void download(AbstractBackupPath path) throws BackupRestoreException
    {
        try
        {
            if (path.type == BackupFileType.META)
            {
                // List all files and generate the file
                FileWriter fr = new FileWriter(path.newRestoreFile());
                try
                {
                    JSONArray jsonObj = new JSONArray();
                    for (AbstractBackupPath filePath : flist)
                    {
                        if (filePath.type == BackupFileType.SNAP)
                            jsonObj.add(filePath.getRemotePath());
                    }
                    fr.write(jsonObj.toJSONString());
                }
                finally
                {
                    IOUtils.closeQuietly(fr);
                }
            }
            downloadedFiles.add(path.getRemotePath());
            System.out.println("Downloading " + path.getRemotePath());
        }
        catch (IOException io)
        {
            throw new BackupRestoreException(io.getMessage(), io);
        }
    }

    @Override
    public void upload(AbstractBackupPath path) throws BackupRestoreException
    {
        try
        {
            uploadedFiles.add(path.localReader().getPath());
        }
        catch (IOException io)
        {
            throw new BackupRestoreException(io.getMessage(), io);
        }
    }

    @Override
    public Iterator<AbstractBackupPath> list(String bucket, Date start, Date till)
    {
        List<AbstractBackupPath> tmpList = new ArrayList<AbstractBackupPath>();
        for (AbstractBackupPath path : flist)
        {
            if ((path.time.after(start) && path.time.before(till)) || path.time.equals(start))
                tmpList.add(path);
        }
        return tmpList.iterator();
    }

    @Override
    public int getActivecount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}