package de.codecentric.batch.simplejsr;

import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JsrTestUtils;

import java.util.concurrent.TimeoutException;

public class TraditionalJsr352Test {
	
	@Test
	public void testTraditionalJsr352() throws TimeoutException{
		JsrTestUtils.runJob("partitionMapperJobSpringDIBatchXml", null, 1000000);
	}

}
