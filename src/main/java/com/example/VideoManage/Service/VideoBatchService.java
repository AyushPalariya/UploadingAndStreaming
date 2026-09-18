package com.example.VideoManage.Service;

import com.example.VideoManage.Interfaces.VideoBatchInterface;
import org.springframework.batch.core.*;


import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

@Service
public class VideoBatchService implements VideoBatchInterface {
    private final Job videoMergeJob;
    private final JobLauncher jobLauncher;
    public VideoBatchService(
            JobLauncher jobLauncher,
            Job videoMergeJob
    ) {
        this.jobLauncher = jobLauncher;
        this.videoMergeJob = videoMergeJob;
    }
    public void startVideoMergeJob(String uploadId,int totalChunks,String fileName) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters=new JobParametersBuilder()//job ko runtime information provide krne ke liye parameter banaye jate hai
                .addString("uploadId",uploadId)
                .addString("fileName",fileName)

                .addLong("totalChunks",(long)totalChunks)
                .addLong("timestamp",System.currentTimeMillis())//create unique job parameters
                .toJobParameters();
        jobLauncher.run(videoMergeJob,jobParameters);
        //for job parameter
        //Ye sabse important part hai.
        //
        //Har upload ka data different hoga.
        //
        //Suppose User 1:
        //
        //uploadId = ABC123
        //totalChunks = 100
        //
        //User 2:
        //
        //uploadId = XYZ789
        //totalChunks = 50
        //
        //Batch Job ko pata hona chahiye ki kis upload ke chunks process karne hain.
        //
        //Isliye:
        //
        //.addString("uploadId", uploadId)
        //
        //aur:
        //
        //.addLong("totalChunks", (long) totalChunks)

        //uploadId   = abc123
        //totalChunks = 10
        //timestamp   = 172....
        //ye above information batch job ko milegi
    }
}
