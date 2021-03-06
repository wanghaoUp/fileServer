package fileserver.cache;

import fileserver.entity.FileEntity;
import fileserver.exception.CacheRequestException;
import fileserver.utils.FileEntityHashArray;
import fileserver.utils.FileProcessorUtils;
import org.apache.commons.logging.impl.SimpleLog;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.HashMap;

public class FileCacheImpl implements FileCache {
    // 自己的数据结构
    private static FileEntityHashArray<String, FileEntity> fileEntityHashArray = new FileEntityHashArray<String, FileEntity>();
    // 所有的文件记录
    private static HashMap<String,FileListNode> allFilesRecords = new HashMap<String, FileListNode>();

    private FileStoreImpl fileStoreImpl = new FileStoreImpl();

    private SimpleLog log = new SimpleLog("log");

    private static String allImagesDirectory  = "";

    // 仅一次
    static {
        initialCache();
    }
    // 实例化 类的时候，先去加载指定文件目录下的所有图片
    static final void initialCache(){// 通过配置文件的方式，获取指定的目录
        String path = FileProcessorUtils.getTheFilePathFromPropertiesFile("allImagesDirectory");

        allImagesDirectory = path;

        File file = new File(path);
        findAllFiles(file);
    }

    static void findAllFiles(File file){
        if(file.isFile()){
            // 文件记录存放
            allFilesRecords.put(file.getName(),new FileListNode(file.getPath(),1));

            // 预读取一部分文件
            FileEntity fileEntity = FileProcessorUtils.imageToDataInBase64(file);
            /*
            **********************************此处可以优化，降低置换次数，并且数组满*****************
             */
            fileEntityHashArray.put(fileEntity.getFileName(),fileEntity);
        }else {
            if(file.exists() && file.isDirectory()){
                File[] files = file.listFiles();
                for(File file1 : files){
                    findAllFiles(file1);
                }
            }
        }
    }
    @Override
    public boolean hashArrayExistCache(String fileName) {
        return fileEntityHashArray.whetherExistKey(fileName);
    }

    @Override
    public boolean recordsExistMemory(String fileName) {
        return allFilesRecords.get(fileName)==null ? false : true;
    }

    @Override
    public FileEntity getSpecialFile(String fileName) {
        if (recordsExistMemory(fileName)){// 内存存在 ?
            if(hashArrayExistCache(fileName)){// 缓存存在 ?
                return fileEntityHashArray.get(fileName);
            }else{// 从磁盘读取,置换内存中的
                return exchangeEntityFromCache(fileName, reloadFromMemory(fileName));
            }
        }else {
            throw new CacheRequestException("The requested resource is not in the memory!");
        }
    }

    @Override
    public FileEntity exchangeEntityFromCache(String preFileName, File newFile) {
       return fileEntityHashArray.put(preFileName,FileProcessorUtils.imageToDataInBase64(newFile));
    }

    private static File reloadFromMemory(String preFileName){
        return new File(allFilesRecords.get(preFileName).getFilePath());
    }

    public void removeFromList(String fileName){
        allFilesRecords.remove(fileName);
    }

    public void addToList(String fileName){
        allFilesRecords.put(fileName,new FileListNode(allImagesDirectory+fileName,1));
    }

    /**
     * 存 图片文件的 ，存储后需要添加到缓存
     * @param imageFile
     * @return
     */
    public String storeNewFile(File imageFile){
        String imagePath = fileStoreImpl.storeFileToDisk(imageFile);
        addToList(imagePath);
        int capacity = fileEntityHashArray.getElementNumber();
        return imagePath+":"+capacity;
    }

    /**
     * 存office的，图片路径在存储后不直接添加到 缓存链表
     * @param officeFile
     * @param path
     * @return
     */
    public String storeNewImageFile(File officeFile){
        return fileStoreImpl.storeFileToDisk(officeFile);
    }

    /**
     * 存 一般文件
     * @param commonFile
     * @return
     */
    public String storeNewFile(MultipartFile commonFile, String path){
        File dest = new File(path);
        try {
            commonFile.transferTo(dest);
        } catch (IOException e) {
            log.info("context",e);
        }
        // 先存 在仓库，再转存到一个固定文件夹做 缓存 操作
        return fileStoreImpl.storeFileToDisk(dest);
    }

    public void recallAddRecord(File file){
        allFilesRecords.put(file.getName(),new FileListNode(file.getPath(),1));
    }

}
