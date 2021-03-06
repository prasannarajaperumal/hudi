/*
 *  Copyright (c) 2016 Uber Technologies, Inc. (hoodie-dev-group@uber.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.uber.hoodie.io.compact.strategy;

import com.google.common.collect.Maps;
import com.uber.hoodie.common.model.HoodieDataFile;
import com.uber.hoodie.common.model.HoodieLogFile;
import com.uber.hoodie.common.util.FSUtils;
import com.uber.hoodie.config.HoodieWriteConfig;
import com.uber.hoodie.io.compact.CompactionOperation;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy for compaction. Pluggable implementation to define how compaction should be done. The
 * over-ridden implementations of this abstract class can capture the relevant metrics to order
 * and filter the final list of compaction operation to run in a single compaction.
 * Implementation of CompactionStrategy cannot hold any state. Difference instantiations can be
 * passed in every time
 *
 * @see com.uber.hoodie.io.compact.HoodieRealtimeTableCompactor
 * @see CompactionOperation
 */
public abstract class CompactionStrategy implements Serializable {

  public static final String TOTAL_IO_READ_MB = "TOTAL_IO_READ_MB";
  public static final String TOTAL_IO_WRITE_MB = "TOTAL_IO_WRITE_MB";
  public static final String TOTAL_IO_MB = "TOTAL_IO_MB";
  public static final String TOTAL_LOG_FILE_SIZE = "TOTAL_LOG_FILES_SIZE";
  public static final String TOTAL_LOG_FILES = "TOTAL_LOG_FILES";

  /**
   * Callback hook when a CompactionOperation is created. Individual strategies can capture the
   * metrics they need to decide on the priority.
   *
   * @param dataFile - Base file to compact
   * @param partitionPath - Partition path
   * @param logFiles - List of log files to compact with the base file
   * @return Map[String, Object] - metrics captured
   */
  public Map<String, Object> captureMetrics(HoodieDataFile dataFile, String partitionPath,
      List<HoodieLogFile> logFiles) {
    Map<String, Object> metrics = Maps.newHashMap();
    // Total size of all the log files
    Long totalLogFileSize = logFiles.stream().map(HoodieLogFile::getFileSize).filter(Optional::isPresent)
        .map(Optional::get).reduce((size1, size2) -> size1 + size2).orElse(0L);
    // Total read will be the base file + all the log files
    Long totalIORead = FSUtils.getSizeInMB(dataFile.getFileSize() + totalLogFileSize);
    // Total write will be similar to the size of the base file
    Long totalIOWrite = FSUtils.getSizeInMB(dataFile.getFileSize());
    // Total IO will the the IO for read + write
    Long totalIO = totalIORead + totalIOWrite;
    // Save these metrics and we will use during the filter
    metrics.put(TOTAL_IO_READ_MB, totalIORead);
    metrics.put(TOTAL_IO_WRITE_MB, totalIOWrite);
    metrics.put(TOTAL_IO_MB, totalIO);
    metrics.put(TOTAL_LOG_FILE_SIZE, totalLogFileSize);
    metrics.put(TOTAL_LOG_FILES, logFiles.size());
    return metrics;

  }

  /**
   * Order and Filter the list of compactions. Use the metrics captured with the captureMetrics to
   * order and filter out compactions
   *
   * @param writeConfig - HoodieWriteConfig - config for this compaction is passed in
   * @param operations - list of compactions collected
   * @return list of compactions to perform in this run
   */
  public List<CompactionOperation> orderAndFilter(HoodieWriteConfig writeConfig,
      List<CompactionOperation> operations) {
    return operations;
  }
}
