package de.codecentric.batch.simplejsr.item;

import java.util.Properties;

import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;

public class SimplePartitionMapper implements PartitionMapper {

	@Override
	public PartitionPlan mapPartitions() throws Exception {
		PartitionPlan partitionPlan = new PartitionPlanImpl();
		partitionPlan.setPartitions(3);
		partitionPlan.setThreads(2);
		Properties[] propertiesArray = new Properties[3];
		for (int i = 0; i < 3; i++) {
			Properties properties = new Properties();
			properties.put("datakeyPartition", "key" + (i + 1));
			propertiesArray[i] = properties;
		}
		partitionPlan.setPartitionProperties(propertiesArray);
		return partitionPlan;
	}

}
