package fileserver.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class OfficeThreadPoolManager {

    private ThreadPoolExecutor  threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    public void commitThread(AbstractOfficeProcessThread processThread){
        threadPoolExecutor.submit(processThread);
    }

    public int poolState(){
        return threadPoolExecutor.getActiveCount();
    }
    public void shutDownPool(){
        threadPoolExecutor.shutdown();
    }
}
