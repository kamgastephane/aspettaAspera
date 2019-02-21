package agoda.storage;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

abstract class FileStorage implements Storage {



   private static final Logger logger = LogManager.getLogger();

   protected String fileName;

   File getFile(String url,String destinationFolder,boolean increment)
   {
      String fileName = null;
      try {
         fileName = StorageUtils.getFileNameBasedOnUrl(url);
      } catch (MalformedURLException e) {
         logger.error("%s is not a valid URL, aborting",e);
         return null;
      }
      if(fileName == null || fileName.isEmpty())
      {
         logger.error("Could not defer a name based on url {}, Aborting!",url);
         return null;
      }

      String destinationPath = FilenameUtils.concat(destinationFolder, fileName);
      File file = new File(destinationPath);
      int count = 1;
      while (file.exists() && count< 100 && increment)
      {
         //we add a suffix to the filename and increment it
         String incFileName = StorageUtils.getIncrementalFileName(count,destinationPath);
         file = new File(incFileName);
         count++;
      }
      if(count==100)
      {
         //we have more than 100 file with the same name, something is going wrong
         logger.warn("we have more than 100 file with the same name {} derived from url {}, something is going wrong",destinationPath,url);
         return null;
      }
      return file;
   }
   /**
    * close the access to the resource and delete the underlying storage
    */
   boolean cleanUp() {
      logger.info("cleaning up: deleting file at {}",this.fileName);
      try {
         this.close();
         return new File(fileName).delete();
      } catch (IOException e) {
         logger.error("An exception occurred white cleaning up file at " + fileName, e);
         return false;
      }

   }
}
