package de.codecentric.batch.simplejsr;

import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.springframework.batch.test.JsrTestUtils;

public class TraditionalJsr352Test {
	
	@Test
	public void testTraditionalJsr352() throws TimeoutException{
		JsrTestUtils.runJob("partitionMapperJobSpringDIBatchXml", null, 1000000);
	}

}
