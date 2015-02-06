# Metrics for Spring Batch

## Setup

### Graphite with Grafana

### InfluxDB with Grafana

https://coderwall.com/p/fg18jq/getting-started-influxdb-grafana-docker
https://github.com/tutumcloud/tutum-docker-influxdb

docker run -d -p 8083:8083 -p 8086:8086 -e PRE_CREATE_DB="db1" tutum/influxdb:latest
docker run -d -name grafana -p 8080:80 -e INFLUXDB_HOST=192.168.59.103 -e INFLUXDB_PORT=8086 -e INFLUXDB_NAME=db1 -e INFLUXDB_USER=root -e INFLUXDB_PASS=root tutum/grafana

curl -G 'http://localhost:8086/db/db1/series?u=root&p=root' --data-urlencode "q=select * from denschu.counter.batch.simpleBatchMetricsJob.count"


## Counter

### Job
counter.batch.simpleJob (Listener: beforeJob)

### Step
counter.batch.simpleJob.step.simpleStep (Listener: beforeStep)

### Custom (available after Restart)
counter.batch.simpleJob.step.simpleStep.businesscounter (BatchMetrics)


## Durations

### Job
gauge.batch.simpleJob.duration (Listener: afterJob)

### Step
gauge.batch.simpleJob.step.simpleStep.duration (Listener: afterStep)

### Chunk
gauge.batch.simpleJob.step.simpleStep.chunk.duration (Listener: afterChunk)
gauge.batch.simpleJob.step.simpleStep.chunk.count (Listener: afterChunk)

### Item
gauge.batch.simpleJob.step.simpleStep.item.duration (Listener: afterChunk)
gauge.batch.simpleJob.step.simpleStep.item.count (Listener: afterChunk)

### Read/Process/Write methods
gauge.batch.simpleJob.step.simpleStep.DummyItemReader.read.duration (Aspect: read)

### Custom (see AbstractBatchMetricsAspect)
gauge.batch.simpleJob.step.simpleStep.ExampleService.callExternalRemoteService.duration (Aspect: callExternalRemoteService)


## Functions

### Show count of job executions for a specific interval
summarize(hostname.counter.batch.example-job.count, '10m', 'sum', false)

### Show item duration/count for a specific job execution
hostname.gauge.batch.example-job.step.item.duration
hostname.gauge.batch.example-job.step.item.count

### Show item count for all job executions

<hostname>.gauge.batch.example-job.step.item.duration
<hostname>.gauge.batch.example-job.step.item.count

VHVP169368.counter.batch.lf-snts-to-daus-job.count
VHVP169368.gauge.batch.lf-snts-to-daus-job.*.xmlFileReadingStepMaster.item.duration
VHVP169368.gauge.batch.lf-snts-to-daus-job.*.xmlFileReadingStepMaster.item.count

## Notes

".count" is added automatically when using a Counter with GraphiteReporter
".value"  is added automatically when using a Gauge with InfluxDBReporter

All metrics will be prefixed with the Hostname or an Environment identifier (dev, test, qa, prod)